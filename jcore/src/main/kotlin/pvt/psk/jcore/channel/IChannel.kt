package pvt.psk.jcore.channel

interface IChannel : IChannelEndPoint {

    fun getChannel(received: DataReceived? = null, description: String? = null): IChannelEndPoint
}