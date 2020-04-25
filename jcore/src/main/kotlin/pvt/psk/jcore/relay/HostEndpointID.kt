package pvt.psk.jcore.relay

import pvt.psk.jcore.host.*
import pvt.psk.jcore.utils.*

/**
 * Идентификатор цели ретранслятора
 */
class HostEndpointID {

    /**
     * Идентификатор хоста
     */
    val hostID: HostID

    /**
     * Идентификатор конечной точки хоста
     */
    val endpointID: UShort

    constructor(hostID: HostID, endpointID: UShort) {
        this.hostID = hostID
        this.endpointID = endpointID
    }

    constructor(reader: BinaryReader) {
        hostID = HostID(reader)
        endpointID = reader.readUInt16()
    }

    fun write(writer: BinaryWriter) {
        writer.write(hostID)
        writer.write(endpointID)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HostEndpointID

        return hostID == other.hostID && endpointID == other.endpointID
    }

    override fun hashCode(): Int {
        return (hostID.hashCode() * 397) xor endpointID.toInt()
    }

    override fun toString(): String = "[$hostID:$endpointID]"
}