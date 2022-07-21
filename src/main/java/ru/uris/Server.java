package ru.uris;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server implements Closeable {
    protected final List<Connection> connections = new ArrayList<>();
    protected final ServerSocket socket;
    public final List<Object> pool = new ArrayList<>();

    public Server(int port) throws IOException {
        this.socket = new ServerSocket(port);
    }

    public Connection accept() throws IOException {
        return new Connection(this.socket.accept());
    }

    @Override
    public void close() throws IOException {
        this.socket.close();
    }

    public class Connection extends ObjectServer {
        protected Connection(Socket socket) throws IOException {
            super(socket);
            this.sendPacketHello();
            if (this.readPacket().type != PacketType.HELLO_PACKET)
                throw new IOException("Connection error!");
            System.out.println("Connection success!");
            Server.this.connections.add(this);
        }

        @Override
        public List<Object> objectPool() {
            return Server.this.pool;
        }

        @Override
        public Packet listen() throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
            var packet = this.readPacket();
            if (packet.type == PacketType.CLOSE) {
                this.socket.close();
                this.close();
                return packet;
            }
            return super.listen(packet);
        }
    }
}
