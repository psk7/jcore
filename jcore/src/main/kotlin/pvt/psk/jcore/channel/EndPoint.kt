@file:Suppress("unused", "UNUSED_PARAMETER", "MemberVisibilityCanBePrivate")

package pvt.psk.jcore.channel

import pvt.psk.jcore.relay.*

/**
 * Удаленная конечная точка канала
 */
class EndPoint(val target: HostEndpointID, acceptTags: Array<PacketTag>?) {

    var acceptTags: Array<PacketTag>? = null
        set(value) {
            field = value

            if (field?.size == 0 || field?.all { it.isEmpty } == true)
                field = null
        }

    init {
        this.acceptTags = acceptTags
    }

    val isAvailable = true
}