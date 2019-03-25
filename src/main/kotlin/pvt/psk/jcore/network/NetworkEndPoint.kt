@file:Suppress("unused")

package pvt.psk.jcore.network

import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import java.net.*

class NetworkEndPoint(dataChannel: IChannel,
                      private val sender: ISender,
                      targetHost: HostID,
                      private val directory: IPAddressDirectory,
                      private val port: Int,
                      acceptTags: Array<String>?,
                      canReceiveStream: Boolean = false)
    : EndPoint(dataChannel, sender, targetHost, acceptTags, canReceiveStream) {

    val target: InetSocketAddress?
        get() {
            val ad = directory.resolve(targetHost) ?: return null

            return InetSocketAddress(ad, port)
        }

    init {
        (sender as? NetworkSenderSocket)?.register(this)
    }

    override fun close() {
        super.close()

        (sender as? NetworkSenderSocket)?.unregister(this)
    }

    override fun toString(): String = "$targetHost$target Accept:${acceptTags?.joinToString(",") ?: ""}"
}