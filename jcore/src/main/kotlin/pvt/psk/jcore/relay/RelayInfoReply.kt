package pvt.psk.jcore.relay

import pvt.psk.jcore.channel.*
import pvt.psk.jcore.utils.*

class RelayInfoReply : Message, RevisionEnsurator.IReply {

    val relay: RelayID
    override val ack: AckToken
    override val accepted: Boolean

    constructor(relay: RelayID, ack: AckToken, accepted: Boolean) {
        this.relay = relay
        this.ack = ack
        this.accepted = accepted
    }

    constructor(reader: BinaryReader) {
        relay = RelayID(reader)
        ack = AckToken(reader)
        accepted = reader.readBoolean()
    }

    fun serialize(writer: BinaryWriter) {
        writer.write(relay)
        writer.write(ack)
        writer.write(accepted)
    }
}