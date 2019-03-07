package pvt.psk.jcore.utils

import java.io.*
import java.nio.*
import java.util.*

class BinaryWriter {

    constructor(stream: OutputStream) {
        baseStream = stream
    }

    constructor() {
        baseStream = ByteArrayOutputStream()
    }

    val baseStream: OutputStream

    protected fun write7BitEncodedInt(value: Int) {

        var v = value
        do {
            baseStream.write(byteArrayOf((v and 255).toByte()))
            v = v ushr 7
        } while (v > 0)
    }

    fun write(byte: Byte) = baseStream.write(byteArrayOf(byte))

    fun write(string: String) {
        write7BitEncodedInt(string.length)

        string.forEach { write(it.toByte()) }
    }

    fun write(v: Int) = write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array())

    fun write(v: ByteArray) = baseStream.write(v)

    fun write(v: UUID) = write(v.toArray())

    private fun write(v: Long) = write(ByteBuffer.allocate(8).putLong(v).array())

    fun write(v: Short) = write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(v).array())

    fun write(v: Boolean) = write(if (v) 1.toByte() else 0)

    fun write(tk: AckToken) = tk.toStream(baseStream)

    fun toArray(): ByteArray = (baseStream as ByteArrayOutputStream).toByteArray()

    fun write(v: Double) = write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(v).array())
}