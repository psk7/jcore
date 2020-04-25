package pvt.psk.jcore.relay

import pvt.psk.jcore.channel.*

class RelayEnvelope(val from: HostEndpointID, val targets: HostEndpointIDSet, val payload: Message) {

    constructor(from: HostEndpointID, targets: Array<HostEndpointID>, payload: Message) : this(from, HostEndpointIDSet(targets), payload)

    constructor(from: HostEndpointID, targets: Iterable<HostEndpointID>, payload: Message)
            : this(from, HostEndpointIDSet(targets.toList()), payload)
}