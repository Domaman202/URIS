package ru.uris.test;

import ru.uris.Client;
import ru.uris.Packet;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class TestClient {
    public static void main(String[] args) throws IOException, InvocationTargetException, IllegalAccessException {
        try (var client = new Client("localhost", 2022)) {
            client.runListener();
            var count = client.sendPacketObjectList().count;
            System.out.println(count);
            var mid = Arrays.stream(client.sendPacketMethodList(count - 1).methods).filter(m -> m.name.equals("hashCode")).findFirst().orElseThrow().methodId;
            System.out.println(mid);
            System.out.println(client.sendInvoke(0, mid, new Packet.Invoke.Argument[0]).value);
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
