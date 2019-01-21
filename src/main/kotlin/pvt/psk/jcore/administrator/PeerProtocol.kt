package pvt.psk.jcore.administrator

import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import java.util.concurrent.*
import pvt.psk.jcore.utils.*
import java.util.concurrent.atomic.*

abstract class PeerProtocol(val selfHostID: HostID, val domain: String, val controlChannel: IChannel, val logger: Logger?) {
    val logCat: String = "Peer"

    data class HostData(var lastSequenceID: Int)

    val hosts = ConcurrentHashMap<HostID, HostData>()
    val curseqid = AtomicInteger(1)

    // Control.FilterLocal().OnDataReceived += ControlReceived;

    fun discovery() = controlChannel.sendMessage(DiscoveryCommand(selfHostID, HostID.Network))
    fun leave() = controlChannel.sendMessage(LeaveCommand(selfHostID, HostID.Network))

    fun getHosts() = hosts.keys

    private fun controlReceived(channel: IChannel, packet: Message) {
        val fh = packet.FromHost
        var th = packet.ToHost

        if (th == HostID.All || th == HostID.Local)
            th = selfHostID

        if (fh == selfHostID)
            return

        // Посторонние команды отбрасываются
        if (fh != selfHostID && th != selfHostID)
            return

        onControlReceived(packet, fh);
    }

    protected open fun onControlReceived(packet: Message, fromHost: HostID) {

        when (packet) {
            is DiscoveryCommand -> onDiscovery(fromHost)
            is HostInfoCommand  -> onHostInfo(packet, fromHost)
            is LeaveCommand     -> onLeave(fromHost)
            is PingCommand      -> onPing(packet)
            is PingReplyCommand -> onPingReply(packet)
        }
    }

    private fun onDiscovery(fromHost: HostID) {

        var tgt = fromHost;

        val isHostKnown = hosts.containsKey(tgt);

        sendHostInfo(tgt);

        if (isHostKnown)
            return;

        controlChannel.sendMessage(DiscoveryCommand(selfHostID, tgt));
    }

    private fun onHostInfo(HostInfo: HostInfoCommand, FromHost: HostID) {

        var isHostKnown: Boolean

        synchronized(hosts) {
            var hd = hosts.get(FromHost)
            isHostKnown = hd != null

            if (!isHostKnown) {
                hd = HostData(HostInfo.SequenceID)
                hosts[FromHost] = hd
            } else if (HostInfo.SequenceID <= hd!!.lastSequenceID)
                return

            hd.lastSequenceID = HostInfo.SequenceID;
        }

        if (!isHostKnown) {
            logger?.writeLog(LogImportance.Info, logCat, "В домене обнаружен хост {$FromHost}");

            onNewHost(FromHost);
        }

        ProcessHostInfoCommand(HostInfo);

        onHostInfo(HostInfo)
    }

    private fun onLeave(LeavingHost: HostID) {

        val rl = synchronized(hosts) { hosts.remove(LeavingHost) != null }

        if (rl)
            logger?.writeLog(LogImportance.Info, logCat, "Хост {$LeavingHost} покидает домен");

        onLeaveHost(LeavingHost);
    }

    protected open fun onHostInfo(Command: HostInfoCommand): Unit = Unit
    protected open fun onNewHost(FromHost: HostID): Unit = Unit
    protected open fun onLeaveHost(FromHost: HostID): Unit = Unit

    fun sendHostInfo(ToHost: HostID) {

        logger?.writeLog(LogImportance.Info, logCat, "Отправка информации о каналах");

        var np = createPollCommand();

        controlChannel.sendMessage(np);

        controlChannel.sendHostInfo(np.createHostInfoCommand(curseqid.incrementAndGet(), selfHostID, ToHost));
    }

    protected fun adjustTargetHost(From: HostID, To: HostID): Pair<Boolean, HostID> {

        if (From == selfHostID) // Свои пакеты отбрасываются
            return Pair(false, To)

        if (To != HostID.All && To != selfHostID)
            return Pair(false, To)

        var to = To

        if (To == HostID.All || To == selfHostID)
            to = HostID.Local;

        return Pair(true, to)
    }

    protected abstract fun createPollCommand(): PollCommand

    private fun onPing(Ping: PingCommand) = controlChannel.sendMessage(PingReplyCommand(HostID.Local, Ping.FromHost, Ping.Token))

    private fun onPingReply(PingReply: PingReplyCommand) = received(PingReply.Token, true)

    protected abstract fun ProcessHostInfoCommand(Command: HostInfoCommand)
}



