package pvt.psk.jcore.relay

import pvt.psk.jcore.utils.*
import java.util.*

/**
 * Идентификатор ретранслятора сообщений
 *
 * @param id Идентификатор ретранслятора сообщений
 */
class RelayID(val id: Int) : Comparable<RelayID> {

    companion object {
        fun new() = RelayID(UUID.randomUUID())

        val Unknown = RelayID(0)
    }

    constructor(id: UUID) : this(id.hashCode())

    constructor(reader: BinaryReader) : this(reader.readInt32())

    val isUnknown
        get() = id == 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RelayID

        if (id != other.id) return false

        return true
    }

    override fun hashCode() = id

    fun write(writer: BinaryWriter) = writer.write(id)

    override fun toString(): String = "<$id>"

    override fun compareTo(other: RelayID): Int = id.compareTo(other.id)
}