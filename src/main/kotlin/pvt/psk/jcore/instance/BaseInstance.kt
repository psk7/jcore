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

    protected val selfHostID: HostID
    protected val ControlBus: Router
    protected var PeerProto: PeerProtocol? = null
    protected var ComSocket: PeerCommandSocket? = null

    val cancellationSource = CancellationTokenSource()
    val CancellationToken = cancellationSource.token

    private val chanLock = ReentrantReadWriteLock()
    private val channels = Hashtable<String, BaseChannel>()

    init {
        selfHostID = HostID(UUID.randomUUID(), Name)

        ControlBus = Router()

    }

    open fun init() {
        Log?.writeLog(LogImportance.Info, logCat, "Создан экземпляр $selfHostID")

        PeerProto = createPeerProtocol(ControlBus, DomainName)

        ComSocket = createPeerCommandSocket()

        PeerProto!!.discovery()
    }

    fun joinChannel(channelName: String): BaseChannel =
            chanLock.write {
                channels.getOrPut(channelName) { createChannel(channelName, Router()) }
            }.also {
                PeerProto!!.sendHostInfo(HostID.All)
            }

    open fun close() {

        PeerProto?.leave()

        cancellationSource.cancel()
    }

    protected abstract fun createPeerProtocol(Control: IChannel, Domain: String): PeerProtocol
    protected abstract fun createPeerCommandSocket(): PeerCommandSocket
    protected abstract fun createChannel(channelName: String, channelRouter: Router): BaseChannel

    abstract fun createPollCommand(): PollCommand

    fun getHosts(): Array<HostID> = PeerProto!!.getHosts()
}