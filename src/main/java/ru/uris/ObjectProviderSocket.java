package ru.uris;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

public abstract class ObjectProviderSocket implements Closeable {
    protected Socket socket;
    protected DataInputStream istream;
    protected DataOutputStream ostream;
    public final List<Packet> buffer = new ArrayList<>();
    protected Thread listener;

    public ObjectProviderSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.istream = new DataInputStream(socket.getInputStream());
        this.ostream = new DataOutputStream(socket.getOutputStream());
    }

    public abstract List<Object> getObjectPool();

    public RemoteObject createRemoteObject(int id) throws IOException {
        var methods = ((Packet.PMethodList) this.sendAndReceive(new Packet.PMethodList(Packet.nextId(), id, true))).methods;
        return new RemoteObjectImpl(id, methods);
    }

    public Object invokeRemoteMethod(RemoteMethod method, Object ... args) throws IOException {
        try {
            var obj = this.getObjectPool().get(method.obj);
            return obj.getClass().getMethod(method.name, Arrays.stream(method.args).map(t -> t.type.map()).toList().toArray(new Class[0])).invoke(obj, args);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new IOException(e);
        }
    }

    public Thread createListener() {
        return this.listener == null ?
                this.listener = new Thread(() -> {
                    while (!this.socket.isClosed()) {
                        try {
                            if (this.istream.available() > 0)
                                this.listen();
                            else Thread.onSpinWait();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }) : this.listener;
    }

    public synchronized void listen() throws IOException {
        var packet = this.readPacket();
        if (packet.request) {
            switch (packet.type) {
                case HELLO -> this.writePacket(new Packet(packet.id, Packet.Type.HELLO, false));
                case CLOSE -> this.close();
                case OBJECT_LIST -> this.writePacket(new Packet.PObjectList(packet.id, this.getObjectPool()));
                case METHOD_LIST -> this.writePacket(new Packet.PMethodList(packet.id, ((Packet.PMethodList) packet).oid, false));
                case METHOD_CALL -> this.writePacket(new Packet.PMethodCall((Packet.PMethodCall) packet));
            }
            this.ostream.flush();
        } else buffer.add(packet);
    }

    public Packet sendAndReceive(Packet packet) throws IOException {
        int id;
        synchronized (this) {
            id = this.writePacket(packet);
            this.ostream.flush();
        }
        while (checkBuffer(id))
            Thread.onSpinWait();
        return this.buffer.stream().filter(p -> p.id == id).findFirst().orElseThrow(IOException::new);
    }

    public synchronized boolean checkBuffer(int id) {
        return this.buffer.stream().noneMatch(p -> p.id == id);
    }

    public synchronized void writeObject(Object arr) throws IOException {
        var type = ARType.of(arr);
        var size = type.dim == 0 ? -1 : Array.getLength(arr);
        this.ostream.writeInt(size);
        if (size > -1) {
            for (int i = 0; i < size; i++) {
                this.writeObject(Array.get(arr, i));
            }
        } else {
            this.writeEnum(type.type);
            if (arr instanceof Boolean b)
                arr = (byte) (b ? 1 : 0);
            writeWithType(arr, type.type);
        }
    }

    public synchronized void writeWithType(Object obj, PType type) throws IOException {
        switch (type) {
            case BYTE -> this.writeByte((Byte) obj);
            case SHORT -> this.writeShort((Short) obj);
            case INT -> this.writeInt((Integer) obj);
            case LONG -> this.writeLong((Long) obj);
            case DOUBLE -> this.writeDouble((Double) obj);
            case STRING -> this.writeString((String) obj);
            case ENUM -> this.writeEnum((Enum<?>) obj);
            case PACKET -> this.writePacket((Packet) obj);
            case OBJECT -> {
                this.getObjectPool().add(obj);
                this.ostream.writeInt(this.getObjectPool().indexOf(obj));
            }
        }
    }

    public synchronized int writePacket(Packet packet) throws IOException {
        this.ostream.write(7);
        packet.write(this);
        return packet.id;
    }

    public synchronized void writeARType(ARType type) throws IOException {
        this.ostream.write(10);
        this.ostream.writeInt(type.dim);
        this.writeEnum(type.type);
    }

    public synchronized void writeEnum(Enum<?> value) throws IOException {
        this.ostream.write(6);
        this.writeStringI(value.name());
    }

    public synchronized void writeEnum(String value) throws IOException {
        this.ostream.write(6);
        this.writeStringI(value);
    }

    public synchronized void writeString(String value) throws IOException {
        this.ostream.write(0);
        this.writeStringI(value);
    }

    protected synchronized void writeStringI(String value) throws IOException {
        var bytes = value.getBytes(StandardCharsets.UTF_8);
        this.ostream.writeInt(bytes.length);
        this.ostream.write(bytes);
    }

    public synchronized void writeDouble(double value) throws IOException {
        this.ostream.write(1);
        this.ostream.writeDouble(value);
    }

    public synchronized void writeLong(long value) throws IOException {
        this.ostream.write(2);
        this.ostream.writeLong(value);
    }

    public synchronized void writeInt(int value) throws IOException {
        this.ostream.write(3);
        this.ostream.writeInt(value);
    }

    public synchronized void writeShort(short value) throws IOException {
        this.ostream.write(4);
        this.ostream.writeShort(value);
    }

    public synchronized void writeByte(byte value) throws IOException {
        this.ostream.write(5);
        this.ostream.writeByte(value);
    }

    public synchronized void writeBoolean(boolean value) throws IOException {
        this.writeByte((byte) (value ? 1 : 0));
    }

    public synchronized Object readObject() throws IOException {
        var size = this.istream.readInt();
        if (size > -1) {
            var arr = new Object[size];
            for (int i = 0; i < size; i++)
                Array.set(arr, i, this.readObject());
            return arr;
        } else {
            var type = this.readEnum(PType.class);
            return switch (type) {
                case BYTE -> this.readByte();
                case SHORT -> this.readShort();
                case INT -> this.readInt();
                case LONG -> this.readLong();
                case DOUBLE -> this.readDouble();
                case STRING -> this.readString();
                case ENUM -> this.readEnum();
                case PACKET -> this.readPacket();
                case NULL -> null;
                case OBJECT -> this.createRemoteObject(this.istream.readInt());
            };
        }
    }

    public synchronized Packet readPacket() throws IOException {
        this.checkValue(7);
        return Packet.read(this);
    }

    public synchronized ARType readARType() throws IOException {
        this.checkValue(10);
        return new ARType(this.istream.readInt(), this.readEnum(PType.class));
    }

    @SuppressWarnings("unchecked")
    public synchronized <T extends Enum<T>> T readEnum(Class<T> clazz) throws IOException {
        this.checkValue(6);
        var name = this.readStringI();
        try {
            return Arrays.stream(((T[]) MethodHandles.lookup().findStatic(clazz, "values", MethodType.methodType(clazz.arrayType())).invoke())).filter(o -> o.name().equals(name)).findFirst().orElseThrow();
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    public synchronized String readEnum() throws IOException {
        this.checkValue(6);
        return this.readStringI();
    }

    public synchronized String readString() throws IOException {
        this.checkValue(0);
        return this.readStringI();
    }

    public synchronized String readStringI() throws IOException {
        return new String(this.istream.readNBytes(this.istream.readInt()));
    }

    public synchronized double readDouble() throws IOException {
        this.checkValue(1);
        return this.istream.readDouble();
    }

    public synchronized long readLong() throws IOException {
        this.checkValue(2);
        return this.istream.readLong();
    }

    public synchronized int readInt() throws IOException {
        this.checkValue(3);
        return this.istream.readInt();
    }

    public synchronized short readShort() throws IOException {
        this.checkValue(4);
        return this.istream.readShort();
    }

    public synchronized byte readByte() throws IOException {
        this.checkValue(5);
        return this.istream.readByte();
    }

    public synchronized boolean readBoolean() throws IOException {
        return this.readByte() == 1;
    }

    protected synchronized void checkValue(int needed) throws IOException {
        var i = this.istream.read();
        if (i != needed)
            throw new IOException("Invalid Value {" + i + "}! Required {" + needed + "}!");
    }

    @Override
    public synchronized void close() throws IOException {
        this.istream.close();
        this.ostream.close();
    }

    public interface RemoteObject {
        Object invoke(String name, Object ... args) throws IOException, NoSuchMethodException;
    }

    public class RemoteObjectImpl implements RemoteObject {
        public final int id;
        public final RemoteMethod[] methods;

        public RemoteObjectImpl(int id, RemoteMethod[] methods) {
            this.id = id;
            this.methods = methods;
        }

        public Object invoke(String name, Object ... args) throws IOException, NoSuchMethodException {
            var argt = new ARType[args.length];
            for (int i = 0; i < args.length; i++)
                argt[i] = ARType.of(args[i]);

            for (var method : this.methods) {
                if (method.name.equals(name) && method.args.length == argt.length) {
                    var j = 0;
                    for (int i = 0; i < argt.length; i++)
                        if (method.args[i].equals(argt[i]))
                            j++;
                    if (j == argt.length)
                        return ((Packet.PMethodCall) ObjectProviderSocket.this.sendAndReceive(new Packet.PMethodCall(Packet.nextId(), method, args))).result;
                }
            }
            throw new NoSuchMethodException("Unknown method: " + name);
        }
    }
}
