@file:Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")

package pvt.psk.jcore.channel

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.util.*

class BaseChannelTest {

    private class Sender : ISender {
        override fun send(packet: DataPacket, target: EndPoint) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    private class EP(Data: IChannel, Sender: ISender, TargetHost: HostID) : EndPoint(Data, Sender, TargetHost, null) {
    }

        private class PC(val _fromHost: HostID, ToHost: HostID) : PollCommand() {

        override fun createHostInfoCommand(FromHost: HostID, ToHost: HostID): HostInfoCommand {
            return HostInfoCommand(_fromHost, arrayOf(), HostID.Local)
        }
    }

        private class PP(val Self: HostID, ControlChannel: IChannel, Logger: Logger?) :
            PeerProtocol(Self, ControlChannel, Logger) {

        override fun createPollCommand(): PollCommand = PC(Self, HostID.Local)
    }

        class BC(Name: String, ControlBus: IChannel, Data: Router, Logger: Logger?, CancellationToken: CancellationToken) :
            BaseChannel(Name, ControlBus, Data, Logger, CancellationToken) {

        init {
            initComplete()
        }

        override fun processPollCommand(command: PollCommand) {}

            override fun onHostUpdate(endPointInfo: EndPointInfo, endPoint: EndPoint) {
            throw Exception()
        }

        override fun onHostCreate(endPointInfo: EndPointInfo): EndPoint {
            return EP(data, Sender(), endPointInfo.target)
        }
    }

    @Test
    fun NewHost() {
        val cr = Router()
        val dr = Router()

        val self = HostID(UUID.randomUUID(), "Self")
        val rh = HostID(UUID.randomUUID(), "Remote")

        val pp = PP(self, cr, null)
        val bc = BC("Channel", cr, dr, null, CancellationToken.None)

        val cc = cr.getChannel()

        val hi = HostInfoCommand(rh, arrayOf(EndPointInfo(rh, "Channel", arrayOf())), HostID.Local)

        var newhost = HostID.All

        dr.getChannel({ _, p ->
                          when (p) {
                              is NewHostInChannel -> newhost = p.fromHost
                          }
                      })

        cc.sendMessage(hi)

        assertEquals(rh, newhost)
    }
}