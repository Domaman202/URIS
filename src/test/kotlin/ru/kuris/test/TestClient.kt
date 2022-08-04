package ru.kuris.test

import kotlinx.coroutines.runBlocking
import ru.kuris.Client
import ru.kuris.Packet
import ru.kuris.Packet.Companion.nextId

object TestClient {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        Client("localhost", 2014).use { client ->
            client.objectPool.add(Any())
            client.createListener().start()
            println((client.sendAndReceive(Packet.Companion.PObjectList(nextId())) as Packet.Companion.PObjectList).objects.contentToString())
            println(
                (client.sendAndReceive(
                    Packet.Companion.PMethodList(
                        nextId(),
                        0,
                        true
                    )
                ) as Packet.Companion.PMethodList).methods.contentToString()
            )
            println()
            for (method in (client.sendAndReceive(
                Packet.Companion.PMethodList(
                    nextId(),
                    1,
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