package pvt.psk.jcore.administrator

import kotlinx.atomicfu.*
import kotlinx.coroutines.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.channel.commands.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.util.*
import java.util.concurrent.*
import kotlin.collections.HashSet

/**
 * Протокол обмена информацией о соседних хостах
 */
abstract class PeerProtocol(val selfHostID: HostID, controlChannel: IChannel, val logger: Logger?) {

    val logCat: String = "Peer"

    data class HostData(var lastSequenceID: Int)

    private val hosts = ConcurrentHashMap<HostID, HostData>()
    private val curseqid = atomic(1)
    private val _chans = Hashtable<String, HashSet<HostID>>()

    @Suppress("MemberVisibilityCanBePrivate")
    protected val Control: IChannelEndPoint

    init {
        Control = controlChannel.getChannel(::controlReceived)
    }

    /**
     * Отправка команды Discovery удаленным хостам
     */
    fun discovery() = Control.sendMessage(DiscoveryCommand(selfHostID, HostID.Network))

    fun getHosts(): Array<HostID> = hosts.keys.toTypedArray()

    private fun controlReceived(@Suppress("UNUSED_PARAMETER") channel: IChannelEndPoint, packet: Message) {
        if (packet !is PeerCommand || !packet.toHost.isLocal)
            return

        onControlReceived(packet)
    }

    /**
     * Диспетчеризация полученных команд протокола
     * @param command Принятая команда
     */
    protected open fun onControlReceived(command: PeerCommand) {

        when (command) {
            is DiscoveryCommand -> onDiscovery(command.fromHost)
            is HostInfoCommand -> onHostInfo(command)
            is PingCommand -> onPing(command)
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
    private fun onHostInfo(hostInfo: HostInfoCommand) {

        val msgs = onHostInfoInt(hostInfo)

        msgs.forEach { Control.sendMessage(it) }

        if (msgs.any { it is NewHostInChannelCommand })
            sendHostInfo(hostInfo.fromHost)
    }

    private fun onHostInfoInt(hostInfo: HostInfoCommand) = synchronized(_chans) {

        val l = mutableListOf<Message>()

        val src = hostInfo.fromHost

        // Выясняем в каких каналах сейчас состоим
        val np = createPollCommand()
        Control.sendMessage(np)

        // Создаем записи в карте при необходимости
        np.channels.forEach {
            if (!_chans.containsKey(it.first))
                _chans[it.first] = HashSet()
        }

        // Работаем только с теми каналами, которые нам известны
        val fep = hostInfo.endPoints.filter { _chans.containsKey(it.channelName) }.toTypedArray()

        // Каналы в которых источник состоял
        val was = _chans.filter { c -> c.value.contains(src) }.keys.toTypedArray()

        // Каналы в которых источник состоит теперь
        val nc = fep.map { it.channelName }.toTypedArray()

        // Каналы из которых источник вышел
        val lc = was.filter { !nc.contains(it) }.toTypedArray()

        // Каналы в которые источник вошел
        @Suppress("UNUSED_VARIABLE")
        val ec = nc.filter { !was.contains(it) }.toTypedArray()

        l.addAll(nc.map { chan ->
            val ep = fep.firstOrNull { it.channelName == chan }!!
            if (_chans[chan]!!.add(src))
                NewHostInChannelCommand(ep, hostInfo.completion)
            else
                UpdateHostInChannelCommand(ep)
        })

        l.addAll(lc.mapNotNull { chan ->
            if (_chans[chan]!!.remove(src))
                HostLeaveChannelCommand(src, chan)
            else null
        })

        return@synchronized l
    }

    /**
     * Фильтрация команды
     *
     * Команды HostInfoCommand должны обрабатываться точно в том же порядке, в котором были созданы.
     * Причем опоздавшие команды уже не актуальны.
     * Фильтрация осуществляется на основе поля sequenceID.
     * Принятые команды со значением sequenceID меньшим или равным уже обработанной **отбрасываются**.
     */
    fun filter(command: PeerCommand): Boolean {

        if (command !is HostInfoCommand)
            return true

        var isHostKnown: Boolean

        synchronized(hosts) {
            var hd = hosts.get(command.fromHost)
            isHostKnown = hd != null

            if (!isHostKnown) {
                hd = HostData(command.sequenceID)
                hosts[command.fromHost] = hd
            } else if (command.sequenceID <= hd!!.lastSequenceID) {
                logger?.writeLog(LogImportance.Info, logCat,
                                 "Команда $command проигнорирована из-за значения SeqID=${command.sequenceID} <= ${hd.lastSequenceID}")
                return false
            }

            hd.lastSequenceID = command.sequenceID
        }

        return true
    }

    /**
     * Отправки инфомации о конечных точках указанному хосту
     * @param toHost Идентификатор хоста получателя команды
     */
    fun sendHostInfo(toHost: HostID) {

        logger?.writeLog(LogImportance.Info, logCat, "Отправка информации о каналах")

        val np = createPollCommand()

        Control.sendMessage(np)

        Control.sendMessage(np.createHostInfoCommand(curseqid.incrementAndGet(), selfHostID, toHost))
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
    @Suppress("unused")
    private fun onPingReply(PingReply: PingReplyCommand) = PingReply.token.received(true)
}



