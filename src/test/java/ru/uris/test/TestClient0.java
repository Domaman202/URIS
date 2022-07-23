package ru.uris.test;

import ru.uris.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class TestClient0 {
    public static void main(String[] args) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        try (var client = new Client("localhost", 2022)) {
            client.runListener();

            var obj = client.getRemoteObject(0);
            var result = obj.invokeMethod("add", new Type[]{Type.INT}, 21);
            System.out.println(result);
            obj.invokeMethod("print", new Type[]{Type.OBJECT}, (Object) null);
            obj.invokeMethod("print", new Type[]{Type.OBJECT}, "Hello, World!");
            obj.invokeMethod("print", new Type[]{Type.OBJECT}, 12.21);

            var obj0 = client.getRemoteObject(1).createProxy(IAdder.class);
            var result0 = obj0.add(21);
            System.out.println(result0);

            client.sync(() -> {
                client.writePacket(new Packet(PacketType.TEST_PACKET0));
                client.send();
                System.out.println(client.readObject());
                System.out.println(client.readObject());
                System.out.println(client.readObject());
                System.out.println(client.readObject());
                System.out.println(((ObjectServer.RemoteObject) client.readObject()).invokeMethod("get", new Type[0]));
                return null;
            });

            var loader = client.getRemoteObject(2);
            System.out.println(loader);
            var clazz = (ObjectServer.RemoteObject) loader.invokeMethod("loadClass", new Type[]{Type.STRING}, "java.lang.Object");
            System.out.println(clazz);
            var instance = (ObjectServer.RemoteObject) clazz.invokeMethod("newInstance", new Type[0]);
            System.out.println(instance);
            var result1 = instance.invokeMethod("toString", new Type[0]);
            System.out.println(result1);
        }
    }
}
