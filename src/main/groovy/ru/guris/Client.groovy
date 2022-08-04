package ru.guris

class Client extends ObjectProviderSocket {
    final List<Object> objectPool = new ArrayList<>()

    Client(String host, int port) {
        super(new Socket(host, port))
    }

    @Override
    synchronized void close() throws IOException {
        if (!this.socket.closed) {
            this.writePacket(new Packet(Packet.nextId(), Packet.Type.CLOSE, true))
            this.ostream.flush()
        }
        super.close()
    }
}
