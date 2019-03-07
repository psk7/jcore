package pvt.psk.jcore.utils

import kotlinx.coroutines.io.*
import java.io.*
import kotlin.experimental.*
import java.util.UUID
import java.nio.*

class BinaryReader {

    var baseStream: InputStream? = null

    constructor(Data: ByteArray) {
        baseStream = ByteArrayInputStream(Data)
    }

    constructor(Stream: InputStream) {
        baseStream = Stream
    }

    constructor(rc: ByteReadChannel) {

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

    fun ReadString(): String = String(CharArray(read7BitEncodedInt()) { readChar() })

    fun readByte(): Byte = baseStream!!.read().toByte()

    fun readBytes(count: Int): ByteArray = ByteArray(count) { readByte() }

    fun readChar(): Char = readByte().toChar()

    fun readInt32(): Int = ByteBuffer.wrap(readBytes(Int.SIZE_BYTES)).order(ByteOrder.LITTLE_ENDIAN).int

    fun readInt16(): Short = ByteBuffer.wrap(readBytes(Short.SIZE_BYTES)).order(ByteOrder.LITTLE_ENDIAN).short

    fun readBoolean(): Boolean = readByte() != 0.toByte()

    fun <T> readEnum(): T {
        val b = readByte().toInt()

        return b as T
    }

    fun readLong(): Long = ByteBuffer.wrap(readBytes(Long.SIZE_BYTES)).order(ByteOrder.LITTLE_ENDIAN).long
}
