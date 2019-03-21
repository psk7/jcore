package pvt.psk.jcore.network

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.io.core.*
import pvt.psk.jcore.utils.*
import java.net.*
import kotlin.coroutines.*

class SafeUdpClient(BindEndPoint: InetSocketAddress,
                    cancellationToken: CancellationToken,
                    private val isMulticast: Boolean = false,
                    private val sendMultiplicator: Int = 1,
                    private val received: (ByteArray, InetSocketAddress) -> Unit) : CoroutineScope {

    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val selector: SelectorManager = ActorSelectorManager(Dispatchers.IO + job)
    private var _udp: BoundDatagramSocket? = null
    private var lbep: InetSocketAddress = BindEndPoint

    val localEndPoint: InetSocketAddress
        get() = (_udp?.localAddress as InetSocketAddress)

    init {
        bind()

        cancellationToken.getSafeToken().register(::close)
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
            } catch (e: ClosedReceiveChannelException) {
                break
            }
        }
    }

    /**
     * Отправляет пакет в сеть
     *
     * @param data датаграмма
     * @param target сетевой адрес назначения
     */
    fun send(data: ByteArray, target: SocketAddress) {
        for (i in 1..sendMultiplicator)
            sendSafe(data, target)
    }

    /**
     * Отправляет пакет в сеть.
     *
     * Исключения игнорируются.
     *
     * @param data датаграмма
     * @param target сетевой адрес назначения
     */
    private fun sendSafe(data: ByteArray, target: SocketAddress) {
        try {
            _udp!!.outgoing.sendBlocking(Datagram(ByteReadPacket(data), target))
        } catch (e: Exception) {
        }
    }

    fun close() {
        // Порядок важен!
        // Сначала завершается ожидание приема в beginReceive, затем закрывается udp
        // иначе будет исключение в _udp.receive()
        coroutineContext.cancelChildren()
        runBlocking { job.cancelAndJoin() }
        socketExceptionSafe { _udp?.close() }
    }

    override fun toString(): String = "UDP: ${_udp?.localAddress}"
}