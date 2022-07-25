package ru.uris;

import ru.DmN.ReflectionUtils;

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
        var request = socket.readBoolean();
        return switch (type) {
            case HELLO, CLOSE -> new Packet(id, type, request);
            case OBJECT_LIST -> request ? new PObjectList(id) : new PObjectList(id, socket);
            case METHOD_LIST -> request ? new Packet.PMethodList(id, socket.readInt(), true) : new PMethodList(id, socket);
        };
    }

    public void write(ObjectProviderSocket socket) throws IOException {
        socket.writeInt(id);
        socket.writeEnum(this.type);
        socket.writeBoolean(this.request);
    }

    public static class PMethodList extends Packet {
        public final int objectId;
        public final RemoteMethod[] methods;

        public PMethodList(int pid, int oid, boolean request) {
            super(pid, Type.METHOD_LIST, request);
            this.objectId = oid;
            this.methods = null;
        }

        public PMethodList(int pid, ObjectProviderSocket socket) throws IOException {
            super(pid, Type.METHOD_LIST, false);
            this.objectId = socket.readInt();
            var mc = socket.readInt();
            this.methods = new RemoteMethod[mc];
            for (int i = 0; i < mc; i++) {
                var name = socket.readStringI();
                var ac = socket.readInt();
                var args = new VType[ac];
                for (int j = 0; j < ac; j++) {
                    args[j] = VType.of(socket.readByte());
                }
                this.methods[i] = new RemoteMethod(name, args, VType.of(socket.readByte()));
            }
        }

        @Override
        public void write(ObjectProviderSocket socket) throws IOException {
            super.write(socket);
            socket.writeInt(this.objectId);
            if (this.request)
                return;
            var methods = ReflectionUtils.getAllMethods(socket.ObjectPool().get(this.objectId).getClass());
            socket.writeInt(methods.length);
            for (var method : methods) {
                socket.writeStringI(method.getName());
                socket.writeInt(method.getParameterCount());
                for (var arg : method.getParameterTypes()) {
                    socket.writeByte((byte) VType.of(arg).id);
                }
                socket.writeByte((byte) VType.of(method.getReturnType()).id);
            }
        }
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

        OBJECT_LIST,
        METHOD_LIST
    }
}