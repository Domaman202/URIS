package ru.guris

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.nio.charset.StandardCharsets

abstract class ObjectProviderSocket implements Closeable {
    protected Socket socket
    protected DataInputStream istream
    protected DataOutputStream ostream
    final List<Packet> buffer = new ArrayList<>()
    protected Thread listener

    ObjectProviderSocket(Socket socket) {
        this.socket = socket
        this.istream = new DataInputStream(socket.inputStream)
        this.ostream = new DataOutputStream(socket.outputStream)
    }

    abstract List<Object> getObjectPool();

    Thread createListener() {
        return this.listener == null ?
                this.listener = new Thread(() -> {
                    while (!this.socket.closed) {
                        try {
                            if (this.istream.available() > 0)
                                this.listen()
                            else Thread.onSpinWait()
                        } catch (IOException e) {
                            throw new RuntimeException(e)
                        }
                    }
                }) : this.listener
    }

    synchronized void listen() throws IOException {
        var packet = this.readPacket();
        if (packet.request) {
            switch (packet.type) {
                case Packet.Type.HELLO -> this.writePacket(new Packet(packet.id, Packet.Type.HELLO, false));
                case Packet.Type.CLOSE -> this.close();
                case Packet.Type.OBJECT_LIST -> this.writePacket(new Packet.PObjectList(packet.id, this.getObjectPool()));
                case Packet.Type.METHOD_LIST -> this.writePacket(new Packet.PMethodList(packet.id, ((Packet.PMethodList) packet).oid, false));
            }
            this.ostream.flush();
        } else buffer.add(packet);
    }

    Packet sendAndReceive(Packet packet) throws IOException {
        int id
        synchronized (this) {
            id = this.writePacket(packet)
            this.ostream.flush()
        }
        while (checkBuffer(id))
            Thread.onSpinWait()
        return this.buffer.stream().filter(p -> p.id == id).findFirst().orElseThrow(IOException::new)
    }

    synchronized boolean checkBuffer(int id) {
        return this.buffer.stream().noneMatch(p -> p.id == id)
    }

    synchronized int writePacket(Packet packet) throws IOException {
        this.ostream.write(7);
        packet.write(this);
        return packet.id;
    }

    synchronized void writeARType(ARType type) throws IOException {
        this.ostream.write(10);
        this.ostream.writeInt(type.dim);
        this.writeEnum(type.type);
    }

    synchronized void writeEnum(Enum value) throws IOException {
        this.ostream.write(6);
        this.writeStringI(value.name());
    }

    synchronized void writeEnum(String value) throws IOException {
        this.ostream.write(6);
        this.writeStringI(value);
    }

    synchronized void writeString(String value) throws IOException {
        this.ostream.write(0);
        this.writeStringI(value);
    }

    protected synchronized void writeStringI(String value) throws IOException {
        var bytes = value.getBytes(StandardCharsets.UTF_8);
        this.ostream.writeInt(bytes.length);
        this.ostream.write(bytes);
    }

    synchronized void writeDouble(double value) throws IOException {
        this.ostream.write(1);
        this.ostream.writeDouble(value);
    }

    synchronized void writeLong(long value) throws IOException {
        this.ostream.write(2);
        this.ostream.writeLong(value);
    }

    synchronized void writeInt(int value) throws IOException {
        this.ostream.write(3);
        this.ostream.writeInt(value);
    }

    synchronized void writeShort(short value) throws IOException {
        this.ostream.write(4);
        this.ostream.writeShort(value);
    }

    synchronized void writeByte(byte value) throws IOException {
        this.ostream.write(5);
        this.ostream.writeByte(value);
    }

    synchronized void writeBoolean(boolean value) throws IOException {
        this.writeByte((byte) (value ? 1 : 0));
    }

    synchronized Packet readPacket() throws IOException {
        this.checkValue(7);
        return Packet.read(this);
    }

    synchronized ARType readARType() throws IOException {
        this.checkValue(10);
        return new ARType(this.istream.readInt(), this.readEnum(PType.class));
    }

    @SuppressWarnings("unchecked")
    synchronized <T extends Enum<T>> T readEnum(Class<T> clazz) throws IOException {
        this.checkValue(6);
        var name = this.readStringI()
        try {
            return Arrays.stream((T[]) clazz.values()).filter(o -> o.name() == name).findFirst().orElseThrow()
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    synchronized String readEnum() throws IOException {
        this.checkValue(6);
        return this.readStringI();
    }

    synchronized String readString() throws IOException {
        this.checkValue(0);
        return this.readStringI();
    }

    synchronized String readStringI() throws IOException {
        return new String(this.istream.readNBytes(this.istream.readInt()));
    }

    synchronized double readDouble() throws IOException {
        this.checkValue(1);
        return this.istream.readDouble();
    }

    synchronized long readLong() throws IOException {
        this.checkValue(2);
        return this.istream.readLong();
    }

    synchronized int readInt() throws IOException {
        this.checkValue(3);
        return this.istream.readInt();
    }

    synchronized short readShort() throws IOException {
        this.checkValue(4);
        return this.istream.readShort();
    }

    synchronized byte readByte() throws IOException {
        this.checkValue(5);
        return this.istream.readByte();
    }

    synchronized boolean readBoolean() throws IOException {
        return (int) this.readByte() == 1;
    }

    protected synchronized void checkValue(int needed) throws IOException {
        var i = this.istream.read();
        if (i != needed)
            throw new IOException("Invalid Value {" + i + "}! Required {" + needed + "}!");
    }

    @Override
    synchronized void close() throws IOException {
        this.istream.close();
        this.ostream.close();
    }
}
