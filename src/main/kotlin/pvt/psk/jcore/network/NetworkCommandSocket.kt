package pvt.psk.jcore.network

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*
import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.net.*
import java.util.concurrent.*
import kotlin.coroutines.*

class NetworkCommandSocket(Bus: IChannel,
                           val admPort: Int,
                           Log: Logger?,
                           CommandFactory: IPeerCommandFactory,
                           private val directory: IPAddressDirectory,
                           private val CancellationToken: CancellationToken) :
        PeerCommandSocket(Bus, Log, CommandFactory, CancellationToken), CoroutineScope {

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

    private val mtx = Mutex()

    var IgnoreFromHost: HostID? = null

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
            Log?.writeLog(LogImportance.Info, LogCat, "Найден подходящий сетевой интерфейс $it %${it.index}")
        }

        val mcids = ifcs.map { it.index }.toIntArray()
        mcids.forEach { z ->
            try {
                _mcsocket.joinGroup(InetSocketAddress(mca, admPort), ifcs.find { it.index == z })
            } catch (e: SocketException) {
            }
        }

        multicasts = mcids.map { InetSocketAddress(InetAddress.getByName("FF02::1%$it"), admPort) }.toTypedArray()
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
        Log?.writeLog(LogImportance.Info, LogCat, "Разрешение административной точки хоста $target")

        val cd = CompletableDeferred<InetSocketAddress?>()

        multicasts.forEach {
            val (tk, j) = register<InetSocketAddress?>(CancellationTokenSource(1000).token)

            launch {
                val r = j.await()

                if (r != null && cd.complete(r))
                    Log?.writeLog(LogImportance.Warning, LogCat, "Административная точка хоста $target найдена по адресу $r")
            }

            unicastudp.send(PingCommand(HostID.Local, target, tk).toBytes(CommandFactory), it)
        }

        launch {
            delay(2000)

            if (cd.complete(null))
                Log?.writeLog(LogImportance.Warning, LogCat, "Административная точка хоста $target не найдена")
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

    private suspend fun received(data: ByteArray, from: InetSocketAddress) {

        if (data.count() == 0)
            return

        val rd = BinaryReader(data)

        val cmd = CommandFactory.create(rd) ?: return

        @Suppress("DeferredResultUnused")
        admpoints.getOrPut(cmd.fromHost) { CompletableDeferred<InetSocketAddress?>(from) }

        when (cmd) {
            is PingCommand -> {
                replyPing(cmd.fromHost, from, cmd.token)
                return
            }

            is PingReplyCommand -> {
                cmd.token.received(from)
                return
            }
        }

        try {
            mtx.lock()

            directory.set(cmd.fromHost, from.address)

            if (!CommandFactory.filter(cmd))
                return

            val ig = IgnoreFromHost

            if (ig != null && ig == cmd.fromHost)
                return

            onReceive(cmd)
        } finally {
            if (cmd is HostInfoCommand)
                cmd.release()

            mtx.unlock()
        }
    }

    /**
     * Формирует команду PingReply для отправителя команды Ping с переданной меткой
     *
     *
     * @param from Отправитель команды Ping
     * @param to Адресат команды PingReply
     * @param token Метка команды
     */
    private fun replyPing(from: HostID, to: InetSocketAddress, token: AckToken): Unit =
            unicastudp.send(PingReplyCommand(HostID.Local, from, token).toBytes(CommandFactory), to)

    override fun send(datagram: ByteArray, target: HostID) {

        fun send(send: InetSocketAddress?) {
            if (send == null)
                return

            Log?.writeLog(LogImportance.Trace, LogCat, "Отправка команды по адресу $send")

            unicastudp.send(datagram, send)
        }

        if (target.isBroadcast)
            multicasts.forEach(::send)
        else {
            GlobalScope.launch(Dispatchers.Unconfined) {
                val tgt = resolve(target) ?: return@launch
                send(tgt)
            }
        }
    }

    override fun dumpHostInfoCommand(cmd: HostInfoCommand) {
        if (Log == null)
            return

//        cmd.endPoints.forEach {
//            Log.writeLog(LogImportance.Info, LogCat, "EPI: ${it.channelName} at ${it.target}:${it.port}")
//        }
    }
}