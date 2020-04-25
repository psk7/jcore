package pvt.psk.jcore.utils

import pvt.psk.jcore.host.*
import pvt.psk.jcore.relay.*
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

    fun write7BitEncodedInt(value: Int) {
        var v = value
        do {
            baseStream.write(if (v < 128) v else v or 0x80)
            v = v ushr 7
        } while (v > 0)
    }

    fun write(byte: Byte) = baseStream.write(byte.toInt())

    fun write(string: String) {
        val bytes = string.toByteArray(Charsets.UTF_8)

        write7BitEncodedInt(bytes.size)
        write(bytes)
    }

    fun write(v: Int) = write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array())

    fun write(v: ByteArray) = baseStream.write(v)

    fun write(v: UUID) = write(v.toArray())

    private fun write(v: Long) = write(ByteBuffer.allocate(Long.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putLong(v).array())

    fun write(v: Short) = write(ByteBuffer.allocate(Short.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putShort(v).array())
    fun write(v: UShort) = write(ByteBuffer.allocate(UShort.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putShort(v.toShort()).array())

    fun write(v: Boolean) = write(if (v) 1.toByte() else 0)

    fun write(tk: AckToken) = tk.toStream(baseStream)

    fun toArray(): ByteArray = (baseStream as ByteArrayOutputStream).toByteArray()

    fun write(v: Double) = write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(v).array())

    fun write(v: HostID) = v.serialize(this)

    fun write(v: RelayID) = v.write(this)

    fun write(v: HostEndpointID) = v.write(this)

    fun write(v: PacketTag) = v.serialize(this)

    fun write(v: ISerializable) = v.serialize(this)

    fun write(v: HostEndpointIDSet) = v.serialize(this)
}