package pvt.psk.jcore.channel

interface IChannel {
    fun getChannel(Received: DataReceived? = null, Description: String? = null): IChannelEndPoint
}