package pvt.psk.jcore.network

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.io.core.*
import pvt.psk.jcore.utils.*
import java.net.*

class SafeUdpClient(BindEndPoint: InetSocketAddress,
                    val CancellationToken: CancellationToken,
                    val IsMulticast: Boolean = false,
                    private val received: suspend (ByteArray, InetSocketAddress) -> Unit) {

    val selector = ActorSelectorManager(Dispatchers.IO)
    private val _udp: BoundDatagramSocket

    val localEndPoint: InetSocketAddress
        get() = (_udp.localAddress as InetSocketAddress)

    init {

        fun UDPSocketBuilder.safeBind(): BoundDatagramSocket {
            while (true) {
                try {
                    return bind(BindEndPoint) { reuseAddress = IsMulticast }
                } catch (e: Exception) {
                    runBlocking { delay(100) }
                }
            }

        }

        _udp = aSocket(selector).udp().safeBind()

        beginReceive()
    }

    private fun beginReceive() = GlobalScope.launch(Dispatchers.IO) {
        while (!CancellationToken.isCancellationRequested) {
            val rp = _udp.incoming.receive()

            val ba = ByteArray(rp.packet.remaining.toInt())
            rp.packet.readAvailable(ba)

            launch { received(ba, rp.address as InetSocketAddress) }
        }
    }

    fun send(data: ByteArray, target: SocketAddress): Unit =
            _udp.outgoing.sendBlocking(Datagram(ByteReadPacket(data), target))
}