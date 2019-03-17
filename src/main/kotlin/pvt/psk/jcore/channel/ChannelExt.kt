package pvt.psk.jcore.channel

import pvt.psk.jcore.host.*
import pvt.psk.jcore.utils.*

typealias Predicate<P> = (P) -> Boolean

private class FilteredChannel<T>(val source: IChannel) : IChannel where T : Message {

    private class FilteredEndPoint<TT>(val Filters: Array<Predicate<TT>>,
                                       Source: IChannel,
                                       val Received: DataReceived?,
                                       @Suppress("unused") val Description: String?) : IChannelEndPoint where TT : Message {

        val source: IChannelEndPoint

        init {
            source = Source.getChannel({ c, m ->
                                           @Suppress("UNUSED_VARIABLE", "UNCHECKED_CAST") val mm = m as TT
                                           onReceived(c, m)
                                       })
        }

        fun onReceived(channel: IChannelEndPoint, packet: TT) {
            if (Received != null && Filters.all { it(packet) })
                Received.invoke(channel, packet)
        }

        override fun sendMessage(message: Message) = source.sendMessage(message)

        override fun close() = source.close()
    }

    var filters = arrayOf<Predicate<T>>()

    fun addFilter(filter: Predicate<T>) {
        filters = filters.plusElement(filter)
    }

    override fun getChannel(received: DataReceived?, description: String?): IChannelEndPoint {
        return FilteredEndPoint(filters, source, received, description)
    }

    override fun sendMessage(message: Message) = source.sendMessage(message)

    override fun close() {}
}

@Suppress("UNCHECKED_CAST")
fun <T> IChannel.filter(filter: Predicate<T>): IChannel where T : Message =
        (this as? FilteredChannel<T> ?: FilteredChannel(this)).apply {
            addFilter(filter)
        }

fun IChannel.filterLocal() = filter<DirectedMessage> { it.toHost == HostID.Local }

fun IChannel.acceptHost(accept: HostID) = filter<DirectedMessage> { it.toHost == HostID.All || it.toHost == accept }

@Suppress("unused")
fun IChannel.filterLocal(received: DataReceived?) = filterLocal().getChannel(received)

fun IChannelEndPoint.sendMessage(toHost: HostID, block: BinaryWriter.() -> Unit): IChannelEndPoint {
    val wr = BinaryWriter()

    wr.block()

    sendMessage(BytesPacket(wr.toArray(), HostID.Local, toHost))

    return this
}


