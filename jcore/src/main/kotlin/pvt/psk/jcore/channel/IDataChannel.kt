package pvt.psk.jcore.channel

import pvt.psk.jcore.relay.*

interface IDataChannel : IDataChannelEndPoint {

    /**
     * Возвращает конечную точку канала
     */
    fun getChannel(received: DataChannelReceived? = null, acceptTag: PacketTag? = null): IDataChannelEndPoint

    fun getChannelBytes(received: DataChannelBytesReceived? = null, acceptTag: PacketTag? = null): IDataChannelEndPoint

    fun getChannelStream(received: DataChannelStreamReceived? = null, acceptTag: PacketTag? = null): IDataChannelEndPoint

    fun onNewHost(onNewHostInDataChannel: NewHostInDataChannel)
}