package pvt.psk.jcore.network

import io.ktor.util.*
import kotlinx.coroutines.*
import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.network.commands.*
import pvt.psk.jcore.utils.*
import java.net.*
import java.util.concurrent.*
import kotlin.coroutines.*

@KtorExperimentalAPI
@ExperimentalCoroutinesApi
class NetworkCommandSocket(Bus: IChannel,
                           val admPort: Int,
                           Log: Logger?,
                           private val directory: IPAddressDirectory,
                           private val CancellationToken: CancellationToken) :
        PeerCommandSocket(Bus, Log, CancellationToken), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Unconfined

    private val unicastudp: SafeUdpClient
    private val admpoints = ConcurrentHashMap<HostID, Deferred<InetSocketAddress?>>()

    /**
     * Multicast сокет. Используется **только** для приема широковещательной рассылки
     * Отправка **всегда** через unicast
     */
    private val _mcsocket: MulticastSocket

    private var multicasts = arrayOf<InetSocketAddress>()

    init {
        unicastudp = SafeUdpClient(InetSocketAddress("::", 0), CancellationToken, false, ::received)

        _mcsocket = MulticastSocket(admPort)

        beginReceive()

        scan()
    }

    fun scan() {
        val mca = InetAddress.getByName("FF02::1")

        val ifcs = NetworkInterface.getNetworkInterfaces().toList().filter {
            !it.isLoopback && it.isUp && it.supportsMulticast() && !it.isPointToPoint &&
                    !it.name.contains("radio", true)
        }.toTypedArray()

        ifcs.forEach {
            Log?.writeLog(LogImportance.Info, logCat, "Найден подходящий сетевой интерфейс $it %${it.index}")
        }

        val mcids = ifcs.map { it.index }.toIntArray()
        mcids.forEach { z ->
            try {
                _mcsocket.joinGroup(InetSocketAddress(mca, admPort), ifcs.find { it.index == z })
            } catch (e: SocketException) {
            }
        }

        multicasts = mcids.map { InetSocketAddress(InetAddress.getByName("FF02::1%$it"), admPort) }.toTypedArray()

        val hosts = admpoints.keys.toTypedArray()

        admpoints.clear()
        directory.reset()

        GlobalScope.launch(Dispatchers.Unconfined) {
            hosts.forEach { resolve(it) }
        }
    }

    override fun onBusCmd(channel: IChannelEndPoint, data: Message) {
        when (data) {
            is HostAdmResolved -> {
                @Suppress("DeferredResultUnused")
                admpoints.getOrPut(data.host) { CompletableDeferred(data.admPoint) }
                directory.set(data.host, data.admPoint.address)
            }

            is ResetNetworkCommand -> scan()

            is OutgoingSerializedCommand -> send(data)
        }
    }

    /**
     * Асинхронное разрешение адреса управляющей конечной точки для удаленного хоста
     *
     * @param target Идентификатор хоста
     *
     * @return Управляющая конечная точка хоста
     */
    private suspend fun resolve(target: HostID): InetSocketAddress? {
        val ep = admpoints.getOrPut(target) { resolveAsync(target) }.await()

        @Suppress("DeferredResultUnused")
        if (ep == null)
            admpoints.remove(target)

        return ep
    }

    /**
     * Асинхронное разрешение адреса управляющей конечной точки для удаленного хоста
     *
     * @param target Идентификатор хоста
     *
     * @return Управляющая конечная точка хоста
     *
     * Разрешение происходит путем групповой отправки команды Ping и ожидания ответа на нее.
     * Ожидание ограничено 1 сек. Первый же ответ становится результатом операции, а остальные отбрасываются.
     * Результат неудачного разрешения кешируется на 2 сек., а затем удаляется.
     */
    private fun resolveAsync(target: HostID): Deferred<InetSocketAddress?> {
        Log?.writeLog(LogImportance.Info, logCat, "Разрешение административной точки хоста $target")

        val cd = CompletableDeferred<InetSocketAddress?>()

        val (tk, j) = register<InetSocketAddress?>(CancellationTokenSource(1000).token)

        bus.sendMessage(NetworkPingCommand(HostID.Local, target, tk, null))

        launch {
            val r = j.await()

            if (r != null && cd.complete(r))
                Log?.writeLog(LogImportance.Warning, logCat, "Административная точка хоста $target найдена по адресу $r")
        }

        launch {
            delay(2000)

            if (cd.complete(null))
                Log?.writeLog(LogImportance.Warning, logCat, "Административная точка хоста $target не найдена")
        }

        return cd
    }

    override fun beginReceive() {
        GlobalScope.launch(Dispatchers.IO) {
            val buf = ByteArray(16384)

            while (!CancellationToken.isCancellationRequested) {
                val dp = DatagramPacket(buf, buf.size)

                _mcsocket.receive(dp)

                val l = dp.length

                if (l == 0)
                    continue

                val ba = ByteArray(l)
                dp.data.copyInto(ba, endIndex = l)

                launch { received(ba, dp.socketAddress as InetSocketAddress) }
            }
        }
    }

    @Suppress("RedundantSuspendModifier")
    private suspend fun received(data: ByteArray, from: InetSocketAddress) {

        if (data.count() == 0)
            return

        onReceive(IncomingSerializedCommand(data, from))
    }

    fun send(command: OutgoingSerializedCommand) {

        val d = command.data
        val tgt = command.toHost

        fun send(send: InetSocketAddress?) {
            if (send == null)
                return

            Log?.writeLog(LogImportance.Trace, logCat, "Отправка команды по адресу $send")

            unicastudp.send(d, send)
        }

        if (command.toEndPoint != null)
            send(command.toEndPoint)
        else if (tgt.isBroadcast || command.isBroadcast)
            multicasts.forEach(::send)
        else {
            launch {
                val target = resolve(tgt) ?: return@launch
                send(target)
            }
        }
    }
}