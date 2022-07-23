package ru.uris.test;

import ru.uris.Client;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class TestClient1 {
    public static void main(String[] args) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InterruptedException {
        try (var client = new Client("localhost", 2022)) {
            client.runListener();
            client.syncWaitCode(() -> {
                client.sendPacketHello();
                System.out.println(client.readPacket().type);
                System.out.println(client.readPacket().type);
                return null;
            });
            Thread.sleep(1000);
        }
    }
}
