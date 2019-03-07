package pvt.psk.jcore.channel

import pvt.psk.jcore.host.*

abstract class EndPoint(val dataChannel: IChannel?, protected val sender: ISender, val targetHost: HostID, val canReceiveStream: Boolean = false) {

    protected val Data = dataChannel?.getChannel(::send)

    private var isClosed = false

    fun send(channel: IChannelEndPoint, message: Message) {
        if (isClosed)
            return

        if (message is DataPacket)
            sender.send(message, this)
    }

    open fun onReceived(message: Message) = Data?.sendMessage(message)

    open fun close() {
        isClosed = true

        Data?.close()
    }
}