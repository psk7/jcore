package pvt.psk.jcore.network

import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import java.net.*

class NetworkEndPoint(dataChannel: IChannel,
                      sender: NetworkSenderSocket,
                      targetHost: HostID,
                      val controlBus: IChannel,
                      val readOnly: Boolean = false,
                      val sorter: IGetPreferredIPEndPoint) : EndPoint(dataChannel, sender, targetHost) {

    lateinit var target: InetSocketAddress
        private set
}