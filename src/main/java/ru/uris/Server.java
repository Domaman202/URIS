package ru.uris;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server implements Closeable {
    public final List<Object> objectPool = new ArrayList<>();
    protected final ServerSocket socket;

    public Server(int port) throws IOException {
        this.socket = new ServerSocket(port);
    }

    public Connection accept() throws IOException {
        return new Connection(this.socket.accept());
    }

    public boolean isClosed() {
        return this.socket.isClosed();
    }

    @Override
    public void close() throws IOException {
        this.socket.close();
    }

    public class Connection extends ObjectProviderSocket implements AutoCloseable {
        public Connection(Socket socket) throws IOException {
            super(socket);
        }

        @Override
        public List<Object> getObjectPool() {
            return Server.this.objectPool;
        }
    }
}
