package pvt.psk.jcore.administrator.peerCommands

import java.net.*
import java.util.concurrent.*
import kotlin.collections.*

class CommandEndPoints {
    private val _eps = ConcurrentHashMap<InetSocketAddress, Unit>()

    fun add(IPEndPoint: InetSocketAddress) = _eps.set(IPEndPoint, Unit)

    fun getPrimary(): InetSocketAddress? = _eps.keys().toList().firstOrNull()
}