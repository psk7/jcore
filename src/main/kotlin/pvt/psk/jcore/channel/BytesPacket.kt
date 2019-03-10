package pvt.psk.jcore.channel

import pvt.psk.jcore.host.HostID

/**
 * Сообщение, содержащее байтовый массив
 */
class BytesPacket(Data: ByteArray, FromHost: HostID, ToHost: HostID, Metadata: Array<Any>? = null) : DataPacket(FromHost, ToHost, Metadata)
{
    val data: ByteArray = Data
}