package ru.uris;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public abstract class ObjectServer implements AutoCloseable {
    protected final Socket socket;
    public final DataInputStream is;
    public final DataOutputStream os;
    protected Thread listener;
    protected volatile boolean sync;

    public ObjectServer(Socket socket) throws IOException {
        this.socket = socket;
        this.is = new DataInputStream(this.socket.getInputStream());
        this.os = new DataOutputStream(this.socket.getOutputStream());
    }

    public abstract List<Object> objectPool();

    public void runListener() {
        this.listener = new Thread(() -> {
            try {
                while (true) {
                    while (this.is.available() == 0 || sync) ;
                    this.listen();
                }
            } catch (IOException | InvocationTargetException | IllegalAccessException e) {
                if (e.toString().contains("Socket closed")) {
                    this.listener = null;
                    return;
                }
                throw new RuntimeException(e);
            }
        });
        this.listener.start();
    }

    public boolean changeSync(boolean state) {
        var old = this.sync;
        this.sync = state;
        return old;
    }

    public Packet sendAndRead() throws IOException, InvocationTargetException, IllegalAccessException {
        var sync = this.changeSync(true);
        this.send();
        var result = this.listen();
        this.sync = sync;
        return result;
    }

    public Packet listen() throws IOException, InvocationTargetException, IllegalAccessException {
        return listen(this.readPacket());
    }

    public Packet listen(Packet packet) throws IOException, InvocationTargetException, IllegalAccessException {
        var pool = this.objectPool();
        return switch (packet.type) {
            case OBJECT_LIST_REQUEST -> {
                this.writePacket(new Packet.ObjectList(pool.size()));
                this.send();
                yield packet;
            }
            case OBJECT_LIST -> new Packet.ObjectList(this.is.readInt());
            case METHOD_LIST_REQUEST -> {
                this.writePacket(new Packet.MethodList(pool.get(this.is.readInt()).getClass().getDeclaredMethods()));
                this.send();
                yield packet;
            }
            case METHOD_LIST -> new Packet.MethodList(this);
            case INVOKE -> {
                var p = new Packet.Invoke(this);
                var obj = this.objectPool().get(p.objectId);
                var method = obj.getClass().getDeclaredMethods()[p.methodId];
                method.setAccessible(true);
                var args = new Object[p.arguments.length];
                for (int i = 0; i < args.length; i++) {
                    var arg = p.arguments[i];
                    if (arg.type == Type.OBJECT)
                        args[i] = this.getRemoteObject(arg.id);
                    args[i] = arg.value;
                }
                this.writePacket(new Packet.Return(method.invoke(Modifier.isStatic(method.getModifiers()) ? null : obj, args)));
                this.send();
                yield p;
            }
            case RETURN -> new Packet.Return(this);
            case CLOSE -> {
                this.close();
                yield packet;
            }
            default -> packet;
        };
    }

    public Object getRemoteObject(int objectId, Class<?> ... interfaces) throws IOException, InvocationTargetException, IllegalAccessException {
        boolean sync = this.changeSync(true);
        var instance = this.getRemoteObject(objectId);
        var result = Proxy.newProxyInstance(ObjectServer.class.getClassLoader(), interfaces, (proxy, method, args) -> instance.invokeMethod(method.getName(), Arrays.stream(method.getParameterTypes()).map(Type::of).toArray(Type[]::new), args == null ? new Object[0] : args));
        this.sync = sync;
        return result;
    }

    public RemoteObject getRemoteObject(int objectId) throws IOException, InvocationTargetException, IllegalAccessException {
        return new RemoteObject(objectId);
    }

    public Packet.Return sendInvoke(int objectId, int methodId, Packet.Invoke.Argument[] arguments) throws IOException, InvocationTargetException, IllegalAccessException {
        this.writePacket(new Packet.Invoke(objectId, methodId, arguments));
        return (Packet.Return) this.sendAndRead();
    }

    public Packet.MethodList sendPacketMethodList(int id) throws IOException, InvocationTargetException, IllegalAccessException {
        this.writePacket(new Packet(PacketType.METHOD_LIST_REQUEST));
        this.os.writeInt(id);
        return (Packet.MethodList) this.sendAndRead();
    }

    public Packet.ObjectList sendPacketObjectList() throws IOException, InvocationTargetException, IllegalAccessException {
        this.writePacket(new Packet(PacketType.OBJECT_LIST_REQUEST));
        return (Packet.ObjectList) this.sendAndRead();
    }

    public void sendPacketHello() throws IOException {
        this.writePacket(new Packet(PacketType.HELLO_PACKET));
        this.send();
    }

    public void send() throws IOException {
        this.os.flush();
    }

    public void writePacket(Packet packet) throws IOException {
        packet.write(this);
    }

    public void writeEnum(Enum<?> element) throws IOException {
        this.writeString(element.name());
    }

    public void writeString(String value) throws IOException {
        this.os.write(0);
        var bytes = value.getBytes(StandardCharsets.UTF_8);
        this.os.writeInt(bytes.length);
        this.os.write(bytes);
    }

    public void writeDouble(double value) throws IOException {
        this.os.write(1);
        this.os.writeDouble(value);
    }

    public void writeLong(long value) throws IOException {
        this.os.write(2);
        this.os.writeLong(value);
    }

    public void writeInt(int value) throws IOException {
        this.os.write(3);
        this.os.writeInt(value);
    }

    public void writeShort(short value) throws IOException {
        this.os.write(4);
        this.os.writeShort(value);
    }

    public void writeByte(byte value) throws IOException {
        this.os.write(5);
        this.os.writeByte(value);
    }

    public Packet readPacket() throws IOException {
        return new Packet(this.readEnum(PacketType.class));
    }

    public <T extends Enum<T>> T readEnum(Class<T> clazz) throws IOException {
        try {
            var name = this.readString();
            return Arrays.stream(((T[]) MethodHandles.lookup().findStatic(clazz, "values", MethodType.methodType(clazz.arrayType())).invoke())).filter(o -> o.name().equals(name)).findFirst().orElseThrow();
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    public String readString() throws IOException {
        if (this.is.read() != 0)
            throw new IOException("Invalid value!");
        return new String(this.is.readNBytes(this.is.readInt()), StandardCharsets.UTF_8);
    }

    public double readDouble() throws IOException {
        if (this.is.read() != 1)
            throw new IOException("Invalid value!");
        return this.is.readDouble();
    }

    public long readLong() throws IOException {
        if (this.is.read() != 2)
            throw new IOException("Invalid value!");
        return this.is.readLong();
    }

    public int readInt() throws IOException {
        if (this.is.read() != 3)
            throw new IOException("Invalid value!");
        return this.is.readInt();
    }

    public short readShort() throws IOException {
        if (this.is.read() != 4)
            throw new IOException("Invalid value!");
        return this.is.readShort();
    }

    public byte readByte() throws IOException {
        if (this.is.read() != 5)
            throw new IOException("Invalid value!");
        return this.is.readByte();
    }

    @Override
    public void close() throws IOException {
        if (!this.socket.isClosed()) {
            this.writePacket(new Packet(PacketType.CLOSE));
            this.send();
        }
        this.sync = true;
        this.listener = null;
        this.socket.close();
        System.out.println("Connection closed!");
    }

    public class RemoteObject {
        public final int id;
        public final Packet.MethodList.MethodInfo[] methods;

        public RemoteObject(int id) throws IOException, InvocationTargetException, IllegalAccessException {
            this.id = id;
            this.methods = ObjectServer.this.sendPacketMethodList(id).methods;
        }

        public Object invokeMethod(String name, Type[] argts, Object... args) throws NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
            mloop:
            for (int j = 0; j < methods.length; j++) {
                var method = methods[j];
                if (method.name.equals(name)) {
                    for (int i = 0; i < method.args.length; i++)
                        if (method.args[i] != argts[i])
                            continue mloop;
                    var sync = ObjectServer.this.changeSync(true);
                    var ret = ObjectServer.this.sendInvoke(0, j, Arrays.stream(args).map(o -> new Packet.Invoke.Argument(ObjectServer.this, o)).toList().toArray(new Packet.Invoke.Argument[0]));
                    var result = ret.type == Type.OBJECT ? ObjectServer.this.getRemoteObject(ret.id) : ret.value;
                    ObjectServer.this.sync = sync;
                    return result;
                }
            }
            throw new NoSuchMethodException(name);
        }
    }
}
