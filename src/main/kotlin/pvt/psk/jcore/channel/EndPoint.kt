@file:Suppress("unused", "UNUSED_PARAMETER", "MemberVisibilityCanBePrivate")

package pvt.psk.jcore.channel

import pvt.psk.jcore.host.*

abstract class EndPoint(dataChannel: IChannel?,
                        private val sender: ISender, val targetHost: HostID,
                        val canReceiveStream: Boolean = false) {

    protected val data = dataChannel?.getChannel(::send)

    private var isClosed = false

    fun send(channel: IChannelEndPoint, message: Message) {
        if (isClosed)
            return

        if (message is DataPacket)
            sender.send(message, this)
    }

    open fun onReceived(message: Message) = data?.sendMessage(message)

    open fun close() {
        isClosed = true

        data?.close()
    }
}