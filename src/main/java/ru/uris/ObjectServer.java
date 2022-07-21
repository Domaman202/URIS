package ru.uris;

import ru.DmN.ReflectionUtils;

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
import java.util.concurrent.atomic.AtomicInteger;

import static ru.uris.Packet.Invoke.writeWithType;

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
            } catch (IOException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                if (e.toString().contains("Socket closed")) {
                    this.listener = null;
                    return;
                }
                throw new RuntimeException(e);
            }
        });
        this.listener.start();
    }

    public Object sync(SyncFunction code) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        var old = this.sync;
        this.sync = true;
        var ret = code.run();
        this.sync = old;
        return ret;
    }

    public Packet sendAndRead() throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        this.send();
        return this.listen();
    }

    public Packet listen() throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        return listen(this.readPacket());
    }

    public Packet listen(Packet packet) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        var pool = this.objectPool();
        return switch (packet.type) {
            case TEST_PACKET -> {
                this.writeString("Hello, World!");
                this.writeEnum(PacketType.TEST_PACKET);
                this.writeDouble(202.213);
                this.writeObject(new Packet(PacketType.HELLO_PACKET));
                this.writeObject(new AtomicInteger(777));
                yield packet;
            }
            case OBJECT_LIST_REQUEST -> {
                this.writePacket(new Packet.ObjectList(pool.size()));
                this.send();
                yield packet;
            }
            case OBJECT_LIST -> new Packet.ObjectList(this.is.readInt());
            case METHOD_LIST_REQUEST -> {
                this.writePacket(new Packet.MethodList(ReflectionUtils.getAllMethods(pool.get(this.is.readInt()).getClass())));
                this.send();
                yield packet;
            }
            case METHOD_LIST -> new Packet.MethodList(this);
            case INVOKE -> {
                var p = new Packet.Invoke(this);
                var obj = pool.get(p.objectId);
                var method = ReflectionUtils.getAllMethods(obj.getClass())[p.methodId];
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

    public RemoteObject getRemoteObject(int objectId) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        return new RemoteObject(objectId);
    }

    public Packet.Return sendInvoke(int objectId, int methodId, Packet.Invoke.Argument[] arguments) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        this.writePacket(new Packet.Invoke(objectId, methodId, arguments));
        return (Packet.Return) this.sendAndRead();
    }

    public Packet.MethodList sendPacketMethodList(int id) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        return (Packet.MethodList) this.sync(() -> {
            this.writePacket(new Packet(PacketType.METHOD_LIST_REQUEST));
            this.os.writeInt(id);
            return this.sendAndRead();
        });
    }

    public Packet.ObjectList sendPacketObjectList() throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        return (Packet.ObjectList) this.sync(() -> {
            this.writePacket(new Packet(PacketType.OBJECT_LIST_REQUEST));
            return this.sendAndRead();
        });
    }

    public void sendPacketHello() throws IOException {
        this.writePacket(new Packet(PacketType.HELLO_PACKET));
        this.send();
    }

    public void send() throws IOException {
        this.os.flush();
    }

    public void writeObject(Object object) throws IOException {
        if (object instanceof Packet packet)
            this.writePacket(packet);
        else if (object instanceof Enum<?> e)
            this.writeEnum(e);
        else {
            var type = Type.of(object);
            this.os.write(type.getId());
            var index = -1;
            if (type == Type.OBJECT) {
                this.objectPool().add(object);
                index = this.objectPool().indexOf(object);
            }
            writeWithType(this, type, object, index);
        }
    }

    public void writePacket(Packet packet) throws IOException {
        this.os.write(7);
        packet.write(this);
    }

    public void writeEnum(Enum<?> element) throws IOException {
        this.os.write(6);
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

    public Object readObject() throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        var type = this.is.read();
        return switch (type) {
            case 7 -> this.readPacketI();
            case 6 -> this.readString();
            case 5 -> this.is.readByte();
            case 4 -> this.is.readShort();
            case 3 -> this.is.readInt();
            case 2 -> this.is.readLong();
            case 1 -> this.is.readDouble();
            case 0 -> this.readStringI();
            case 8 -> this.getRemoteObject(this.is.readInt());
            case 9 -> null;
            default -> throw new IllegalStateException("Invalid type: " + type);
        };
    }

    public Packet readPacket() throws IOException {
        if (this.is.read() != 7)
            throw new IOException("Invalid value!");
        return this.readPacketI();
    }

    public Packet readPacketI() throws IOException {
        return new Packet(this.readEnum(PacketType.class));
    }

    public <T extends Enum<T>> T readEnum(Class<T> clazz) throws IOException {
        if (this.is.read() != 6)
            throw new IOException("Invalid value!");
        return this.readEnumI(clazz);
    }

    public <T extends Enum<T>> T readEnumI(Class<T> clazz) throws IOException {
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
        return this.readStringI();
    }

    public String readStringI() throws IOException {
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

    @FunctionalInterface
    public interface SyncFunction {
        Object run() throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException;
    }

    public class RemoteObject {
        public final int id;
        public final Packet.MethodList.MethodInfo[] methods;

        public RemoteObject(int id) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
            this.id = id;
            this.methods = ObjectServer.this.sendPacketMethodList(id).methods;
        }

        @SuppressWarnings("unchecked")
        public <T> T createProxy(Class<T> intf) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
            return (T) createProxy(new Class[]{intf});
        }

        public Object createProxy(Class<?> ... interfaces) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
            return ObjectServer.this.sync(() -> {
                var instance = ObjectServer.this.getRemoteObject(this.id);
                return Proxy.newProxyInstance(ObjectServer.class.getClassLoader(), interfaces, (proxy, method, args) -> instance.invokeMethod(method.getName(), Arrays.stream(method.getParameterTypes()).map(Type::of).toArray(Type[]::new), args == null ? new Object[0] : args));
            });
        }

        public Object invokeMethod(String name, Type[] argts, Object... args) throws NoSuchMethodException, IOException, InvocationTargetException, IllegalAccessException {
            mloop:
            for (int j = 0; j < methods.length; j++) {
                var method = methods[j];
                if (method.name.equals(name)) {
                    if (method.args.length != argts.length)
                        continue;
                    for (int i = 0; i < method.args.length; i++)
                        if (method.args[i] != argts[i])
                            continue mloop;
                    int finalJ = j;
                    return ObjectServer.this.sync(() -> {
                        var ret = ObjectServer.this.sendInvoke(this.id, finalJ, Arrays.stream(args).map(o -> new Packet.Invoke.Argument(ObjectServer.this, o)).toList().toArray(new Packet.Invoke.Argument[0]));
                        return ret.type == Type.OBJECT ? ObjectServer.this.getRemoteObject(ret.id) : ret.value;
                    });
                }
            }
            throw new NoSuchMethodException(name);
        }
    }
}
