package ru.uris.test;

import ru.uris.Server;

import java.io.IOException;

public class TestServer {
    public static void main(String[] args) throws IOException {
        try (var server = new Server(2022)) {
            server.pool.add(new TestClass(12));
            server.accept().runListener();
//            server.accept().runListener();
        }
    }
}
