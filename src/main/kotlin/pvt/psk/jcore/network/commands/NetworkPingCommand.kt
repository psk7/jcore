package pvt.psk.jcore.network.commands

import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.utils.*
import java.net.*

class NetworkPingCommand(fromHost: HostID, toHost: HostID, token: AckToken, val from: InetSocketAddress?) : PingCommand(fromHost, toHost, token) {


    override fun getReply(): PingReplyCommand = NetworkPingReplyCommand(HostID.Local, fromHost, token, from)
}