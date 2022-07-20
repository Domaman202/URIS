package ru.uris;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static ru.uris.Packet.Invoke.Argument.read;

public class Packet {
    public final PacketType type;

    public Packet(PacketType type) {
        this.type = type;
    }

    public void write(ObjectServer server) throws IOException {
        server.writeEnum(this.type);
    }

    public static class Return extends Packet {
        public int id;
        public final Object value;

        public Return(Object value) {
            super(PacketType.RETURN);
            this.value = value;
        }

        public Return(ObjectServer server) throws IOException {
            super(PacketType.RETURN);
            this.id = server.is.readInt();
            this.value = read(server, server.readEnum(Type.class));
        }

        @Override
        public void write(ObjectServer server) throws IOException {
            super.write(server);
            var value = this.value;
            var type = value == null ? Type.NULL : Type.of(value.getClass());
            if (type == Type.OBJECT) {
                server.objectPool().add(value);
                Invoke.write(server, type, value, server.objectPool().indexOf(value));
            } else Invoke.write(server, type, value, -1);
        }
    }

    public static class Invoke extends Packet {
        public final Argument[] arguments;
        public final int objectId;
        public final int methodId;

        public Invoke(int objectId, int methodId, Argument[] arguments) {
            super(PacketType.INVOKE);
            this.objectId = objectId;
            this.methodId = methodId;
            this.arguments = arguments;
        }

        public Invoke(ObjectServer server) throws IOException {
            super(PacketType.INVOKE);
            this.objectId = server.is.readInt();
            this.methodId = server.is.readInt();
            var i = server.is.readInt();
            this.arguments = new Argument[i];
            for (int j = 0; j < i; j++)
                this.arguments[j] = new Argument(server);
        }

        @Override
        public void write(ObjectServer server) throws IOException {
            super.write(server);
            server.os.writeInt(this.objectId);
            server.os.writeInt(this.methodId);
            server.os.writeInt(arguments.length);
            for (var arg : this.arguments) {
                write(server, arg.type, arg.value, arg.id);
            }
        }

        public static void write(ObjectServer server, Type type, Object value, int id) throws IOException {
            server.os.writeInt(id);
            server.writeEnum(type);
            switch (type) {
                case BYTE -> server.os.writeByte((Integer) value);
                case SHORT -> server.os.writeShort((Integer) value);
                case INT -> server.os.writeInt((Integer) value);
                case LONG -> server.os.writeLong((Long) value);
                case DOUBLE -> server.os.writeDouble((Double) value);
                case STRING -> server.writeString((String) value);
                case OBJECT -> server.os.writeInt(id);
            }
        }

        public static class Argument {
            public final int id;
            public final Type type;
            public final Object value;

            public Argument(ObjectServer server, Type type, Object value) {
                this.id = type == Type.OBJECT ? server.objectPool().indexOf(value) : -1;
                this.type = type;
                this.value = value;
            }

            public Argument(ObjectServer server) throws IOException {
                this.id = server.is.readInt();
                this.type = server.readEnum(Type.class);
                this.value = read(server, this.type);
            }

            public static Object read(ObjectServer server, Type type) throws IOException {
                return switch (type) {
                    case BYTE -> server.is.readByte();
                    case SHORT -> server.is.readShort();
                    case INT, OBJECT -> server.is.readInt();
                    case LONG -> server.is.readLong();
                    case DOUBLE -> server.is.readDouble();
                    case STRING -> server.readString();
                    case NULL -> null;
                };
            }
        }
    }

    public static class ObjectList extends Packet {
        public final int count;

        public ObjectList(int count) {
            super(PacketType.OBJECT_LIST);
            this.count = count;
        }

        @Override
        public void write(ObjectServer server) throws IOException {
            super.write(server);
            server.os.writeInt(this.count);
        }
    }

    public static class MethodList extends Packet {
        public final MethodInfo[] methods;

        public MethodList(Method[] methods) {
            super(PacketType.METHOD_LIST);
            this.methods = new MethodInfo[methods.length];
            for (int i = 0; i < methods.length; i++)
                this.methods[i] = new MethodInfo(methods[i], i);
        }

        public MethodList(ObjectServer server) throws IOException {
            super(PacketType.METHOD_LIST);
            this.methods = new MethodInfo[server.is.readInt()];
            for (int i = 0; i < this.methods.length; i++) {
                var name = server.readString();
                var ret = server.readEnum(Type.class);
                var argc = server.is.readInt();
                var args = new Type[argc];
                for (int j = 0; j < argc; j++) {
                    args[j] = server.readEnum(Type.class);
                }
                this.methods[i] = new MethodInfo(i, name, args, ret);
            }
        }

        @Override
        public void write(ObjectServer server) throws IOException {
            super.write(server);
            server.os.writeInt(this.methods.length);
            for (var method : this.methods) {
                server.writeString(method.name);
                server.writeEnum(method.ret);
                server.os.writeInt(method.args.length);
                for (var arg : method.args) {
                    server.writeEnum(arg);
                }
            }
        }

        public static class MethodInfo {
            public final int methodId;
            public final String name;
            public final Type[] args;
            public final Type ret;

            public MethodInfo(Method method, int methodId) {
                this.methodId = methodId;
                this.name = method.getName();
                this.args = Arrays.stream(method.getParameterTypes()).map(Type::of).toList().toArray(new Type[0]);
                this.ret = Type.of(method.getReturnType());
            }

            public MethodInfo(int methodId, String name, Type[] args, Type ret) {
                this.methodId = methodId;
                this.name = name;
                this.args = args;
                this.ret = ret;
            }
        }
    }
}
