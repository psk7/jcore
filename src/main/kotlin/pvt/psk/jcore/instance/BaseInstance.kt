package pvt.psk.jcore.instance

import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.util.*
import java.util.concurrent.locks.*
import kotlin.concurrent.*

abstract class BaseInstance(Name: String, val DomainName: String, val AdmPort: Int, val Log: Logger?) {

    val logCat: String = "Peer"

    protected val selfHostID = HostID(UUID.randomUUID(), Name)
    protected val controlBus = Router()

    protected var PeerProto: PeerProtocol? = null
    protected var ComSocket: PeerCommandSocket? = null

    val cancellationSource = CancellationTokenSource()
    val CancellationToken = cancellationSource.token

    private val chanLock = ReentrantReadWriteLock()
    private val channels = Hashtable<String, BaseChannel>()

    open fun init() {
        Log?.writeLog(LogImportance.Info, logCat, "Создан экземпляр $selfHostID")

        PeerProto = createPeerProtocol(controlBus, DomainName)

        ComSocket = createPeerCommandSocket()
    }

    fun joinChannel(channelName: String): BaseChannel =
            chanLock.write {
                channels.getOrPut(channelName) { createChannel(channelName, Router()) }
            }.also {
                PeerProto!!.sendHostInfo(HostID.All)
            }

    fun leaveAllChannels() {
        chanLock.write {
            channels.values.forEach { it.close() }

            channels.clear()
        }

        PeerProto?.sendHostInfo(HostID.All)
    }

    open fun close() {
        cancellationSource.cancel()
    }

    protected abstract fun createPeerProtocol(Control: IChannel, Domain: String): PeerProtocol
    protected abstract fun createPeerCommandSocket(): PeerCommandSocket
    protected abstract fun createChannel(channelName: String, channelRouter: Router): BaseChannel

    abstract fun createPollCommand(): PollCommand

    fun getHosts(): Array<HostID> = PeerProto!!.getHosts()
}