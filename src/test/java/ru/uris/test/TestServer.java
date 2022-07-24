package ru.uris.test;

import ru.uris.Packet;
import ru.uris.Server;

import java.io.IOException;
import java.util.Arrays;

public class TestServer {
    public static void main(String[] args) throws IOException {
        try (var server = new Server(2014)) {
            server.objectPool.add(new Object());
            server.objectPool.add(new TestClass(12));

            server.objectPool.add(TestServer.class.getClassLoader());
            var connection = server.accept();
            connection.createListener().start();

            System.out.println(Arrays.toString(((Packet.PObjectList) connection.sendAndReceive(new Packet.PObjectList(Packet.nextId()))).objects));
            System.out.println(Arrays.toString(((Packet.PObjectList) connection.sendAndReceive(new Packet.PObjectList(Packet.nextId()))).objects));
        }
    }
}
