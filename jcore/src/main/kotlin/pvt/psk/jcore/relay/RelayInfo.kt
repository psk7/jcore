package pvt.psk.jcore.relay

import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.utils.*

class RelayInfo : Message {

    data class Entry(val relay: RelayID, val revision: Int,
                     val adjacentRelays: Array<RelayID>, val adjacentHosts: Array<HostID>) {
        constructor(from: RoutingTable.Entry) : this(from.relay, from.revision, from.adjacentRelays, from.adjacentHosts)
    }

    val relay: RelayID
    val ack: AckToken
    val list: Array<Entry>

    constructor(relay: RelayID, adjacentHosts: Array<HostID>, adjacentRelays: Array<RelayID>,
                token: AckToken, revision: Int) : super() {
        this.relay = relay
        list = arrayOf(Entry(relay, revision, adjacentRelays, adjacentHosts))
        ack = token
    }

    constructor(relay: RelayID, table: Collection<RoutingTable.Entry>, token: AckToken) {
        this.relay = relay

        list = table.map { Entry(it) }.toTypedArray()
        ack = token
    }

    constructor(reader: BinaryReader) {
        relay = RelayID(reader)
        ack = AckToken(reader)

        list = Array(reader.readUInt16().toInt()) {
            val rel = RelayID(reader)
            val rev = reader.readInt32()
            val hids = Array(reader.readUInt16().toInt()){HostID(reader)}
            val rids = Array(reader.readUInt16().toInt()){RelayID(reader)}
            Entry(rel, rev, rids, hids)
        }
    }

    fun serialize(writer: BinaryWriter) {
        writer.write(relay)
        writer.write(ack)

        writer.write(list.size.toUShort())

        list.forEach {
            writer.write(it.relay)
            writer.write(it.revision)
            writer.write(it.adjacentHosts.size.toUShort())
            it.adjacentHosts.forEach(writer::write)
            writer.write(it.adjacentRelays.size.toUShort())
            it.adjacentRelays.forEach(writer::write)
        }
    }
}