package ru.kuris

import ru.kuris.Packet.Companion.nextId
import java.io.IOException
import java.net.Socket

open class Client(host: String?, port: Int) : ObjectProviderSocket(Socket(host, port)) {
    @JvmField
    val objectPool = ArrayList<Any>()

    override fun ObjectPool(): List<Any> {
        return objectPool
    }

    @Throws(IOException::class)
    override fun close() {
        if (!socket.isClosed) {
            writePacket(Packet(nextId(), Packet.Type.CLOSE, true))
            ostream.flush()
        }
        super.close()
    }
}