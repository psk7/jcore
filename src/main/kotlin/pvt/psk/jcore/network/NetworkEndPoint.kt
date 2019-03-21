@file:Suppress("unused")

package pvt.psk.jcore.network

import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import java.net.*

class NetworkEndPoint(dataChannel: IChannel, sender: ISender, targetHost: HostID,
                      private val directory: IPAddressDirectory,
                      private val port: Int,
                      var dontSendTo: Boolean = false,
                      canReceiveStream: Boolean = false)
    : EndPoint(dataChannel, sender, targetHost, canReceiveStream) {

    val target: InetSocketAddress?
        get() {
            val ad = directory.resolve(targetHost) ?: return null

            return InetSocketAddress(ad, port)
        }

    init {
        (sender as? NetworkSenderSocket)?.register(this)
    }

    override fun toString(): String = "$targetHost$target(${if (dontSendTo) "*" else ""})"
}