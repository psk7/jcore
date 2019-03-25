package pvt.psk.jcore.utils

import kotlinx.coroutines.io.*
import java.io.*
import java.nio.*
import java.util.*
import kotlin.experimental.*

class BinaryReader {

    var baseStream: InputStream? = null

    constructor(Data: ByteArray) {
        baseStream = ByteArrayInputStream(Data)
    }

    constructor(Stream: InputStream) {
        baseStream = Stream
    }

    constructor(@Suppress("UNUSED_PARAMETER") rc: ByteReadChannel) {
    }

    protected fun read7BitEncodedInt(): Int {
        var acc = 0

        do {
            val b = readByte()

            acc *= 128
            acc += b and 127
        } while (b > 127)

        return acc
    }

    fun readUUID(): UUID = readBytes(16).toUUID()

    fun readString(): String = String((readBytes(read7BitEncodedInt())), Charsets.UTF_8)

    fun readByte(): Byte = baseStream!!.read().toByte()

    fun readBytes(count: Int): ByteArray = ByteArray(count).also { baseStream!!.read(it) }

    fun readChar(): Char = readByte().toChar()

    fun readInt32(): Int = ByteBuffer.wrap(readBytes(Int.SIZE_BYTES)).order(ByteOrder.LITTLE_ENDIAN).int

    fun readInt16(): Short = ByteBuffer.wrap(readBytes(Short.SIZE_BYTES)).order(ByteOrder.LITTLE_ENDIAN).short

    fun readBoolean(): Boolean = readByte() != 0.toByte()

    fun readLong(): Long = ByteBuffer.wrap(readBytes(Long.SIZE_BYTES)).order(ByteOrder.LITTLE_ENDIAN).long

    fun readDouble(): Double = Double.fromBits(readLong())
}
