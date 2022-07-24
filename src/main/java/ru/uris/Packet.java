package ru.uris;

import java.io.IOException;
import java.util.List;

@SuppressWarnings("unused")
public class Packet {
    public static int LAST_ID = 0;
    public final int id;
    public final Type type;
    public final boolean request;

    public Packet(int id, Type type, boolean request) {
        this.id = id;
        this.type = type;
        this.request = request;
    }

    public static int nextId() {
        return LAST_ID++;
    }

    public static Packet read(ObjectProviderSocket socket) throws IOException {
        var id = socket.readInt();
        var type = socket.readEnum(Type.class);
        return switch (type) {
            case HELLO, CLOSE -> new Packet(id, type, socket.readBoolean());
            case OBJECT_LIST -> socket.readBoolean() ? new PObjectList(id) : new PObjectList(id, socket);
        };
    }

    public void write(ObjectProviderSocket socket) throws IOException {
        socket.writeInt(id);
        socket.writeEnum(this.type.name());
        socket.writeBoolean(this.request);
    }

    public static class PObjectList extends Packet {
        public final long[] objects;

        public PObjectList(int id) {
            super(id, Type.OBJECT_LIST, true);
            this.objects = new long[0];
        }

        public PObjectList(int id, List<Object> objects) {
            super(id, Type.OBJECT_LIST, false);
            this.objects = objects.stream().mapToLong(Object::hashCode).toArray();
        }

        public PObjectList(int id, ObjectProviderSocket socket) throws IOException {
            super(id, Type.OBJECT_LIST, false);
            this.objects = new long[socket.readInt()];
            for (int i = 0; i < this.objects.length; i++) {
                this.objects[i] = socket.readLong();
            }
        }

        @Override
        public void write(ObjectProviderSocket socket) throws IOException {
            super.write(socket);
            if (!this.request) {
                socket.writeInt(objects.length);
                for (long object : objects) {
                    socket.writeLong(object);
                }
            }
        }
    }

    public enum Type {
        HELLO,
        CLOSE,

        OBJECT_LIST
    }
}
