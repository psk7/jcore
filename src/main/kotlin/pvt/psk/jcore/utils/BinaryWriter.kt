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

    fun write(v: Int) = write(ByteBuffer.allocate(4).putInt(v).array())

    fun write(v: ByteArray) = baseStream.write(v)

    fun write(v: UUID) {
        write(v.mostSignificantBits)
        write(v.leastSignificantBits)
    }

    private fun write(v: Long) = write(ByteBuffer.allocate(8).putLong(v).array())

    fun toArray(): ByteArray = (baseStream as ByteArrayOutputStream).toByteArray()
}