package pvt.psk.jcore.instance

import kotlinx.atomicfu.*
import kotlinx.coroutines.*
import org.koin.core.*
import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.relay.*
import java.util.concurrent.*
import kotlin.coroutines.*

abstract class BaseInstance(name: String, val domainName: String) : CoroutineScope, KoinComponent {

    protected val supervisorJob = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + supervisorJob

    protected val logCat = "Instance"
    protected val log: Logger by inject()

    private val hostInfoSeqId = atomic(0)

    private val admChannelId: UShort = 0.toUShort()

    private val channels = ConcurrentHashMap<String, BaseChannel>()
    private val chanNums = ConcurrentHashMap<UShort, BaseChannel>()
    private val lastHostInfo = ConcurrentHashMap<HostID, Int>()

    private val chanSinks = ConcurrentHashMap<UShort, (ChannelEnvelope) -> Unit>()

    private val hostInfoLock = Any()

    private val controlBus = Router()
    protected var relay: IRelay? = null

    private var chanIds = 10

    val hostID = HostID.new(name)

    open fun init() {
        log.writeLog(LogImportance.Info, logCat, "Создан экземпляр $hostID")

        relay?.addAdjacentHost(hostID, ::dispatchRelayMessage, ::sendHostInfo)
    }

    /**
     * Отправка сообщений сервисных каналов
     */
    private fun sendServiceChannel(data: Message, serviceChannelID: UShort, crossDomain: Boolean): Unit =
        relay?.send(RelayEnvelope(HostEndpointID(hostID, serviceChannelID),
                                  if (crossDomain) arrayOf(HostEndpointID(HostID.All, serviceChannelID))
                                  else lastHostInfo.map { HostEndpointID(it.key, serviceChannelID) }.toTypedArray(), data))!!

    fun leaveAllChannels(): Unit = synchronized(this) {
        channels.values.forEach {
            it.close()
        }
    }

    open fun close() {
    }

    /**
     * Подключается к именованному каналу обмена сообщениями
     *
     * @param channelName Имя канала обмена сообщениями
     *
     * @return Канал обмена сообщениями
     */
    fun joinChannel(channelName: String): BaseChannel = synchronized(this) {

        var c = channels.get(channelName)

        if (c != null)
            return@synchronized c

        val chid = chanIds++.toUShort()

        c = BaseChannel(channelName, chid, { m -> dispatchBusMessage(chid, m) })
        channels[channelName] = c
        chanSinks[chid] = c::receivedFromRelay
        chanNums[c.channelId] = c

        sendHostInfo()

        return@synchronized c
    }

    /**
     * Маршрутизация сообщений каналов ретранслятору
     */
    private fun dispatchBusMessage(channelId: UShort, packet: ChannelEnvelope) {

        if (!packet.from.isLocal || packet.targetIsLocal)
            return

        val c = chanNums[channelId] ?: return

        var av = c.remotes.values.filter { it.isAvailable } // Доступные точки

        if (!packet.targetIsBroadcast)
            av = av.filter { packet.targets.contains(it.target.hostID) }

        if (packet.payload is DataPacket && !packet.payload.tag.isEmpty)
            av = av.filter { it.acceptTags == null || it.acceptTags!!.isEmpty() || it.acceptTags!!.contains(packet.payload.tag) }

        val tgts = av.map { it.target }.toTypedArray()

        if (tgts.isNotEmpty())
            relay?.send(RelayEnvelope(HostEndpointID(hostID, channelId), tgts, packet.payload))
    }

    /**
     * Маршрутизация сообщений ретранслятора каналам
     */
    private fun dispatchRelayMessage(message: RelayEnvelope) {

        val th = message.targets.singleValue ?: return

        val fh = message.from.hostID

        if (fh == hostID || th.hostID != hostID)
            return

        when (message.payload) {
            is DataPacket      -> chanSinks[th.endpointID]?.invoke(ChannelEnvelope(fh, HostID.Local, message.payload))
            is HostInfoCommand -> onHostInfo(message)
        }
    }

    /**
     * Обработка команды HostInfo
     */
    protected fun onHostInfo(hostInfo: RelayEnvelope) {

        val hic = hostInfo.payload as? HostInfoCommand ?: return

        if (hic.domain != domainName)
            return

        var f = !lastHostInfo.containsKey(hostInfo.from.hostID)

        val from = hostInfo.from.hostID

        synchronized(hostInfoLock) {
            val lastId = lastHostInfo[from]

            if (lastId != null && lastId >= hic.sequenceId)
                return

            log.writeLog(LogImportance.Trace, logCat,
                         "Получена информация о каналах от ${hostInfo.from} (#${hic.sequenceId}) : ${hic.endPoints.map { "${it.channelName} (${it.acceptTags.joinToString()})" }
                             .joinToString()}")

            lastHostInfo[from] = hic.sequenceId

            channels.forEach { ch ->
                val ep = hic.endPoints.firstOrNull { x -> x.channelName == ch.key }

                f = f or ch.value.updateHostEndpointInfo(hostInfo.from.hostID, ep, hic.sequenceId)
            }
        }

        if (f)
            sendHostInfo()
    }

    fun sendHostInfo() {

        log.writeLog(LogImportance.Trace, logCat, "Отправка информации о каналах")

        val l = channels.map { EndPointInfo(HostEndpointID(hostID, it.value.channelId), it.key, it.value.acceptTags) }.toTypedArray()

        sendServiceChannel(HostInfoCommand(domainName, l, hostInfoSeqId.incrementAndGet()), admChannelId, true)
    }
}