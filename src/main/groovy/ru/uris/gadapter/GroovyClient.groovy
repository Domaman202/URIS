package ru.uris.gadapter

import ru.uris.Client

class GroovyClient extends Client implements GroovySocket {
    GroovyClient(String host, int port) throws IOException {
        super(host, port)
    }
}
