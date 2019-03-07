package pvt.psk.jcore.channel

import kotlinx.coroutines.*
import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.util.concurrent.*

abstract class BaseChannel(val Name: String,
                           protected val Peer: PeerProtocol,
                           protected val ControlBus: IChannel,
                           protected val Data: Router,
                           protected val Logger: Logger?,
                           cancellationToken: CancellationToken) {

    val logCat = "BaseChannel"

    private val svcEp: IChannelEndPoint
    lateinit private var cbEp: IChannelEndPoint

    private val _l = ConcurrentHashMap<HostID, EndPoint>()

    private val cts: CancellationTokenSource

    protected val cancToken: CancellationToken

    lateinit var localEndPoint: EndPoint
        protected set

    val isClosed: Boolean
        get() = cts.isCancellationRequested

    init {

        svcEp = Data.getChannel(description = "ServiceEndPoint of $Name")

        cts = cancellationToken.getSafeToken()
        cancToken = cts.token
    }

    protected fun initComplete() {
        cbEp = ControlBus.filterLocal(::controlReceived)
    }

    fun getChannel(received: DataReceived? = null, description: String? = null): IChannelEndPoint = Data.getChannel(
            received, description)

    protected abstract fun processPollCommand(command: PollCommand)

    fun close() {
        //_cts.Cancel()

        Logger?.writeLog(LogImportance.Info, logCat, "Канал $Name закрывается")

        cbEp.close()
    }

    protected abstract fun onHostRemove(host: EndPoint)
    protected abstract suspend fun onHostUpdate(command: HostInfoCommand, endPointInfo: EndPointInfo,
                                                endPoint: EndPoint)

    protected abstract suspend fun onHostCreate(Command: HostInfoCommand, EndPointInfo: EndPointInfo): EndPoint

    private fun controlReceived(c: IChannelEndPoint, m: Message) {

        when (m) {
            is HostInfoCommand -> {
                m.addTask(GlobalScope.launch(Dispatchers.Unconfined) { processHostInfo(m) })
            }

            is PollCommand -> processPollCommand(m)
        }
    }

    private suspend fun processHostInfo(command: HostInfoCommand) {
        if (isClosed)
            return

        val fh = command.FromHost
        val chen = command.endPoints.firstOrNull { it.channelName == Name }

        val f = _l.get(fh)

        if (chen == null) {
            if (f == null)
                return

            f.close()

            _l.remove(fh)

            onHostRemove(f)

            Logger?.writeLog(LogImportance.Info, logCat, "Хост ${fh.Name}<${fh.ID}> удален из канала $Name")

            if (_l.count() == 0)
                close()

        } else {
            if (f != null) {
                onHostUpdate(command, chen, f)
            } else {
                _l[fh] = onHostCreate(command, chen)

                command.addFinalizer {
                    Logger?.writeLog(LogImportance.Info, logCat, "Хост ${fh.Name}<${fh.ID}> добавлен в канал ${Name}")

                    Peer.sendHostInfo(fh)

                    svcEp.sendMessage(NewHostInChannel(fh, HostID.Local))
                }
            }
        }
    }
}