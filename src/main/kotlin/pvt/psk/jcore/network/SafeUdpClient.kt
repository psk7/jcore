package pvt.psk.jcore.network

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.io.core.*
import pvt.psk.jcore.utils.*
import java.net.*
import kotlin.coroutines.*

class SafeUdpClient(BindEndPoint: InetSocketAddress,
                    cancellationToken: CancellationToken,
                    private val isMulticast: Boolean = false,
                    private val noBind: Boolean = false,
                    private val received: suspend (ByteArray, InetSocketAddress) -> Unit) : CoroutineScope {

    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val selector: SelectorManager = ActorSelectorManager(Dispatchers.IO + job)
    private var _udp: BoundDatagramSocket? = null
    private var lbep: InetSocketAddress = BindEndPoint

    val localEndPoint: InetSocketAddress
        get() = (_udp?.localAddress as InetSocketAddress)

    init {
        if (!noBind)
            bind()

        cancellationToken.getSafeToken().register {
            // Порядок важен!
            // Сначала завершается ожидание приема в beginReceive, затем закрывается udp
            // иначе будет исключение в _udp.receive()
            coroutineContext.cancelChildren()
            runBlocking { job.cancelAndJoin() }
            _udp?.close()
        }
    }

    fun bind() {
        fun UDPSocketBuilder.safeBind(): BoundDatagramSocket? {
            // 10 попыток создать сокет
            for (i in 1..10) {
                try {
                    val r = bind(lbep) { reuseAddress = isMulticast }
                    lbep = r.localAddress as InetSocketAddress
                    return r
                } catch (e: Exception) {
                    runBlocking { delay(if (i < 5) 10 else 100) }
                }
            }

            return null
        }

        val u = _udp
        if (u != null && !u.isClosed)
            u.close()

        _udp = aSocket(selector).udp().safeBind()

        beginReceive()
    }

    private fun beginReceive() = launch {
        while (isActive) {
            try {
                val rp = _udp?.receive()

                val ba = ByteArray(rp!!.packet.remaining.toInt())
                rp.packet.readAvailable(ba)

                launch { received(ba, rp.address as InetSocketAddress) }
            } catch (e: Exception) {
                break
            }
        }
    }

    fun send(data: ByteArray, target: SocketAddress): Unit =
            try {
                _udp!!.outgoing.sendBlocking(Datagram(ByteReadPacket(data), target))
            } catch (e: Exception) {
            }
}