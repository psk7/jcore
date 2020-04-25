@file:Suppress("MemberVisibilityCanBePrivate", "JoinDeclarationAndAssignment")

package pvt.psk.jcore.channel

import kotlinx.coroutines.*
import org.koin.core.*
import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.relay.*
import pvt.psk.jcore.utils.*
import java.io.*
import java.util.concurrent.*
import kotlin.coroutines.*

/**
 * Базовый класс, представляющий пользовательский канал передачи сообщений
 */
class BaseChannel(val name: String,
                  val channelId: UShort,
                  protected val sender: (ChannelEnvelope) -> Unit) : IDataChannel, CoroutineScope, KoinComponent {

    protected val logCat = "BaseChannel"

    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    protected val dataBus: IChannel
    protected val logger: Logger by inject()

    val remotes = ConcurrentHashMap<HostID, EndPoint>()

    private val dBus: IChannelEndPoint

    private val acceptableTags = ConcurrentHashMap<PacketTag, Int>()
    private val newHostNotifiers = ConcurrentLinkedQueue<NewHostInDataChannel>()

    init {
        dataBus = Router()
        dBus = dataBus.getChannel(::busDispatch)
    }

    private fun getChannel(dataReceived: DataReceived?, acceptTag: PacketTag?, description: String?): IChannelEndPoint {
        if (acceptTag != null) {
            addAcceptTag(acceptTag)
        }

        return dataBus.getChannel(dataReceived, description)
    }

    companion object {
        fun sendMessage(to: IChannelEndPoint, data: ByteArray, target: HostID?, metadata: Array<Any>?, tag: PacketTag?): Unit =
            to.sendMessage(ChannelEnvelope(HostID.Local, target ?: HostID.All, BytesPacket(data).also { it.tag = tag ?: PacketTag.Empty }))

        fun sendMessage(to: IChannelEndPoint, data: ByteArray, targets: Array<HostID>, metadata: Array<Any>?, tag: PacketTag?): Unit =
            to.sendMessage(ChannelEnvelope(HostID.Local, targets, BytesPacket(data).also { it.tag = tag ?: PacketTag.Empty }))

        fun sendMessage(to: IChannelEndPoint, target: HostID?, metadata: Array<Any>?, tag: PacketTag?): Deferred<OutputStream> =
            sendMessage(to, arrayOf(target ?: HostID.All), metadata, tag)

        fun sendMessage(to: IChannelEndPoint, targets: Array<HostID>, metadata: Array<Any>?, tag: PacketTag?): Deferred<OutputStream> {
            val sp = StreamPacket(metadata).also { it.tag = tag ?: PacketTag.Empty }

            to.sendMessage(ChannelEnvelope(HostID.Local, targets, sp))

            sp.completed.invokeOnCompletion { sp.dispose() }

            return sp.enableTransferAsync().continueWith { sp.stream }
        }

        fun sendMessage(source: InputStream, to: IChannelEndPoint, target: HostID?, metadata: Array<Any>?, tag: PacketTag?,
                        scope: CoroutineScope): Job =
            sendMessage(source, to, arrayOf(target ?: HostID.All), metadata, tag, scope)

        fun sendMessage(source: InputStream, to: IChannelEndPoint, targets: Array<HostID>, metadata: Array<Any>?, tag: PacketTag?,
                        scope: CoroutineScope): Job {
            val sp = StreamPacket(metadata).also {
                it.tag = tag ?: PacketTag.Empty
                it.sourceStream = source
            }

            to.sendMessage(ChannelEnvelope(HostID.Local, targets, sp))

            sp.completed.invokeOnCompletion { sp.dispose() }

            return scope.launch { sp.completeStreamAsync() }
        }
    }

    override fun sendMessage(data: ByteArray, target: HostID?, metadata: Array<Any>?, tag: PacketTag?) =
        sendMessage(dataBus, data, target, metadata, tag)

    override fun sendMessage(data: ByteArray, targets: Array<HostID>, metadata: Array<Any>?, tag: PacketTag?) =
        sendMessage(dataBus, data, targets, metadata, tag)

    override fun sendStream(target: HostID?, metadata: Array<Any>?, tag: PacketTag?): Deferred<OutputStream> =
        sendMessage(dataBus, target, metadata, tag)

    override fun sendStream(targets: Array<HostID>, metadata: Array<Any>?, tag: PacketTag?): Deferred<OutputStream> =
        sendMessage(dataBus, targets, metadata, tag)

    override fun sendStream(source: InputStream, target: HostID?, metadata: Array<Any>?, tag: PacketTag?): Job =
        sendMessage(source, dataBus, target, metadata, tag, this)

    override fun sendStream(source: InputStream, targets: Array<HostID>, metadata: Array<Any>?, tag: PacketTag?): Job =
        sendMessage(source, dataBus, targets, metadata, tag, this)

    private fun getChannelInt(channel: DataChannelEndPoint, acceptTag: PacketTag?) =
        channel.apply { endPoint = getChannel(::received, acceptTag, null) }

    override fun getChannel(received: DataChannelReceived?, acceptTag: PacketTag?) =
        getChannelInt(DataChannelEndPoint(received, acceptTag), acceptTag)

    override fun getChannelStream(received: DataChannelStreamReceived?, acceptTag: PacketTag?) =
        getChannelInt(DataChannelEndPoint.fromStream(received, acceptTag), acceptTag)

    override fun getChannelBytes(received: DataChannelBytesReceived?, acceptTag: PacketTag?) =
        getChannelInt(DataChannelEndPoint.fromBytes(received, acceptTag), acceptTag)

    override fun onNewHost(onNewHost: NewHostInDataChannel) {
        newHostNotifiers.add(onNewHost)
    }

    override fun close() {
        logger.writeLog(LogImportance.Info, logCat, "Канал $name закрывается")
    }

    /**
     * Принимает пакет для распространения среди подписанных клиентов
     * Принимаются только пакеты с message.to == HostID.Local
     */
    fun receivedFromRelay(message: ChannelEnvelope) {

        if (!message.targetIsLocal)
            return

        dBus.sendMessage(message)
    }

    private fun busDispatch(channel: IChannelEndPoint, packet: Message) {

        if (packet !is ChannelEnvelope)    // Принимаются к отправке только RelayEnvelope
            return

        if (!packet.from.isLocal)  // Принимаются к отправке только от HostID.Local
            return

        if (packet.targetIsLocal)   // С целью HostID.Local на маршрутизацию не отправляются
            return

        sender(packet)
    }

    fun newHostNotify(newHostID: HostID): Unit = newHostNotifiers.forEach { it(newHostID) }

    fun addAcceptTag(tag: PacketTag) {
        acceptableTags[tag] = 0
    }

    val acceptTags
        get() = acceptableTags.keys().toList().toTypedArray()

    fun updateHostEndpointInfo(from: HostID, endPoint: EndPointInfo?, pid: Int = 0): Boolean {
        if (endPoint != null && endPoint.channelName != name)
            return false

        if (endPoint != null && from != endPoint.target.hostID)
            return false

        if (endPoint == null) {

            if (remotes.remove(from) != null)
                logger.writeLog(LogImportance.Info, logCat, "Хост $from вышел из канала $name (#$pid)")

            return false
        }

        val h = remotes.containsKey(from)

        val ep = remotes.getOrPut(from) { EndPoint(endPoint.target, endPoint.acceptTags) }
        ep.acceptTags = endPoint.acceptTags

        if (h)
            return false

        logger.writeLog(LogImportance.Info, logCat, "Хост $from вошел в канал $name (#$pid)")

        newHostNotify(from)

        return true
    }
}