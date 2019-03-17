package pvt.psk.jcore.network.commands

import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.utils.*
import java.net.*

class NetworkPingReplyCommand(fromHost: HostID, toHost: HostID, token: AckToken, val from: InetSocketAddress?)
    : PingReplyCommand(fromHost, toHost, token) {
}