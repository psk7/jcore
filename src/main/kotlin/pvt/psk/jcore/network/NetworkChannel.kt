package pvt.psk.jcore.network

import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.net.*

/**
 * Канал передачи сообщений на основе IP сетей
 */
class NetworkChannel(Name: String, ControlBus: IChannel, Data: Router,
                     HostID: HostID, private val directory: IPAddressDirectory,
                     Logger: Logger?,
                     CancellationToken: CancellationToken)
    : BaseChannel(Name, ControlBus, Data, Logger, CancellationToken) {

    @Suppress("JoinDeclarationAndAssignment")
    private val _nss: NetworkSenderSocket

    val basePort: Int

    init {
        _nss = NetworkSenderSocket(HostID, ControlBus, CancellationToken, Logger)

        basePort = _nss.basePort

        Logger?.writeLog(LogImportance.Info, logCat, "Создан канал передачи данных $Name. Порт $basePort")

        directory.set(HostID, Inet6Address.getLoopbackAddress())

        initComplete()
    }

    override fun onHostUpdate(endPointInfo: EndPointInfo, endPoint: EndPoint) {
        //val ha = InetSocketAddress(directory.resolve(endPointInfo.target) ?: throw Exception(), endPointInfo.port)

        if (endPoint !is NetworkEndPoint)
            return

        endPoint.dontSendTo = endPointInfo.dontSendTo
    }

    override fun onHostCreate(endPointInfo: EndPointInfo): EndPoint =
            NetworkEndPoint(data, _nss, endPointInfo.target, directory, endPointInfo.port,
                            endPointInfo.dontSendTo, endPointInfo.canReceiveStream).also {
                logger?.writeLog(LogImportance.Info, logCat, "Создана конечная точка $it в канале $name")
            }

    override fun processPollCommand(command: PollCommand) {
        command.registerChannel(name, this)
    }
}