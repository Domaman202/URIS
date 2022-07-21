package ru.uris.test;

import ru.uris.Server;

import java.io.IOException;

public class TestServer {
    public static void main(String[] args) throws IOException {
        try (var server = new Server(2022)) {
            server.pool.add(new TestClass(12));
            server.pool.add(new TestClass(999));
            server.pool.add(TestServer.class.getClassLoader());
            server.accept().runListener();
        }
    }
}
