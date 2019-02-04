package pvt.psk.jcore.network

import kotlinx.atomicfu.*
import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*

class NetworkPeerProtocol(selfHostID: HostID, domain: String, controlChannel: IChannel, logger: Logger?) :
    PeerProtocol(selfHostID, domain, controlChannel, logger) {

    private val _curseqid = atomic(0)

    override fun create(Reader: BinaryReader): PeerCommand? {

        val id = CommandID.values()[Reader.readByte().toInt()]

        val dom = Reader.ReadString()

        if (dom != domain)
            return null

        val fromHost = HostID(Reader);
        val (keep, toHost) = adjustTargetHost(fromHost, HostID(Reader))

        return if (keep) when (id) {
            CommandID.Discovery -> DiscoveryCommand(fromHost, toHost)
            CommandID.Leave     -> LeaveCommand(fromHost, toHost)
            CommandID.HostInfo  -> HostInfoCommand(Reader.readInt32(), fromHost, Reader.deserialize(fromHost), toHost)
            CommandID.Ping      -> PingCommand(fromHost, toHost, AckToken(Reader))
            CommandID.PingReply -> PingReplyCommand(fromHost, toHost, AckToken(Reader))
            else                -> null
        } else null
    }

    override fun serialize(Command: PeerCommand, Writer: BinaryWriter) {

        Writer.write(Command.CommandID.ordinal.toByte())
        Writer.write(domain)

        var fh = Command.FromHost
        if (fh == HostID.Local)
            fh = selfHostID

        var th = Command.ToHost
        if (th == HostID.Network)
            th = HostID.All

        fh.serialize(Writer)
        th.serialize(Writer)

        when (Command) {
            is HostInfoCommand  -> {
                Writer.write(_curseqid.getAndIncrement())
                Command.serialize(Writer)
            }

            is PingCommand      -> Command.Token.toStream(Writer.baseStream)
            is PingReplyCommand -> Command.Token.toStream(Writer.baseStream)
        }
    }

    override fun createPollCommand(): PollCommand = NetworkPollCommand()

    override fun ProcessHostInfoCommand(Command: HostInfoCommand) {
    }
}