package ru.kuris

import java.io.Closeable
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

open class Server(port: Int) : Closeable {
    @JvmField
    val objectPool = ArrayList<Any>()

    @JvmField
    protected val socket: ServerSocket = ServerSocket(port)

    @Throws(IOException::class)
    fun accept(): Connection {
        return Connection(socket.accept())
    }

    val isClosed: Boolean
        get() = socket.isClosed

    @Throws(IOException::class)
    override fun close() {
        socket.close()
    }

    inner class Connection(socket: Socket) : ObjectProviderSocket(socket), AutoCloseable {
        override fun ObjectPool(): List<Any> {
            return objectPool
        }
    }
}
