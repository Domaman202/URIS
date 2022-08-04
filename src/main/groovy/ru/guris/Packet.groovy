package ru.guris

import ru.DmN.ReflectionUtils

class Packet {
    static def LAST_ID = 0
    final int id
    final Type type
    final boolean request

    Packet(int id, Type type, boolean request) {
        this.id = id
        this.type = type
        this.request = request
    }

    static int nextId() {
        return LAST_ID++
    }

    static Packet read(ObjectProviderSocket socket) throws IOException {
        var id = socket.readInt()
        var type = socket.readEnum(Type.class)
        var request = socket.readBoolean()
        return switch (type) {
            case Type.HELLO, Type.CLOSE -> new Packet(id, type, request)
            case Type.OBJECT_LIST -> request ? new PObjectList(id) : new PObjectList(id, socket)
            case Type.METHOD_LIST -> request ? new PMethodList(id, socket.readInt(), true) : new PMethodList(id, socket)
        };
    }

    void write(ObjectProviderSocket socket) {
        socket.writeInt(id)
        socket.writeEnum(this.type)
        socket.writeBoolean(this.request)
    }

    static class PMethodList extends Packet {
        final int oid
        final RemoteMethod[] methods

        PMethodList(int pid, int oid, boolean request) {
            super(pid, Type.METHOD_LIST, request)
            this.oid = oid
            this.methods = null
        }

        PMethodList(int pid, ObjectProviderSocket socket) throws IOException {
            super(pid, Type.METHOD_LIST, false)
            this.oid = socket.readInt()
            var mc = socket.readInt()
            this.methods = new RemoteMethod[mc]
            for (int i = 0; i < mc; i++) {
                var name = socket.readString()
                var ac = socket.readInt()
                var args = new ARType[ac]
                for (int j = 0; j < ac; j++)
                    args[j] = socket.readARType()
                this.methods[i] = new RemoteMethod(name, args, socket.readARType())
            }
        }

        @Override
        void write(ObjectProviderSocket socket) throws IOException {
            super.write(socket)
            socket.writeInt(this.oid)
            if (this.request)
                return
            var methods = ReflectionUtils.getAllMethods(socket.getObjectPool().get(this.oid).class)
            socket.writeInt(methods.length)
            for (method in methods) {
                socket.writeString(method.getName())
                socket.writeInt(method.getParameterCount())
                for (arg in method.getParameterTypes())
                    socket.writeARType(ARType.of(arg))
                socket.writeARType(ARType.of(method.getReturnType()))
            }
        }
    }

    static class PObjectList extends Packet {
        final long[] objects

        PObjectList(int id) {
            super(id, Type.OBJECT_LIST, true)
            this.objects = new long[0]
        }

        PObjectList(int id, List<Object> objects) {
            super(id, Type.OBJECT_LIST, false)
            this.objects = objects.stream().mapToLong(o -> (long) o.hashCode()).toArray()
        }

        PObjectList(int id, ObjectProviderSocket socket) throws IOException {
            super(id, Type.OBJECT_LIST, false)
            this.objects = new long[socket.readInt()]
            for (int i = 0; i < this.objects.length; i++)
                this.objects[i] = socket.readLong()
        }

        @Override
        void write(ObjectProviderSocket socket) throws IOException {
            super.write(socket);
            if (!this.request) {
                socket.writeInt(objects.length)
                for (long object : objects)
                    socket.writeLong(object)
            }
        }
    }

    enum Type {
        HELLO,
        CLOSE,

        OBJECT_LIST,
        METHOD_LIST
    }
}
