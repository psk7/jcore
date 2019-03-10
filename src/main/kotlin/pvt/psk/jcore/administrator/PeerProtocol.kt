package pvt.psk.jcore.administrator

import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

/**
 * Протокол обмена информацией о соседних хостах
 */
abstract class PeerProtocol(val selfHostID: HostID, val domain: String, controlChannel: IChannel,
                            val logger: Logger?) : IPeerCommandFactory {

    val logCat: String = "Peer"

    data class HostData(var lastSequenceID: Int)

    val hosts = ConcurrentHashMap<HostID, HostData>()
    val curseqid = AtomicInteger(1)

    protected val Control: IChannelEndPoint

    init {
        Control = controlChannel.filterLocal().getChannel(::controlReceived)
    }

    /**
     * Отправка команды Discovery удаленным хостам
     */
    fun discovery() = Control.sendMessage(DiscoveryCommand(selfHostID, HostID.Network))

    /**
     * Отправка команды Leave удаленным хостам
     */
    fun leave() = Control.sendMessage(LeaveCommand(selfHostID, HostID.Network))

    fun getHosts(): Array<HostID> = hosts.keys.toTypedArray()

    private fun controlReceived(@Suppress("UNUSED_PARAMETER") channel: IChannelEndPoint, packet: Message) {
        val fh = packet.fromHost
        var th = packet.toHost

        if (th == HostID.All || th == HostID.Local)
            th = selfHostID

        if (fh == selfHostID)
            return

        // Посторонние команды отбрасываются
        if (fh != selfHostID && th != selfHostID)
            return

        onControlReceived(packet, fh)
    }

    /**
     * Диспетчеризация полученных команд протокола
     * @param command Принятая команда
     * @param fromHost Идентификатор хоста источника команды
     */
    protected open fun onControlReceived(command: Message, fromHost: HostID) {

        when (command) {
            is DiscoveryCommand -> onDiscovery(fromHost)
            is HostInfoCommand -> onHostInfo(command, fromHost)
            is LeaveCommand -> onLeave(fromHost)
            is PingCommand -> onPing(command)
            is PingReplyCommand -> onPingReply(command)
        }
    }

    /**
     * Обработка команды DiscoveryCommand
     *
     * В ответ отправителю отсылается команда HostInfoCommand с информацией о своих конечных точках
     * Если это первая команда, полученная от этого отправителя, ему, дополнительно, отсылается команда Discovery
     */
    private fun onDiscovery(fromHost: HostID) {
        val isHostKnown = hosts.containsKey(fromHost)

        sendHostInfo(fromHost)

        if (isHostKnown)
            return

        Control.sendMessage(DiscoveryCommand(selfHostID, fromHost))
    }

    /**
     * Обработка команды HostInfoCommand
     */
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

            hd.lastSequenceID = HostInfo.SequenceID
        }

        if (!isHostKnown) {
            logger?.writeLog(LogImportance.Info, logCat, "В домене обнаружен хост $FromHost")

            onNewHost(FromHost)
        }

        processHostInfoCommand(HostInfo)

        onHostInfo(HostInfo)
    }

    /**
     * Фильтрация команды
     *
     * Команды HostInfoCommand должны обрабатываться точно в том же порядке, в котором были созданы.
     * Причем опоздавшие команды уже не актуальны.
     * Фильтрация осуществляется на основе поля SequenceID.
     * Принятые команды со значением SequenceID меньшим или равным уже обработанной **отбрасываются**.
     */
    override fun filter(command: PeerCommand): Boolean {

        if (command !is HostInfoCommand)
            return true

        var isHostKnown: Boolean

        synchronized(hosts) {
            var hd = hosts.get(command.fromHost)
            isHostKnown = hd != null

            if (!isHostKnown) {
                hd = HostData(command.SequenceID)
                hosts[command.fromHost] = hd
            } else if (command.SequenceID <= hd!!.lastSequenceID) {
                logger?.writeLog(LogImportance.Info, logCat,
                                 "Команда $command проигнорирована из-за значения SeqID=${command.SequenceID} <= ${hd.lastSequenceID}")
                return false
            }

            hd.lastSequenceID = command.SequenceID
        }

        return true
    }

    /**
     * Обработка команды LeaveCommand
     */
    private fun onLeave(LeavingHost: HostID) {

        val rl = synchronized(hosts) { hosts.remove(LeavingHost) != null }

        if (rl)
            logger?.writeLog(LogImportance.Info, logCat, "Хост {$LeavingHost} покидает домен")

        onLeaveHost(LeavingHost)
    }

    protected open fun onHostInfo(Command: HostInfoCommand): Unit = Unit
    protected open fun onNewHost(FromHost: HostID): Unit = Unit
    protected open fun onLeaveHost(FromHost: HostID): Unit = Unit

    /**
     * Отправки инфомации о конечных точках указанному хосту
     * @param toHost Идентификатор хоста получателя команды
     */
    fun sendHostInfo(toHost: HostID) {

        logger?.writeLog(LogImportance.Info, logCat, "Отправка информации о каналах")

        val np = createPollCommand()

        Control.sendMessage(np)

        Control.sendHostInfo(np.createHostInfoCommand(curseqid.incrementAndGet(), selfHostID, toHost))
    }

    fun adjustTargetHost(From: HostID, To: HostID): Pair<Boolean, HostID> {

        if (From == selfHostID) // Свои пакеты отбрасываются
            return Pair(false, To)

        if (To != HostID.All && To != selfHostID)
            return Pair(false, To)

        var to = To

        if (To == HostID.All || To == selfHostID)
            to = HostID.Local

        return Pair(true, to)
    }

    protected abstract fun createPollCommand(): PollCommand

    /**
     * Обработка команды PingCommand
     * Отправляет в ответ команду PingReplyCommand
     */
    private fun onPing(Ping: PingCommand) = Control.sendMessage(
            PingReplyCommand(HostID.Local, Ping.fromHost, Ping.token))

    /**
     * Обработка команды PingReplyCommand
     * Информирует ожидающих о поступлении команды
     */
    private fun onPingReply(PingReply: PingReplyCommand) = PingReply.token.received(true)

    protected abstract fun processHostInfoCommand(Command: HostInfoCommand)
}



