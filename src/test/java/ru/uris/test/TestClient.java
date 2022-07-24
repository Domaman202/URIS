package ru.uris.test;

import ru.uris.Client;
import ru.uris.Packet;

import java.io.IOException;
import java.util.Arrays;

public class TestClient {
    public static void main(String[] args) throws IOException {
        try (var client = new Client("localhost", 2014)) {
            client.objectPool.add(new Object());

            client.createListener().start();

            System.out.println(Arrays.toString(((Packet.PObjectList) client.sendAndReceive(new Packet.PObjectList(Packet.nextId()))).objects));
            System.out.println(Arrays.toString(((Packet.PObjectList) client.sendAndReceive(new Packet.PObjectList(Packet.nextId()))).objects));
        }
    }
}
