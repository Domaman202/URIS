package ru.uris.test.java;

import ru.uris.Client;
import ru.uris.Server;

import java.io.IOException;
import java.lang.reflect.Array;

public class Main {
    public static void main(String[] args) throws IOException {
        new Thread(() -> {
            try {
                try (var server = new Server(2014)) {
                    server.accept().writeObject(new Object[]{});
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();

        try (var client = new Client("localhost", 2014)) {
            System.out.println(Array.getLength(client.readObject()));
        }
    }
}