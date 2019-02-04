package pvt.psk.jcore.utils

import java.nio.*
import java.util.*

fun UUID.toArray(): ByteArray {
    val bb = ByteBuffer.allocate(16)
    bb.putLong(mostSignificantBits)
    bb.putLong(leastSignificantBits)

    return bb.array()
}

fun ByteArray.toUUID(): UUID {
    val bb = ByteBuffer.wrap(this)
    val high = bb.getLong()
    val low = bb.getLong()
    return UUID(high, low)
}