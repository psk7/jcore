package pvt.psk.jcore.network

import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import java.net.*

open class NetworkEndPoint(dataChannel: IChannel?, sender: ISender, targetHost: HostID, val controlBus: IChannel,
                           val readOnly: Boolean = false, private val sorter: IGetPreferredIPEndPoint? = null,
                           canReceiveStream: Boolean = false)
    : EndPoint(dataChannel, sender, targetHost, canReceiveStream) {

    var target: InetSocketAddress? = null
        private set

    private val _cbus = controlBus.getChannel(::controlReceive, "ControlBus of EndPoint $targetHost")

    private val _lst = mutableListOf<InetSocketAddress>()

    private fun controlReceive(ch: IChannelEndPoint, msg: Message) {
        when (msg) {
            is LeaveCommand -> if (msg.FromHost == targetHost) close()
            //is RescanEndPointsCommand -> probe(msg)
        }
    }

    fun updateIPAddresses(newTargetEndPoint: InetSocketAddress) {
        synchronized(_lst) {
            if (target == null) {
                // Первый адрес принимается безусловно
                target = newTargetEndPoint
                _lst.add(newTargetEndPoint)
                return
            }
        }

        synchronized(_lst) {
            // Следующие с проверкой

            if (!_lst.any { it == newTargetEndPoint })
                _lst.add(newTargetEndPoint)
        }

        selectPrimaryAddress()
    }

    private fun selectPrimaryAddress() {

        if (sorter == null) {
            synchronized(_lst) {
                target = _lst.firstOrNull()
            }
        } else {
            val eps =
                    synchronized(_lst) {
                        _lst.toTypedArray()
                    }

            target = sorter.get(eps);
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