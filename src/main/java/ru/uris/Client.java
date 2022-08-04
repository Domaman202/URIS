package ru.uris;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client extends ObjectProviderSocket {
    public final List<Object> objectPool = new ArrayList<>();

    public Client(String host, int port) throws IOException {
        super(new Socket(host, port));
    }

    @Override
    public List<Object> getObjectPool() {
        return this.objectPool;
    }

    @Override
    public synchronized void close() throws IOException {
        if (!this.socket.isClosed()) {
            this.writePacket(new Packet(Packet.nextId(), Packet.Type.CLOSE, true));
            this.ostream.flush();
        }
        super.close();
    }
}
