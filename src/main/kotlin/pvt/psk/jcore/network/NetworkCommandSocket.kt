package pvt.psk.jcore.network

import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.net.*
import java.nio.channels.*
import java.util.concurrent.*

class NetworkCommandSocket(Bus: IChannel,
                           AdmPort: Int,
                           Log: Logger?,
                           CommandFactory: IPeerCommandFactory,
                           CancellationToken: CancellationToken) :
    PeerCommandSocket(Bus, Log, CommandFactory, CancellationToken) {

    private val _mcastudp: SafeUdpClient
    private val _unicastudp: SafeUdpClient

    private val _admpoints = ConcurrentHashMap<HostID, CommandEndPoints>()
    private val _admhosts = ConcurrentHashMap<InetAddress, HostID>()

    var IgnoreFromHost: HostID? = null

    init {

        val us = UdpSelector(Selector.open())
        us.run()

        _mcastudp = SafeUdpClient(us, InetSocketAddress("::", AdmPort), CancellationToken, true)
        _unicastudp = SafeUdpClient(us, InetSocketAddress("::", AdmPort), CancellationToken, false)

        val cep = CommandEndPoints()

        cep.add(InetSocketAddress(Inet6Address.getAllByName("[FF02::1]")[0], AdmPort));

        _admpoints[HostID.All] = cep;
        _admpoints[HostID.Network] = cep;

        _mcastudp.received += ::received
        _unicastudp.received += ::received
    }

    protected fun received(data: ByteArray, from: InetSocketAddress) {
        if (data.count() == 0)
            return

    }

    override fun send(datagram: ByteArray, target: HostID) {
        if (!target.isNetwork)
            return

        val epl = _admpoints[target] ?: return
        val send = epl.getPrimary() ?: return

        Log?.writeLog(LogImportance.Trace, LogCat, "Отправка команды по адресу {$send}");

        _unicastudp.send(datagram, send)
    }

    override fun BeginReceive() {
    }

}