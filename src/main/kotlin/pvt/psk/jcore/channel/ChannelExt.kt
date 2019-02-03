package pvt.psk.jcore.channel

import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.utils.*

fun IChannel.sendHostInfo(command: HostInfoCommand) {
    sendMessage(command)
    command.release()
}

class DirectionFilterChannel(private val Channel: IChannel, Target: HostID) : IChannel {

    init {
        Channel.received += { (c, d) ->
            if (d.ToHost == Target)
                received(DataReceived(Channel, d))
        }
    }

    override fun sendMessage(Data: Message) = Channel.sendMessage(Data)

    private val r = Event<DataReceived>()

    override val received: Event<DataReceived> = r
}

fun IChannel.filterLocal(): IChannel = DirectionFilterChannel(this, HostID.Local)