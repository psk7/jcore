package pvt.psk.jcore.network

import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.net.*

class NetworkChannel(Name: String, Peer: PeerProtocol, ControlBus: IChannel, Data: Router, HostID: HostID,
                     Logger: Logger?, bindAddress: InetSocketAddress, CancellationToken: CancellationToken)
    : BaseChannel(Name, Peer, ControlBus, Data, Logger, CancellationToken) {

    val _nss: NetworkSenderSocket
    val _ipl = IPEndPointsList()
    val udp = SafeUdpClient(InetSocketAddress(InetAddress.getByName("::"), 0), CancellationToken)

    init {
        _nss = NetworkSenderSocket(udp, CancellationToken, _ipl, Logger)

        val nep = NetworkEndPoint(null, _nss, HostID, ControlBus)
        nep.updateIPAddresses(InetSocketAddress(Inet6Address.getLoopbackAddress(), udp.localEndPoint.port))

        super.localEndPoint = nep

        initComplete()
    }

    val networkLocalEndPoint: NetworkEndPoint
        get() = super.localEndPoint as NetworkEndPoint

    override fun onHostRemove(host: EndPoint) {
        host.close()
    }

    override suspend fun onHostUpdate(command: HostInfoCommand, endPointInfo: EndPointInfo, endPoint: EndPoint) {
        val ha = InetSocketAddress(command.getSourceIPAddress().await(), endPointInfo.port)

        _ipl.found(ha, endPoint);

        if (endPoint is NetworkEndPoint) {
            endPoint.isReadOnly = endPointInfo.readOnly;
            endPoint.updateIPAddresses(ha);
        }
    }

    override suspend fun onHostCreate(Command: HostInfoCommand, EndPointInfo: EndPointInfo): EndPoint {

        val ipe = InetSocketAddress(Command.getSourceIPAddress().await(), EndPointInfo.port)

        val hid = Command.FromHost

        val ep = NetworkEndPoint(Data, _nss, hid, ControlBus, true, canReceiveStream = EndPointInfo.canReceiveStream)
        ep.updateIPAddresses(ipe)

        Logger?.writeLog(LogImportance.Info, logCat, "Создана конечная точка $ep в канале $Name")

        _ipl.found(ipe, ep)

        return ep
    }

    override fun processPollCommand(command: PollCommand) {
        command.registerChannel(Name, this)
    }
}