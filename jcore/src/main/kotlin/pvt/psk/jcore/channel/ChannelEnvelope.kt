package pvt.psk.jcore.channel

import pvt.psk.jcore.host.*

class ChannelEnvelope : Message {

    val from: HostID
    val targets: Array<HostID>
    val payload: Message

    inline val targetIsLocal
        get() = targets.size == 1 && targets[0].isLocal

    inline val targetIsBroadcast
        get() = targets.size == 1 && targets[0].isBroadcast

    constructor(from: HostID, to: HostID, payload: Message) {
        this.from = from
        this.targets = arrayOf(to)
        this.payload = payload
    }

    constructor(from: HostID, targets: Array<HostID>, payload: Message) {
        this.from = from
        this.targets = if (targets.any { it.isBroadcast }) arrayOf(HostID.All) else targets
        this.payload = payload
    }
}