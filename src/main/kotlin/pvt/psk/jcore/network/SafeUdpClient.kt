package pvt.psk.jcore.network

import kotlinx.coroutines.*
import pvt.psk.jcore.utils.*
import java.net.*
import java.nio.*
import java.nio.channels.*

class SafeUdpClient(val Selector: UdpSelector,
                    val BindEndPoint: InetSocketAddress,
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

    private var _udp: DatagramChannel? = null
    private val _bb = ByteBuffer.allocateDirect(16384)

    val received = Delegate<ByteArray, InetSocketAddress>()

    init {

        try {
            _udp = DatagramChannel.open().apply {
                bind(BindEndPoint)
                configureBlocking(false)
                register(Selector.selector, SelectionKey.OP_READ, this@SafeUdpClient)

                if (IsMulticast)
                    setOption(StandardSocketOptions.SO_REUSEADDR, true)
            }
        }
        catch (be: Exception) {
        }
    }

    private fun onReceived(Data: ByteArray, From: InetSocketAddress) {
        received.invoke(Data, From)
    }

    fun processOnReceived() {
        val from = _udp?.receive(_bb) as InetSocketAddress ?: return

        val ba = ByteArray(_bb.position())

        _bb.run {
            rewind()
            get(ba)
            clear()
        }

        GlobalScope.launch { onReceived(ba, from) }
    }

    fun send(data: ByteArray, target: SocketAddress) {
        _udp?.send(ByteBuffer.wrap(data), target)
    }

    fun localEndPoint(): SocketAddress = _udp?.localAddress ?: throw Exception()
}