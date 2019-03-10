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

class NetworkCommandSocket(Bus: IChannel,
                           AdmPort: Int,
                           Log: Logger?,
                           CommandFactory: IPeerCommandFactory,
                           private val directory: IPAddressDirectory,
                           private val CancellationToken: CancellationToken) :
        PeerCommandSocket(Bus, Log, CommandFactory, CancellationToken) {

    private val _unicastudp: SafeUdpClient

    /**
     * Multicast сокет. Используется **только** для приема широковещательной рассылки
     * Отправка **всегда** через unicast
     */
    private val _mcsocket: MulticastSocket

    private val _admpoints = ConcurrentHashMap<HostID, CommandEndPoints>()
    private val _admhosts = ConcurrentHashMap<InetSocketAddress, HostID>()

    private val mtx = Mutex()

    var IgnoreFromHost: HostID? = null

    init {
        _unicastudp = SafeUdpClient(InetSocketAddress("::", 0), CancellationToken, false, ::received)

        val cep = CommandEndPoints()

        cep.add(InetSocketAddress(Inet6Address.getAllByName("[FF02::1]")[0], AdmPort));

        _admpoints[HostID.All] = cep;
        _admpoints[HostID.Network] = cep;

        _mcsocket = MulticastSocket(AdmPort)
        _mcsocket.joinGroup(InetAddress.getByName("FF02::1"))

        beginReceive()
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

        try {
            mtx.lock()

            directory.set(cmd.fromHost, from.address)

            if (!CommandFactory.filter(cmd))
                return

            val ig = IgnoreFromHost

            if (ig != null && ig == cmd.fromHost)
                return

            _admhosts.getOrPut(from) { cmd.fromHost }
            _admpoints.getOrPut(cmd.fromHost) { CommandEndPoints().add(from) }

            onReceive(cmd)
        } finally {
            if (cmd is HostInfoCommand)
                cmd.release()

            mtx.unlock()
        }
    }

    override fun send(datagram: ByteArray, target: HostID) {
        if (!target.isNetwork)
            return

        val epl = _admpoints[target] ?: return
        val send = epl.getPrimary() ?: return

        Log?.writeLog(LogImportance.Trace, LogCat, "Отправка команды по адресу $send");

        _unicastudp.send(datagram, send)
    }

    override fun dumpHostInfoCommand(cmd: HostInfoCommand) {
        if (Log == null)
            return

//        cmd.endPoints.forEach {
//            Log.writeLog(LogImportance.Info, LogCat, "EPI: ${it.channelName} at ${it.target}:${it.port}")
//        }
    }
}