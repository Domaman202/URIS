package ru.uris.gadapter

import ru.uris.Server

class GroovyServer extends Server {
    GroovyServer(int port) throws IOException {
        super(port)
    }

    @Override
    Connection accept() throws IOException {
        return new Connection(this.socket.accept())
    }

    class Connection extends Server.Connection implements GroovySocket {
        Connection(Socket socket) throws IOException {
            super(GroovyServer.this, socket)
        }
    }
}
