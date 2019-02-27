package pvt.psk.jcore.network

import kotlinx.coroutines.*
import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.net.*

class NetworkChannel(Name: String,
                     Peer: PeerProtocol,
                     ControlBus: IChannel,
                     Data: Router,
                     val HostID: HostID,
                     Logger: Logger?,
                     bindAddress: InetSocketAddress,
                     CancellationToken: CancellationToken) : BaseChannel(Name, Peer, ControlBus, Data, Logger, CancellationToken) {

    val _nss: NetworkSenderSocket
    val _ipl = IPEndPointsList()

    init {

        _nss = NetworkSenderSocket(CancellationToken, Logger)
    }

    val networkLocalEndPoint: NetworkEndPoint
        get() = super.localEndPoint as NetworkEndPoint

    override fun onHostRemove(host: EndPoint) {
        host.close()
    }

    override fun onHostUpdate(command: HostInfoCommand, endPointInfo: EndPointInfo, endPoint: EndPoint) {
        GlobalScope.launch {

            var ha = InetSocketAddress(command.getSourceIPAddress().await(), endPointInfo.port)

            _ipl.found(ha, endPoint);

            if (endPoint is NetworkEndPoint) {
                endPoint.isReadOnly = endPointInfo.readOnly;
                endPoint.updateIPAddresses(ha);
            }
        }
    }

    override fun onHostCreate(Command: HostInfoCommand, EndPointInfo: EndPointInfo): Deferred<EndPoint> = GlobalScope.async {

        val ipe = InetSocketAddress(Command.getSourceIPAddress().await(), EndPointInfo.port)

        val hid = Command.FromHost

        val ep = NetworkEndPoint(Data, _nss, hid, ControlBus, true)
        ep.updateIPAddresses(ipe)

        Logger?.writeLog(LogImportance.Info, logCat, "Создана конечная точка $ep в канале $Name")

        _ipl.found(ipe, ep)

        return@async ep
    }

    override fun processPollCommand(command: PollCommand) {
        command.registerChannel(Name, this)
    }
}