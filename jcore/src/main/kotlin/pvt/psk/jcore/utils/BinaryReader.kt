package pvt.psk.jcore.utils

import java.io.*
import java.nio.*
import java.util.*

class BinaryReader {

    val baseStream: InputStream

    constructor(Data: ByteArray) {
        baseStream = ByteArrayInputStream(Data)
    }

    constructor(Stream: InputStream) {
        baseStream = Stream
    }

    fun read7BitEncodedInt(): Int {
        var acc = 0
        var shft = 0

        do {
            val b = readByte()
            acc += (b and 127) shl shft
            shft += 7
        } while (b > 127)

        return acc
    }

    fun readUUID(): UUID = readBytes(16).toUUID()

    fun readString(): String = String((readBytes(read7BitEncodedInt())), Charsets.UTF_8)

    fun readByte(): Int = baseStream.read()

    fun readBytes(count: Int): ByteArray {
        val b = ByteArray(count)

        var offs = 0
        var remain = count

        do {
            val readed = baseStream.read(b, offs, remain)
            offs += readed
            remain -= readed

        } while (remain > 0 && readed != -1)

        return b
    }

    fun readChar(): Char = baseStream.read().toChar()

    fun readInt32(): Int = ByteBuffer.wrap(readBytes(Int.SIZE_BYTES)).order(ByteOrder.LITTLE_ENDIAN).int

    fun readInt16(): Short = ByteBuffer.wrap(readBytes(Short.SIZE_BYTES)).order(ByteOrder.LITTLE_ENDIAN).short

    fun readUInt16(): UShort = ByteBuffer.wrap(readBytes(Short.SIZE_BYTES)).order(ByteOrder.LITTLE_ENDIAN).short.toUShort()

    fun readBoolean(): Boolean = readByte() != 0

    fun readLong(): Long = ByteBuffer.wrap(readBytes(Long.SIZE_BYTES)).order(ByteOrder.LITTLE_ENDIAN).long

    //fun readDouble(): Double = Double.fromBits(readLong())
}
