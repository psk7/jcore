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
        _nss = NetworkSenderSocket(HostID, CancellationToken, Logger)

        basePort = _nss.basePort

        Logger?.writeLog(LogImportance.Info, logCat, "Создан канал передачи данных $Name. Порт $basePort")

        directory.set(HostID, Inet6Address.getLoopbackAddress())

        initComplete()
    }

    override fun onHostRemove(host: EndPoint) {
        host.close()
    }

    override fun onHostUpdate(endPointInfo: EndPointInfo, endPoint: EndPoint) {
        //val ha = InetSocketAddress(directory.resolve(endPointInfo.target) ?: throw Exception(), endPointInfo.port)

        if (endPoint !is NetworkEndPoint)
            return

        endPoint.isReadOnly = endPointInfo.readOnly
    }

    override fun onHostCreate(endPointInfo: EndPointInfo): EndPoint {

        val ipe = InetSocketAddress(directory.resolve(endPointInfo.target) ?: throw Exception(), endPointInfo.port)

        val hid = endPointInfo.target

        val ep = NetworkEndPoint(data, _nss, hid, directory, ipe.port, controlBus, true, endPointInfo.canReceiveStream)

        logger?.writeLog(LogImportance.Info, logCat, "Создана конечная точка $ep в канале $name")

        return ep
    }

    override fun processPollCommand(command: PollCommand) {
        command.registerChannel(name, this)
    }
}