package ru.uris.test;

import ru.uris.Server;

public class Main {
    public static void main(String[] args) throws Exception {
        try (var server = new Server(25565)) {
            System.out.println(server.accept());
        }

//        try (var server = new ServerSocket(25565)) {
//            try (var connection = server.accept()) {
//                var is = new DataInputStream(connection.getInputStream());
//                var os = new DataOutputStream(connection.getOutputStream());
//                System.out.println(is.read());
//                System.out.println(is.read());
//                System.out.println(is.read());
//                System.out.println(is.readLine());
//            }
//        }
    }
}
