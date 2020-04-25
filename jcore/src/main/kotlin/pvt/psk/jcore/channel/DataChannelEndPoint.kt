package pvt.psk.jcore.channel

import kotlinx.coroutines.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.relay.*
import pvt.psk.jcore.utils.*
import java.io.*

private fun createBytes(dataReceived: DataChannelBytesReceived?): DataChannelReceived {
    fun f(envelope: Envelope, packet: DataPacket) {

        if (packet is BytesPacket) {
            dataReceived?.invoke(envelope, packet.data)
            return
        }

        if (packet !is StreamPacket)
            return

        val ms = ByteArrayOutputStream()
        packet.addTargetStream(ms, true)

        packet.completed.invokeOnCompletion {
            dataReceived?.invoke(envelope, ms.toByteArray())
        }
    }

    return ::f
}

private fun createStream(dataReceived: DataChannelStreamReceived?): DataChannelReceived {
    fun f(envelope: Envelope, packet: DataPacket) {

        if (packet is BytesPacket) {
            dataReceived?.invoke(envelope, ByteArrayInputStream(packet.data))
            return
        }

        if (packet !is StreamPacket)
            return

        val rs = RingStream()
        packet.addTargetStream(rs.getOutputStream())

        GlobalScope.launch(Dispatchers.IO) { dataReceived?.invoke(envelope, rs.getInputStream()) }
    }

    return ::f
}

class DataChannelEndPoint(private val dataReceived: DataChannelReceived?, acceptTag: PacketTag?) : IDataChannelEndPoint {

    private val acceptTag = acceptTag ?: PacketTag.Empty

    var endPoint: IChannelEndPoint? = null

    companion object {
        fun fromBytes(dataReceived: DataChannelBytesReceived?, acceptTag: PacketTag?) = DataChannelEndPoint(createBytes(dataReceived), acceptTag)
        fun fromStream(dataReceived: DataChannelStreamReceived?, acceptTag: PacketTag?) = DataChannelEndPoint(createStream(dataReceived), acceptTag)
    }

    fun received(channel: IChannelEndPoint, packet: Message) {
        if (packet !is ChannelEnvelope || packet.payload !is DataPacket)
            return

        val dp = packet.payload

        // Пакеты отправленные соседями по хосту в этом канале
        // Возвращаются только отправленные явно себе или всем
        if ((packet.targetIsBroadcast || packet.targetIsLocal) && (acceptTag.isEmpty || dp.tag == acceptTag))
            dataReceived?.invoke(Envelope(packet.from, dp.tag), dp)
    }

    override fun sendMessage(data: ByteArray, target: HostID?, metadata: Array<Any>?, tag: PacketTag?) {
        BaseChannel.sendMessage(endPoint ?: return, data, target, metadata, tag)
    }

    override fun sendMessage(data: ByteArray, targets: Array<HostID>, metadata: Array<Any>?, tag: PacketTag?) {
        BaseChannel.sendMessage(endPoint ?: return, data, targets, metadata, tag)
    }

    override fun sendStream(target: HostID?, metadata: Array<Any>?, tag: PacketTag?): Deferred<OutputStream> =
        BaseChannel.sendMessage(endPoint!!, target, metadata, tag)

    override fun sendStream(targets: Array<HostID>, metadata: Array<Any>?, tag: PacketTag?): Deferred<OutputStream> =
        BaseChannel.sendMessage(endPoint!!, targets, metadata, tag)

    override fun sendStream(source: InputStream, target: HostID?, metadata: Array<Any>?, tag: PacketTag?): Job =
        BaseChannel.sendMessage(source, endPoint!!, target, metadata, tag, GlobalScope)

    override fun sendStream(source: InputStream, targets: Array<HostID>, metadata: Array<Any>?, tag: PacketTag?): Job =
        BaseChannel.sendMessage(source, endPoint!!, targets, metadata, tag, GlobalScope)

    override fun close() {
        endPoint?.close()
    }
}