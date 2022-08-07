package ru.uris.gadapter

import ru.uris.ObjectProviderSocket
import ru.uris.Packet
import ru.uris.RemoteMethod

class GroovyRemoteObjectImpl implements ObjectProviderSocket.RemoteObject {
    Object instance
    RemoteMethod[] methods

    GroovyRemoteObjectImpl(ObjectProviderSocket socket, int id) {
        this.instance = socket.getObjectPool()[id]
        this.methods = ((Packet.PMethodList) socket.sendAndReceive(new Packet.PMethodList(Packet.nextId(), id, true))).methods
    }

    @Override
    Object invoke(String name, Object... args) throws IOException, NoSuchMethodException {
        return instance.invokeMethod(name, args)
    }
}
