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
class NetworkChannel(Name: String, Peer: PeerProtocol, ControlBus: IChannel, Data: Router, HostID: HostID,
                     private val directory: IPAddressDirectory,
                     Logger: Logger?, bindAddress: InetSocketAddress, CancellationToken: CancellationToken)
    : BaseChannel(Name, Peer, ControlBus, Data, Logger, CancellationToken) {

    private val _nss: NetworkSenderSocket

    val basePort: Int

    init {
        _nss = NetworkSenderSocket(HostID, CancellationToken, Logger)

        basePort = _nss.basePort

        directory.set(HostID, Inet6Address.getLoopbackAddress())

        initComplete()
    }

    override fun onHostRemove(host: EndPoint) {
        host.close()
    }

    override fun onHostUpdate(command: HostInfoCommand, endPointInfo: EndPointInfo, endPoint: EndPoint) {
        val ha = InetSocketAddress(directory.resolve(command.fromHost) ?: throw Exception(), endPointInfo.port)

        if (endPoint !is NetworkEndPoint)
            return

        endPoint.isReadOnly = endPointInfo.readOnly;
    }

    override fun onHostCreate(command: HostInfoCommand, EndPointInfo: EndPointInfo): EndPoint {

        val ipe = InetSocketAddress(directory.resolve(command.fromHost) ?: throw Exception(), EndPointInfo.port)

        val hid = command.fromHost

        val ep = NetworkEndPoint(Data, _nss, hid, directory, ipe.port, ControlBus, true,
                                 canReceiveStream = EndPointInfo.canReceiveStream)

        Logger?.writeLog(LogImportance.Info, logCat, "Создана конечная точка $ep в канале $Name")

        return ep
    }

    override fun processPollCommand(command: PollCommand) {
        command.registerChannel(Name, this)
    }
}