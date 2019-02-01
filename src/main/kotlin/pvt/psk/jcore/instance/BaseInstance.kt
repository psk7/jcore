package pvt.psk.jcore.instance

import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.util.*

abstract class BaseInstance(Name: String, val DomainName: String, val AdmPort: Int, val Log: Logger?) {

    val logCat: String = "Peer"

    val HostID: HostID
    val ControlBus: Router
    val PeerProto: PeerProtocol
    val ComSocket: PeerCommandSocket
    val CancellationToken: CancellationToken

    init {
        CancellationToken = pvt.psk.jcore.utils.CancellationToken.None

        HostID = HostID(UUID.randomUUID(), Name)

        ControlBus = Router()

        PeerProto = createPeerProtocol(ControlBus.getChannel(), DomainName)

        ComSocket = createPeerCommandSocket()

        PeerProto.discovery()
    }

    open fun init() {
        Log?.writeLog(LogImportance.Info, logCat, "Создан экземпляр {$HostID}");
    }

    protected abstract fun createPeerProtocol(Control: IChannel, Domain: String): PeerProtocol
    protected abstract fun createPeerCommandSocket(): PeerCommandSocket

}