package pvt.psk.jcore.network

import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import java.net.*

open class NetworkEndPoint(dataChannel: IChannel, sender: ISender, targetHost: HostID,
                           private val directory: IPAddressDirectory,
                           private val port: Int,
                           val controlBus: IChannel,
                           val readOnly: Boolean = false, canReceiveStream: Boolean = false)
    : EndPoint(dataChannel, sender, targetHost, canReceiveStream) {

    val target: InetSocketAddress
        get() = InetSocketAddress(directory.resolve(targetHost), port)

    private val _cbus = controlBus.getChannel(::controlReceive, "ControlBus of EndPoint $targetHost")

    private val _lst = mutableListOf<InetSocketAddress>()

    init {
        (sender as NetworkSenderSocket)?.register(this)
    }

    private fun controlReceive(ch: IChannelEndPoint, msg: Message) {
        when (msg) {
            is LeaveCommand -> if (msg.fromHost == targetHost) close()
            //is RescanEndPointsCommand -> probe(msg)
        }
    }

    override fun close() {
        super.close()
        _cbus.close()
    }

    var isReadOnly: Boolean
        get() = true
        set(value) = Unit

    override fun toString(): String = "${targetHost}${target}(${if (isReadOnly) "*" else ""})";
}