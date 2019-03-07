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
                    val IsMulticast: Boolean = false) {

    class Delegate<TS, TA> : Event<TS, TA> {
        private var invocationList: MutableList<(TS, TA) -> Unit>? = null

        override operator fun plusAssign(m: (TS, TA) -> Unit) {
            val list = invocationList ?: mutableListOf<(TS, TA) -> Unit>().apply { invocationList = this }
            list.add(m)
        }

        override operator fun minusAssign(m: (TS, TA) -> Unit) {
            val list = invocationList
            if (list != null) {
                list.remove(m)
                if (list.isEmpty()) {
                    invocationList = null
                }
            }
        }

        operator fun invoke(source: TS, arg: TA) {
            val list = invocationList
            if (list != null) {
                for (m in list)
                    m(source, arg)
            }
        }
    }

    interface Event<out TS, out TA> {
        operator fun plusAssign(m: (TS, TA) -> Unit)
        operator fun minusAssign(m: (TS, TA) -> Unit)
    }

    val selector = ActorSelectorManager(Dispatchers.IO)
    private val _udp: BoundDatagramSocket

    val localEndPoint: InetSocketAddress
        get() = (_udp.localAddress as InetSocketAddress)

    val received = Delegate<ByteArray, InetSocketAddress>()

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

    private fun beginReceive() = GlobalScope.launch(Dispatchers.Unconfined) {
        while (!CancellationToken.isCancellationRequested) {
            val rp = _udp.incoming.receive()

            val ba = ByteArray(rp.packet.remaining.toInt())
            rp.packet.readAvailable(ba)

            launch(Dispatchers.Default) { onReceived(ba, rp.address as InetSocketAddress) }
        }
    }

    private fun onReceived(Data: ByteArray, From: InetSocketAddress) {
        received.invoke(Data, From)
    }

    fun send(data: ByteArray, target: SocketAddress) {
        _udp.outgoing.sendBlocking(Datagram(ByteReadPacket(data), target))
    }

    fun localEndPoint(): SocketAddress = _udp.localAddress
}