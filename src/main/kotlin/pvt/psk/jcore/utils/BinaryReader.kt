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

    constructor(rc: ByteReadChannel){

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

    fun readInt(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun readByte(): Byte = ByteBuffer.wrap(baseStream!!.readNBytes(1)).get()

    fun readBytes(count: Int): ByteArray = ByteArray(count) { readByte() }

    fun readChar(): Char = ByteBuffer.wrap(baseStream!!.readNBytes(1)).get().toChar()

    fun readInt32(): Int = ByteBuffer.wrap(readBytes(Int.SIZE_BYTES)).order(ByteOrder.LITTLE_ENDIAN).int

    fun readInt16(): Short {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun ReadInt32(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun readBoolean(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
