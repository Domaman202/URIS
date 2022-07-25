package ru.uris;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings("unused")
public abstract class ObjectProviderSocket implements Closeable {
    protected Socket socket;
    protected DataInputStream is;
    protected DataOutputStream os;
    public final List<Packet> buffer = new ArrayList<>();
    protected Thread listener;

    public ObjectProviderSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.is = new DataInputStream(socket.getInputStream());
        this.os = new DataOutputStream(socket.getOutputStream());
    }

    public abstract List<Object> ObjectPool();

    public Thread createListener() {
        return this.listener == null ?
                this.listener = new Thread(() -> {
                    while (!this.socket.isClosed()) {
                        try {
                            if (this.is.available() > 0)
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
                case OBJECT_LIST -> this.writePacket(new Packet.PObjectList(packet.id, this.ObjectPool()));
                case METHOD_LIST -> this.writePacket(new Packet.PMethodList(packet.id, ((Packet.PMethodList) packet).objectId, false));
            }
            this.os.flush();
        } else buffer.add(packet);
    }

    public Packet sendAndReceive(Packet packet) throws IOException {
        int id;
        synchronized (this) {
            id = this.writePacket(packet);
            this.os.flush();
        }
        while (checkBuffer(id))
            Thread.onSpinWait();
        return this.buffer.stream().filter(p -> p.id == id).findFirst().orElseThrow(IOException::new);
    }

    public synchronized boolean checkBuffer(int id) {
        return this.buffer.stream().noneMatch(p -> p.id == id);
    }

    public synchronized int writePacket(Packet packet) throws IOException {
        this.os.write(7);
        packet.write(this);
        return packet.id;
    }

    public synchronized void writeEnum(Enum<?> value) throws IOException {
        this.os.write(6);
        this.writeStringI(value.name());
    }

    public synchronized void writeEnum(String value) throws IOException {
        this.os.write(6);
        this.writeStringI(value);
    }

    public synchronized void writeString(String value) throws IOException {
        this.os.write(0);
        this.writeStringI(value);
    }

    protected synchronized void writeStringI(String value) throws IOException {
        var bytes = value.getBytes(StandardCharsets.UTF_8);
        this.os.writeInt(bytes.length);
        this.os.write(bytes);
    }

    public synchronized void writeDouble(double value) throws IOException {
        this.os.write(1);
        this.os.writeDouble(value);
    }

    public synchronized void writeLong(long value) throws IOException {
        this.os.write(2);
        this.os.writeLong(value);
    }

    public synchronized void writeInt(int value) throws IOException {
        this.os.write(3);
        this.os.writeInt(value);
    }

    public synchronized void writeShort(short value) throws IOException {
        this.os.write(4);
        this.os.writeShort(value);
    }

    public synchronized void writeByte(byte value) throws IOException {
        this.os.write(5);
        this.os.writeByte(value);
    }

    public synchronized void writeBoolean(boolean value) throws IOException {
        this.writeByte((byte) (value ? 1 : 0));
    }

    public synchronized Packet readPacket() throws IOException {
        this.checkValue(7);
        return Packet.read(this);
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
        return new String(this.is.readNBytes(this.is.readInt()));
    }

    public synchronized double readDouble() throws IOException {
        this.checkValue(1);
        return this.is.readDouble();
    }

    public synchronized long readLong() throws IOException {
        this.checkValue(2);
        return this.is.readLong();
    }

    public synchronized int readInt() throws IOException {
        this.checkValue(3);
        return this.is.readInt();
    }

    public synchronized short readShort() throws IOException {
        this.checkValue(4);
        return this.is.readShort();
    }

    public synchronized byte readByte() throws IOException {
        this.checkValue(5);
        return this.is.readByte();
    }

    public synchronized boolean readBoolean() throws IOException {
        return this.readByte() == 1;
    }

    protected synchronized void checkValue(int needed) throws IOException {
        var i = this.is.read();
        if (i != needed)
            throw new IOException("Invalid Value {" + i + "}! Required {" + needed + "}!");
    }

    @Override
    public void close() throws IOException {
        this.is.close();
        this.os.close();
    }
}
