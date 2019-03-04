@file:Suppress("UNUSED_VARIABLE")

package channel

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.util.*

class BaseChannelTest {

    private class Sender : ISender {
        override fun send(Packet: DataPacket, Target: EndPoint) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    private class EP(Data: IChannel, Sender: ISender, TargetHost: HostID) : EndPoint(Data, Sender, TargetHost) {
    }

    private class PC(val _fromHost: HostID, ToHost: HostID) : PollCommand(_fromHost, ToHost) {

        override fun createHostInfoCommand(SeqID: Int, FromHost: HostID, ToHost: HostID): HostInfoCommand {
            return HostInfoCommand(2, _fromHost, arrayOf(), HostID.Local)
        }
    }

    private class PP(val Self: HostID, Domain: String, ControlChannel: IChannel, Logger: Logger?) :
        PeerProtocol(Self, Domain, ControlChannel, Logger) {

        override fun processHostInfoCommand(Command: HostInfoCommand) {}

        override fun createPollCommand(): PollCommand = PC(Self, HostID.Local)

        //override fun Create(CommandID: CommandID, From: HostID, To: HostID): PeerCommand = TODO()
        //override fun Create(BinaryReader Reader): PeerCommand => throw new NotImplementedException();

        override fun serialize(Command: PeerCommand, Writer: BinaryWriter) = TODO()

        override fun create(Reader: BinaryReader): PeerCommand? {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    private class BC(Name: String, Peer: PeerProtocol, ControlBus: IChannel, Data: Router, Logger: Logger?, CancellationToken: CancellationToken) :
        BaseChannel(Name, Peer, ControlBus, Data, Logger, CancellationToken) {

        init {
            initComplete()
        }

        override fun processPollCommand(command: PollCommand) {}

        override fun onHostRemove(host: EndPoint) {
            throw Exception()
        }

        override fun onHostUpdate(command: HostInfoCommand, endPointInfo: EndPointInfo, endPoint: EndPoint) {
            throw Exception()
        }

        override fun onHostCreate(Command: HostInfoCommand, EndPointInfo: EndPointInfo): Deferred<EndPoint> {
            return CompletableDeferred(EP(Data, Sender(), EndPointInfo.target))
        }
    }

    @Test
    fun NewHost() {
        val cr = Router()
        val dr = Router()

        val self = HostID(UUID.randomUUID(), "Self")
        val rh = HostID(UUID.randomUUID(), "Remote")

        val pp = PP(self, "Domain", cr, null)
        val bc = BC("Channel", pp, cr, dr, null, CancellationToken.None)

        val cc = cr.getChannel()

        val hi = HostInfoCommand(1, rh, arrayOf(EndPointInfo(rh, "Channel", false)), HostID.Local)

        var newhost = HostID.All

        dr.getChannel({ _, p ->
                          when (p) {
                              is NewHostInChannel -> newhost = p.FromHost
                          }
                      })

        cc.sendHostInfo(hi)

        assertEquals(rh, newhost)
    }
}