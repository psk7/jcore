package pvt.psk.jcore.relay

import pvt.psk.jcore.utils.*

class PacketTag {

    val id: Int

    constructor(id: Int) {
        this.id = id
    }

    constructor(id: String?) {
        this.id = if (id != null) fromString(id) else Empty.id
    }

    constructor(reader: BinaryReader) {
        val b1 = reader.readByte()
        val b2 = reader.readByte() shl 8
        val b3 = reader.readByte() shl 16
        val b4 = reader.readByte() shl 24

        this.id = b1 + b2 + b3 + b4
    }

    companion object {
        val Empty = PacketTag(0)
        val Random
            get() = PacketTag(kotlin.random.Random.nextInt())
    }

    private fun calc(v: String) =
        v[0].toByte().toInt() + (v[1].toByte().toInt() shl 8) + (v[2].toByte().toInt() shl 16) + (v[3].toByte().toInt() shl 24)

    private fun fromString(id: String): Int =
        when (id.length) {
            0    -> throw Exception()
            1    -> calc("$id   ")
            2    -> calc("$id  ")
            3    -> calc("$id ")
            4    -> calc(id)
            else -> id.hashCode()
        }

    val isEmpty
        get() = id == 0

    fun serialize(writer: BinaryWriter): Unit = writer.write(id)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PacketTag

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id

    override fun toString() = "Tag: <$id>"
}