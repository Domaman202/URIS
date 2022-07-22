package ru.uris;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client extends ObjectServer {
    public final List<Object> pool = new ArrayList<>();

    public Client(String host, int port) throws IOException {
        super(new Socket(host, port));
        if (this.readPacket().type != PacketType.HELLO)
            throw new IOException("Connection error!");
        this.sendPacketHello();
        System.out.println("Connection success!");
    }

    @Override
    public List<Object> objectPool() {
        return this.pool;
    }
}
