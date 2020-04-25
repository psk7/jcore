package pvt.psk.jcore.relay

import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.utils.*

class RelayInfo : Message {

    val relayID: RelayID
    val adjacentHosts: Array<HostID>
    val force: Boolean

    constructor(relayID: RelayID, adjacentHosts: Array<HostID>, force: Boolean) : super() {
        this.relayID = relayID
        this.adjacentHosts = adjacentHosts
        this.force = force
    }

    constructor(reader: BinaryReader) {
        relayID = RelayID(reader)
        force = reader.readBoolean()

        adjacentHosts = Array(reader.readUInt16().toInt()) { HostID(reader) }
    }

    fun serialize(writer: BinaryWriter) {

        writer.write(relayID)
        writer.write(force)
        writer.write(adjacentHosts.size.toUShort())

        adjacentHosts.forEach(writer::write)
    }
}