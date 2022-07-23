package ru.uris.test;

import ru.uris.Packet;
import ru.uris.PacketType;
import ru.uris.Server;

public class TestServer1 {
    public static void main(String[] args) throws Exception {
        try (var server = new Server(2022)) {
            var connection = server.accept();
            connection.runListener();
            connection.syncWait(() -> {
                System.out.println(connection.readPacket().type);
                connection.writePacket(new Packet(PacketType.TEST_PACKET));
                connection.send();
                connection.sendPacketHello();
                return null;
            });
            connection.writePacket(new Packet(PacketType.TEST_PACKET));
            connection.send();
        }

//        try (var server = new ServerSocket(2022)) {
//            try (var connection = server.accept()) {
//                var is = new DataInputStream(connection.getInputStream());
//                var os = new DataOutputStream(connection.getOutputStream());
//                os.writeShort(467);
//            }
//        }
    }
}
