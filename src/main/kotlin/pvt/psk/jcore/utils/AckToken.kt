package pvt.psk.jcore.utils

import java.io.*
import java.nio.*
import java.util.*
import java.util.concurrent.atomic.*

val cnt: AtomicLong = AtomicLong((UUID.randomUUID().hashCode().toLong() shl 32))

class AckToken() {

    constructor(reader: BinaryReader) : this() {}

    val ID: Long = cnt.incrementAndGet()

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
}