package ru.kuris

import kotlinx.coroutines.yield
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.*

abstract class ObjectProviderSocket(@JvmField protected val socket: Socket) : Closeable {
    @JvmField
    val istream: DataInputStream = DataInputStream(socket.getInputStream())

    @JvmField
    val ostream: DataOutputStream = DataOutputStream(socket.getOutputStream())

    @JvmField
    val buffer = ArrayList<Packet>()

    @JvmField
    protected var listener: Thread? = null

    abstract fun ObjectPool(): List<Any>

    fun createListener(): Thread {
        return (if (listener == null) {
            listener = Thread {
                while (!socket.isClosed) {
                    try {
                        if (istream.available() > 0)
                            listen()
                        else Thread.onSpinWait()
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    }
                }
            }
            listener
        } else listener)!!
    }

    @Throws(IOException::class)
    @Synchronized
    fun listen() {
        val packet = readPacket()
        if (packet.request) {
            when (packet.type) {
                Packet.Type.HELLO -> writePacket(Packet(packet.id, Packet.Type.HELLO, false))
                Packet.Type.CLOSE -> close()
                Packet.Type.OBJECT_LIST -> writePacket(Packet.Companion.PObjectList(packet.id, ObjectPool()))
                Packet.Type.METHOD_LIST -> writePacket(
                    Packet.Companion.PMethodList(
                        packet.id,
                        (packet as Packet.Companion.PMethodList).oid,
                        false
                    )
                )
            }
            ostream.flush()
        } else buffer.add(packet)
    }

    @Throws(IOException::class)
    suspend fun sendAndReceive(packet: Packet?): Packet? {
        var id: Int
        synchronized(this) {
            id = writePacket(packet!!)
            ostream.run { flush() }
        }
        while (checkBuffer(id)) yield()
        return buffer.stream().filter { p: Packet -> p.id == id }.findFirst().orElseThrow { IOException() }
    }

    @Synchronized
    fun checkBuffer(id: Int): Boolean {
        return buffer.stream().noneMatch { p: Packet -> p.id == id }
    }

    @Synchronized
    @Throws(IOException::class)
    fun writePacket(packet: Packet): Int {
        ostream.write(7)
        packet.write(this)
        return packet.id
    }

    @Synchronized
    @Throws(IOException::class)
    fun writeARType(type: ARType) {
        ostream.write(10)
        ostream.writeInt(type.dim)
        this.writeEnum(type.type)
    }

    @Synchronized
    @Throws(IOException::class)
    fun writeEnum(value: Enum<*>) {
        ostream.write(6)
        writeStringI(value.name)
    }

    @Synchronized
    @Throws(IOException::class)
    fun writeEnum(value: String) {
        ostream.write(6)
        writeStringI(value)
    }

    @Synchronized
    @Throws(IOException::class)
    fun writeString(value: String) {
        ostream.write(0)
        writeStringI(value)
    }

    @Synchronized
    @Throws(IOException::class)
    protected fun writeStringI(value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        ostream.writeInt(bytes.size)
        ostream.write(bytes)
    }

    @Synchronized
    @Throws(IOException::class)
    fun writeDouble(value: Double) {
        ostream.write(1)
        ostream.writeDouble(value)
    }

    @Synchronized
    @Throws(IOException::class)
    fun writeLong(value: Long) {
        ostream.write(2)
        ostream.writeLong(value)
    }

    @Synchronized
    @Throws(IOException::class)
    fun writeInt(value: Int) {
        ostream.write(3)
        ostream.writeInt(value)
    }

    @Synchronized
    @Throws(IOException::class)
    fun writeShort(value: Short) {
        ostream.write(4)
        ostream.writeShort(value.toInt())
    }

    @Synchronized
    @Throws(IOException::class)
    fun writeByte(value: Byte) {
        ostream.write(5)
        ostream.writeByte(value.toInt())
    }

    @Synchronized
    @Throws(IOException::class)
    fun writeBoolean(value: Boolean) {
        writeByte((if (value) 1 else 0).toByte())
    }

    @Synchronized
    @Throws(IOException::class)
    fun readPacket(): Packet {
        checkValue(7)
        return Packet.read(this)
    }

    @Synchronized
    @Throws(IOException::class)
    fun readARType(): ARType {
        checkValue(10)
        return ARType(istream.readInt(), this.readEnum(PType::class.java))
    }

    @Synchronized
    @Throws(IOException::class)
    fun <T : Enum<T>> readEnum(clazz: Class<T>): T {
        checkValue(6)
        val name = readStringI()
        return try {
            Arrays.stream(
                MethodHandles.lookup().findStatic(clazz, "values", MethodType.methodType(clazz.arrayType()))
                    .invoke() as Array<T>
            ).filter { o -> o.name == name }.findFirst().orElseThrow()
        } catch (e: Throwable) {
            throw IOException(e)
        }
    }

    @Synchronized
    @Throws(IOException::class)
    fun readEnum(): String {
        checkValue(6)
        return readStringI()
    }

    @Synchronized
    @Throws(IOException::class)
    fun readString(): String {
        checkValue(0)
        return readStringI()
    }

    @Synchronized
    @Throws(IOException::class)
    fun readStringI(): String {
        return String(this.istream.readNBytes(this.istream.readInt()))
    }

    @Synchronized
    @Throws(IOException::class)
    fun readDouble(): Double {
        checkValue(1)
        return this.istream.readDouble()
    }

    @Synchronized
    @Throws(IOException::class)
    fun readLong(): Long {
        checkValue(2)
        return this.istream.readLong()
    }

    @Synchronized
    @Throws(IOException::class)
    fun readInt(): Int {
        checkValue(3)
        return this.istream.readInt()
    }

    @Synchronized
    @Throws(IOException::class)
    fun readShort(): Short {
        checkValue(4)
        return this.istream.readShort()
    }

    @Synchronized
    @Throws(IOException::class)
    fun readByte(): Byte {
        checkValue(5)
        return this.istream.readByte()
    }

    @Synchronized
    @Throws(IOException::class)
    fun readBoolean(): Boolean {
        return readByte().toInt() == 1
    }

    @Synchronized
    @Throws(IOException::class)
    protected fun checkValue(needed: Int) {
        val i: Int = this.istream.read()
        if (i != needed)
            throw IOException("Invalid Value {$i}! Required {$needed}!")
    }

    override fun close() {
        this.istream.close()
        this.ostream.close()
    }
}