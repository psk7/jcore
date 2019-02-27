package pvt.psk.jcore.channel

import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import kotlinx.coroutines.*
import pvt.psk.jcore.host.*
import java.util.concurrent.*

abstract class BaseChannel(val Name: String,
                           val Peer: PeerProtocol,
                           val ControlBus: IChannel,
                           val Data: Router,
                           val Logger: Logger?,
                           val CancellationToken: CancellationToken) {

    val logCat = "BaseChannel"

    private val svcEp: IChannelEndPoint = Data.getChannel(description = "ServiceEndPoint of $Name")
    private var cbEp: IChannelEndPoint? = null

    private val _l = ConcurrentHashMap<HostID, EndPoint>()

    lateinit var localEndPoint: EndPoint
        protected set

    val isClosed: Boolean
        get() = true

    protected fun initComplete() {
        cbEp = ControlBus.filterLocal(::ControlReceived)
    }

    fun getChannel(received: DataReceived?, description: String): IChannelEndPoint = Data.getChannel(received, description)

    protected abstract fun processPollCommand(command: PollCommand)

    fun close() {
        //_cts.Cancel()

        Logger?.writeLog(LogImportance.Info, logCat, "Канал $Name закрывается")

        cbEp?.close()
    }

    protected abstract fun onHostRemove(host: EndPoint)
    protected abstract fun onHostUpdate(command: HostInfoCommand, endPointInfo: EndPointInfo, endPoint: EndPoint)
    protected abstract fun onHostCreate(Command: HostInfoCommand, EndPointInfo: EndPointInfo): Deferred<EndPoint>

    private fun ControlReceived(c: IChannelEndPoint, m: Message) {

        when (m) {
            is HostInfoCommand -> {
                val j = processHostInfo(m)
                if (j != null)
                    m.addTask(j)
            }

            is PollCommand     -> processPollCommand(m)
        }
    }

    private fun processHostInfo(command: HostInfoCommand): Job? {
        val fh = command.FromHost

        val chen = command.endPoints.firstOrNull { it.channelName == Name }

        if (isClosed)
            return null

        val f = _l.get(fh)

        if (chen == null) {
            if (f == null)
                return null

            f.close()

            _l.remove(fh)

            onHostRemove(f)

            Logger?.writeLog(LogImportance.Info, logCat, "Хост ${fh.Name}<${fh.ID}> удален из канала $Name")

            if (_l.count() == 0)
                close()

            return null
        }

        if (f != null) {
            onHostUpdate(command, chen, f)
        } else {
            return GlobalScope.async {
                _l[fh] = onHostCreate(command, chen).await()

                command.complete().await()

                Logger?.writeLog(LogImportance.Info, logCat, "Хост ${fh.Name}<${fh.ID}> добавлен в канал ${Name}")

                Peer.sendHostInfo(fh)

                svcEp.sendMessage(NewHostInChannel(fh, HostID.Local))
            }
        }

        return null
    }
}