package pvt.psk.jcore.network

import kotlinx.atomicfu.*
import kotlinx.coroutines.*
import org.koin.core.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.io.*
import java.net.*

private val cnt = atomic(1)

open class SafeUdpClient(bindEndPoint: InetSocketAddress,
                         private val received: (ByteArray, InetSocketAddress) -> Unit) : Closeable, KoinComponent {

    private val logCat = "UDP"

    private val id = cnt.getAndIncrement()

    private val log: Logger by inject()
    private val debugHolder: DebugInfoHolder by inject()

    private var udp: DatagramSocket? = null
    private var lbep: InetSocketAddress = bindEndPoint

    private var rcvJob: Job

    @Suppress("unused")
    val localEndPoint: InetSocketAddress
        get() = (udp?.localSocketAddress as InetSocketAddress)

    init {
        bind()
        rcvJob = beginReceive()
    }

    protected open fun createUdp(bindEndPoint: InetSocketAddress) : DatagramSocket = DatagramSocket(bindEndPoint)

    private fun bind() {
        val u = udp
        if (u != null && !u.isClosed)
            u.close()

        udp = createUdp(lbep)
        lbep = udp?.localSocketAddress as InetSocketAddress
    }

    fun rebind() {
        synchronized(this) {
            log.writeLog(LogImportance.Warning, logCat, "SafeUdpSocket begin rebind")

            rcvJob.cancel()

            try {
                udp?.close()     // udp.receive бросит SocketException
            }
            catch (ignored: Exception) {
            }

            runBlocking { rcvJob.join() }

            bind()

            log.writeLog(LogImportance.Warning, logCat, "SafeUdpSocket complete rebind. lbep=$lbep")

            debugHolder.rebinds.incrementAndGet()

            rcvJob = beginReceive()
        }
    }

    /**
     * Начинает прием пакетов из сети
     */
    private fun beginReceive() = GlobalScope.launch(Dispatchers.IO) {
        val buf = ByteArray(16384)

        while (isActive) {
            val dp = DatagramPacket(buf, buf.size)

            try {
                udp?.receive(dp)
            }
            catch (e: SocketException) {
                if (isActive)
                    log.writeLog(LogImportance.Error, logCat, "SafeUdpClient::receive: ${e.message}")

                if (isActive) {
                    rebind()
                    continue
                } else {
                    debugHolder.failedReceives.incrementAndGet()
                    return@launch
                }
            }

            val l = dp.length

            if (l == 0 || l == buf.size)
                continue

            debugHolder.receives.incrementAndGet()

            val ba = ByteArray(l)
            dp.data.copyInto(ba, endIndex = l)

            received(ba, dp.socketAddress as InetSocketAddress)
        }
    }

    /**
     * Отправляет пакет в сеть
     *
     * @param data датаграмма
     * @param target сетевой адрес назначения
     */
    fun send(data: ByteArray, target: SocketAddress) {
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

        var attempt = 3

        while (attempt-- > 0) {
            try {
                udp?.send(DatagramPacket(data, data.size, target))
                debugHolder.sends.incrementAndGet()
                return
            }
            catch (e: Exception) {
                debugHolder.failedSends.incrementAndGet()
                debugHolder.lastFailedSendMessage = e.message ?: ""
                log.writeLog(LogImportance.Error, logCat, e.message.toString())
            }
        }
    }

    /**
     * Завершает получение пакетов данных из сети
     */
    override fun close() {
        rcvJob.cancel()

        try {
            udp?.close()     // udp.receive бросит SocketException
        }
        catch (ignored: Exception) {
        }

        runBlocking { rcvJob.join() }
    }

    override fun toString(): String = "UDP #$id: ${udp?.localSocketAddress}"
}