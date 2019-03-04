package pvt.psk.jcore.network

import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import java.net.*

class NetworkEndPoint(dataChannel: IChannel,
                      sender: ISender,
                      targetHost: HostID,
                      val controlBus: IChannel,
                      val readOnly: Boolean = false,
                      val sorter: IGetPreferredIPEndPoint? = null,
                      canReceiveStream : Boolean = false) : EndPoint(dataChannel, sender, targetHost, canReceiveStream) {

    lateinit var target: InetSocketAddress
        private set

    private val _cbus = controlBus.getChannel(::controlReceive, "ControlBus of EndPoint $targetHost")

    private fun controlReceive(ch: IChannelEndPoint, msg: Message) {
        when (msg) {
            is LeaveCommand -> if (msg.FromHost == targetHost) close()
            //is RescanEndPointsCommand -> probe(msg)
        }
    }

    fun updateIPAddresses(NewTargetEndPoint: InetSocketAddress) {

    }

    override fun close() {
        super.close()
        _cbus.close()
    }

    var isReadOnly: Boolean
        get() = true
        set(value) = Unit

    override fun toString(): String = "(${targetHost}${target}{(${if (isReadOnly) "*" else ""})";
}