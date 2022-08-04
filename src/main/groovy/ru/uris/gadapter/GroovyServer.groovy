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

    class Connection extends Server.Connection {
        Connection(Socket socket) throws IOException {
            super(GroovyServer.this, socket)
        }

        @Override
        synchronized <T extends Enum<T>> T readEnum(Class<T> clazz) throws IOException {
            this.checkValue(6);
            var name = this.readStringI();
            try {
                return Arrays.stream((T[]) clazz.values()).filter(o -> o.name() == name).findFirst().orElseThrow();
            } catch (Throwable e) {
                throw new IOException(e);
            }
        }
    }
}
