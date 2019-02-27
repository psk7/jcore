package pvt.psk.jcore.channel

import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.host.*

fun IChannelEndPoint.sendHostInfo(command: HostInfoCommand) {
    sendMessage(command)
    command.release()
}

typealias Predicate<T> = (T) -> Boolean

private class FilteredChannel(val Source: IChannel) : IChannel {

    private class FilteredEndPoint(val Filters: Array<Predicate<Message>>,
                                   Source: IChannel,
                                   val Received: DataReceived?,
                                   val Description: String?) : IChannelEndPoint {

        val source: IChannelEndPoint

        init {
            source = Source.getChannel(::onReceived)
        }

        fun onReceived(channel: IChannelEndPoint, packet: Message) {
            if (Received != null && Filters.all { it(packet) })
                Received.invoke(channel, packet)
        }

        override fun sendMessage(Data: Message) = source.sendMessage(Data)

        override fun close() = source.close()
    }

    var filters = arrayOf<Predicate<Message>>()

    fun addFilter(filter: Predicate<Message>) {
        filters = filters.plusElement(filter)
    }

    override fun getChannel(Received: DataReceived?, Description: String?): IChannelEndPoint {
        return FilteredEndPoint(filters, Source, Received, Description)
    }
}

fun IChannel.filter(filter: Predicate<Message>): IChannel {
    val fch = if (this is FilteredChannel) this else FilteredChannel(this)

    fch.addFilter(filter)

    return fch
}

fun IChannel.filterLocal() = filter { it.ToHost == HostID.Local }

fun IChannel.acceptHost(accept: HostID) = filter { it.ToHost == HostID.All || it.ToHost == accept }

fun IChannel.filterLocal(received: DataReceived?) = filterLocal().getChannel(received)


