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

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
class SafeUdpClient(BindEndPoint: InetSocketAddress,
                    cancellationToken: CancellationToken,
                    isMulticast: Boolean = false,
                    private val received: suspend (ByteArray, InetSocketAddress) -> Unit) : CoroutineScope {

    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val selector = ActorSelectorManager(Dispatchers.IO + job)
    private val _udp: BoundDatagramSocket

    val localEndPoint: InetSocketAddress
        get() = (_udp.localAddress as InetSocketAddress)

    init {

        fun UDPSocketBuilder.safeBind(): BoundDatagramSocket {
            while (true) {
                try {
                    return bind(BindEndPoint) { reuseAddress = isMulticast }
                } catch (e: Exception) {
                    runBlocking { delay(100) }
                }
            }

        }

        _udp = aSocket(selector).udp().safeBind()

        cancellationToken.getSafeToken().register {
            // Порядок важен!
            // Сначала завершается ожидание приема в beginReceive, затем закрывается udp
            // иначе будет исключение в _udp.receive()
            coroutineContext.cancelChildren()
            runBlocking { job.cancelAndJoin() }
            _udp.close()
        }

        beginReceive()
    }

    private fun beginReceive() = launch {
        while (isActive) {
            val rp = _udp.receive()

            val ba = ByteArray(rp.packet.remaining.toInt())
            rp.packet.readAvailable(ba)

            launch { received(ba, rp.address as InetSocketAddress) }
        }
    }

    fun send(data: ByteArray, target: SocketAddress): Unit =
            try {
                _udp.outgoing.sendBlocking(Datagram(ByteReadPacket(data), target))
            } catch (e: Exception) {
            }
}