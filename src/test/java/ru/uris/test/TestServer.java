package ru.uris.test;

import ru.uris.Server;

import java.io.IOException;

public class TestServer {
    public static void main(String[] args) throws IOException {
        try (var server = new Server(2022)) {
            server.pool.add(new Object());
            server.accept().runListener();
        }
    }

//    public static void main(String[] args) throws IOException, InterruptedException {
//        try (var server = new Server(2022)) {
//            server.pool.add(new Object());
//            var connection = server.accept();
//            connection.runListener();
//            Thread.sleep(5000);
//            connection.close();
//        }
//    }
}
