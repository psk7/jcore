package pvt.psk.jcore.network

import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*

class NetworkPeerProtocol(selfHostID: HostID, domain: String, controlChannel: IChannel, logger: Logger?) :
    PeerProtocol(selfHostID, domain, controlChannel, logger) {

    override fun serialize(Command: PeerCommand, Writer: BinaryWriter) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createPollCommand(): PollCommand {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun ProcessHostInfoCommand(Command: HostInfoCommand) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}