@file:Suppress("unused", "UNUSED_PARAMETER", "MemberVisibilityCanBePrivate")

package pvt.psk.jcore.channel

import pvt.psk.jcore.host.*

abstract class EndPoint(dataChannel: IChannel?,
                        private val sender: ISender, val targetHost: HostID,
                        acceptTags: Array<String>?,
                        val canReceiveStream: Boolean = false) {

    var acceptTags: Array<String>? = null
        set(value) {
            field = value

            if (field?.size == 0 || field?.all { it.isBlank() } == true)
                field = null
        }

    init {
        this.acceptTags = acceptTags
    }

    protected val data = dataChannel?.getChannel(::send)

    private var isClosed = false

    fun send(channel: IChannelEndPoint, message: Message) {
        if (isClosed)
            return

        val at = acceptTags

        if (message is DataPacket && (at == null || message.tags?.any { at.contains(it) } != false))
            sender.send(message, this)
    }

    open fun onReceived(message: Message) = data?.sendMessage(message)

    open fun close() {
        isClosed = true

        data?.close()
    }
}