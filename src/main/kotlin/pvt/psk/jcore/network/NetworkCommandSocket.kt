package pvt.psk.jcore.network

import kotlinx.coroutines.*
import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.network.commands.*
import pvt.psk.jcore.utils.*
import java.net.*
import java.util.concurrent.*
import kotlin.coroutines.*

class NetworkCommandSocket(Bus: IChannel,
                           val admPort: Int,
                           Log: Logger?,
                           private val directory: IPAddressDirectory,
                           private val CancellationToken: CancellationToken) :
        PeerCommandSocket(Bus, Log, CancellationToken), CoroutineScope {

    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Unconfined + job

    var admResolveTimeout = 2000L

    private class Bag(var endPoint: InetSocketAddress? = null)

    private val unicastudp: SafeUdpClient
    private val admpoints = ConcurrentHashMap<HostID, Deferred<Bag>>()

    private var scanBlocked = false

    /**
     * Multicast сокет. Используется **только** для приема широковещательной рассылки
     * Отправка **всегда** через unicast
     */
    private val _mcsocket: MulticastSocket

    private var multicasts = arrayOf<InetSocketAddress>()

    val admEndPoints: Map<HostID, InetSocketAddress?>
        get() = admpoints.mapValues { it.value.result.endPoint }

    init {
        unicastudp = SafeUdpClient(InetSocketAddress("::", 0), CancellationToken, false, noBind = true, received = ::received)

        _mcsocket = MulticastSocket(admPort)

        beginReceive()

        scan()
    }

    /**
     * Сканирование сетевых интерфейсов, пересоздание административных точек, сброс карт сетевых адресов
     * и повторный цикл перерегистрации хостов
     */
    fun scan() {
        if (scanBlocked)
            return

        // Вводится блокировка повторного сканирования
        scanBlocked = true

        launch {
            // Через 2 секунды блокировка снимается
            delay(2000)

            scanBlocked = false
        }

        val mca = InetAddress.getByName("FF02::1")

        val ifcs = NetworkInterface.getNetworkInterfaces().toList().filter {
            !it.isLoopback && it.isUp && it.supportsMulticast() && !it.isPointToPoint && !it.name.contains("radio", true)
        }.associate { Pair(it.index, it) }

        ifcs.values.forEach {
            Log?.writeLog(LogImportance.Info, logCat, "Найден подходящий сетевой интерфейс $it %${it.index}")

            try {
                _mcsocket.joinGroup(InetSocketAddress(mca, admPort), it)
            } catch (e: SocketException) {
            }
        }

        multicasts = ifcs.keys.map { InetSocketAddress(InetAddress.getByName("FF02::1%$it"), admPort) }.toTypedArray()

        unicastudp.bind()

        val hosts = admpoints.keys.toTypedArray()

        admpoints.clear()
        directory.reset()

        bus.sendMessage(DiscoveryCommand(HostID.Local, HostID.All))

        launch { hosts.forEach { resolve(it) } }
    }

    override fun onBusCmd(channel: IChannelEndPoint, data: Message) {
        when (data) {
            is HostAdmResolved -> {
                launch {
                    admpoints.getOrPut(data.host) { CompletableDeferred(Bag(data.admPoint)) }.await().endPoint = data.admPoint
                }
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
        val bag = admpoints.getOrPut(target) { resolveAsync(target) }.await()

        if (bag.endPoint == null) {
            @Suppress("UNUSED_VARIABLE")
            val unused = admpoints.remove(target)
        }

        return bag.endPoint
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
    private fun resolveAsync(target: HostID): Deferred<Bag> {
        Log?.writeLog(LogImportance.Info, logCat, "Разрешение административной точки хоста $target")

        val cd = CompletableDeferred<Bag>()

        val (tk, j) = register<InetSocketAddress?>(CancellationTokenSource(1000).token)

        bus.sendMessage(NetworkPingCommand(HostID.Local, target, tk, null))

        launch {
            val r = j.await()

            if (r != null && cd.complete(Bag(r)))
                Log?.writeLog(LogImportance.Warning, logCat, "Административная точка хоста $target найдена по адресу $r")
        }

        launch {
            delay(admResolveTimeout)

            if (cd.complete(Bag(null)))
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