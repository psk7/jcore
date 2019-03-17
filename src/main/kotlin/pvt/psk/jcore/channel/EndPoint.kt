package pvt.psk.jcore.channel

import pvt.psk.jcore.host.*

abstract class EndPoint(@Suppress("CanBeParameter") private val dataChannel: IChannel?,
                        @Suppress("MemberVisibilityCanBePrivate") protected val sender: ISender, val targetHost: HostID,
                        @Suppress("unused") val canReceiveStream: Boolean = false) {

    protected val Data = dataChannel?.getChannel(::send)

    private var isClosed = false

    @Suppress("MemberVisibilityCanBePrivate")
    fun send(@Suppress("UNUSED_PARAMETER") channel: IChannelEndPoint, message: Message) {
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