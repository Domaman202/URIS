package ru.kuris

import ru.kuris.ARType.Companion.of
import java.io.IOException
import kotlin.reflect.KClass
import kotlin.reflect.full.valueParameters

open class Packet(@JvmField val id: Int, @JvmField val type: Type, @JvmField val request: Boolean) {
    open fun write(socket: ObjectProviderSocket) {
        socket.writeInt(id)
        socket.writeEnum(type)
        socket.writeBoolean(request)
    }

    companion object {
        @JvmField
        var LAST_ID = 0

        @JvmStatic
        fun nextId(): Int = LAST_ID++

        @JvmStatic
        @Throws(IOException::class)
        fun read(socket: ObjectProviderSocket): Packet {
            val id = socket.readInt()
            val type = socket.readEnum(Type::class.java)
            val request = socket.readBoolean()
            return when (type) {
                Type.HELLO, Type.CLOSE -> Packet(id, type, request)
                Type.OBJECT_LIST -> if (request) PObjectList(id) else PObjectList(id, socket)
                Type.METHOD_LIST -> if (request) PMethodList(id, socket.readInt(), true) else PMethodList(id, socket)
            }
        }

        class PMethodList : Packet {
            @JvmField
            val oid: Int

            @JvmField
            val methods: Array<RemoteMethod>

            constructor(pid: Int, oid: Int, request: Boolean) : super(pid, Type.METHOD_LIST, request) {
                this.oid = oid
                this.methods = Array(0) { throw UnsupportedOperationException() }
            }

            constructor(pid: Int, socket: ObjectProviderSocket) : super(pid, Type.METHOD_LIST, false) {
                oid = socket.readInt()
                val mc = socket.readInt()
                val methods = Array<RemoteMethod?>(mc) { null }
                for (i in 0 until mc) {
                    val name = socket.readString()
                    val ac = socket.readInt()
                    val args = Array<ARType?>(ac) { null }
                    for (j in 0 until ac)
                        args[j] = socket.readARType()
                    methods[i] = RemoteMethod(name, args as Array<ARType>, socket.readARType())
                }
                this.methods = methods as Array<RemoteMethod>
            }

            override fun write(socket: ObjectProviderSocket) {
                super.write(socket)
                socket.writeInt(oid)
                if (request)
                    return
                val methods = socket.ObjectPool()[oid]::class.members
                socket.writeInt(methods.size)
                for (method in methods) {
                    socket.writeString(method.name)
                    socket.writeInt(method.valueParameters.size)
                    for (arg in method.valueParameters)
                        socket.writeARType(of(arg.type.classifier as KClass<out Any>))
                    socket.writeARType(of(method.returnType.classifier as KClass<out Any>))
                }
            }
        }

        class PObjectList : Packet {
            @JvmField
            val objects: LongArray

            constructor(id: Int) : super(id, Type.OBJECT_LIST, true) {
                objects = LongArray(0)
            }

            constructor(id: Int, objects: List<Any>) : super(id, Type.OBJECT_LIST, false) {
                this.objects = objects.stream().mapToLong { o -> o.hashCode().toLong() }.toArray()
            }

            constructor(id: Int, socket: ObjectProviderSocket) : super(id, Type.OBJECT_LIST, false) {
                objects = LongArray(socket.readInt())
                for (i in objects.indices)
                    objects[i] = socket.readLong()
            }

            @Throws(IOException::class)
            override fun write(socket: ObjectProviderSocket) {
                super.write(socket)
                if (!request) {
                    socket.writeInt(objects.size)
                    for (obj in objects)
                        socket.writeLong(obj)
                }
            }
        }
    }

    enum class Type {
        HELLO,
        CLOSE,

        OBJECT_LIST,
        METHOD_LIST
    }
}