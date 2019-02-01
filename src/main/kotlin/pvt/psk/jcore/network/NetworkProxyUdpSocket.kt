package pvt.psk.jcore.network

import pvt.psk.jcore.utils.*
import java.net.*

class NetworkProxyUdpSocket(val received: (ByteArray, InetSocketAddress) -> Unit) {
    fun send(datagram: ByteArray, send: InetSocketAddress) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    data class Received(val Data: ByteArray, val From: InetSocketAddress)
}