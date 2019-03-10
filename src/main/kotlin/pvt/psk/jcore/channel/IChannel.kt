package pvt.psk.jcore.channel

interface IChannel {
    fun getChannel(received: DataReceived? = null, description: String? = null): IChannelEndPoint
}