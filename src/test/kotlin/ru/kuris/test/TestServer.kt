package ru.kuris.test

import kotlinx.coroutines.runBlocking
import ru.kuris.Packet
import ru.kuris.Packet.Companion.nextId
import ru.kuris.Server

object TestServer {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        Server(2014).use { server ->
            server.objectPool.add(Any())
            server.objectPool.add(TestClass(12))
            server.objectPool.add(TestServer::class.java.classLoader)
            val connection = server.accept()
            connection.createListener().start()
            println((connection.sendAndReceive(Packet.Companion.PObjectList(nextId())) as Packet.Companion.PObjectList).objects.contentToString())
            println(
                (connection.sendAndReceive(
                    Packet.Companion.PMethodList(
                        nextId(),
                        0,
                        true
                    )
                ) as Packet.Companion.PMethodList).methods.contentToString()
            )
            println()
            for (method in (connection.sendAndReceive(
                Packet.Companion.PMethodList(
                    nextId(),
                    0,
                    true
                )
            ) as Packet.Companion.PMethodList).methods)
                println(
                    """
                    [NAME]${method.name}
                    [ARGS]:${method.args.contentToString()}
                    [RETURN]:${method.ret}

                    """.trimIndent()
                )
        }
    }
}