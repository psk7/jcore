package pvt.psk.jcore.channel

import pvt.psk.jcore.host.*

abstract class DataPacket(FromHost: HostID, ToHost: HostID, Metadata: Array<Any>? = null) : Message(FromHost, ToHost)
{
    val Metadata: Array<Any>? =
        when
        {
            Metadata != null && Metadata.count() > 0 -> Metadata
            else -> null
        }
}