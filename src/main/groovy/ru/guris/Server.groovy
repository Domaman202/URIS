package ru.guris

class Server implements Closeable {
    final List<Object> objectPool = new ArrayList<>();
    protected final ServerSocket socket;

    Server(int port) throws IOException {
        this.socket = new ServerSocket(port)
    }

    Connection accept() throws IOException {
        return new Connection(this.socket.accept())
    }

    @Override
    void close() throws IOException {
        this.socket.close()
    }

    class Connection extends ObjectProviderSocket implements AutoCloseable {
        Connection(Socket socket) throws IOException {
            super(socket);
        }

        @Override
        List<Object> getObjectPool() {
            return Server.this.objectPool;
        }
    }
}
