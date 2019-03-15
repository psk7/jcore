package pvt.psk.jcore.utils

import kotlinx.atomicfu.*
import java.io.*
import java.nio.*
import java.util.*

private val cnt = atomic((UUID.randomUUID().hashCode().toLong() shl 32))

class AckToken {

    constructor() {
        ID = cnt.incrementAndGet()
    }

    constructor(reader: BinaryReader) {
        ID = reader.readLong()
    }

    val ID: Long

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AckToken

        if (ID != other.ID)
            return false

        return true
    }

    override fun hashCode(): Int = ID.hashCode()

    fun toStream(stream: OutputStream) {
        stream.write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(ID).array())
    }

    override fun toString(): String = "Token #${ID and 0xFFFFFFFF} in ${(ID shr 32)}"
}