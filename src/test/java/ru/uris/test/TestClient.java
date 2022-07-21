package ru.uris.test;

import ru.uris.Client;
import ru.uris.ObjectServer;
import ru.uris.Type;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class TestClient {
    public static void main(String[] args) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        try (var client = new Client("localhost", 2022)) {
            client.runListener();

            var obj = client.getRemoteObject(0);
            var result = obj.invokeMethod("add", new Type[]{Type.INT}, 21);
            System.out.println(result);

            var obj0 = (IAdder) client.getRemoteObject(0, IAdder.class);
            var result0 = obj0.add(21);
            System.out.println(result0);
        }
    }

//    public static void main(String[] args) throws IOException, InvocationTargetException, IllegalAccessException {
//        var client = new Client("localhost", 2022);
//        client.runListener();
//        var count = client.sendPacketObjectList().count;
//        System.out.println(count);
//        var mid = Arrays.stream(client.sendPacketMethodList(count - 1).methods).filter(m -> m.name.equals("hashCode")).findFirst().orElseThrow().methodId;
//        System.out.println(mid);
//        System.out.println(client.sendInvoke(0, mid, new Packet.Invoke.Argument[0]).value);
//    }
}
