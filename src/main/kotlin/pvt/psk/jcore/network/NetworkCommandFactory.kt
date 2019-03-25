package pvt.psk.jcore.network

import kotlinx.atomicfu.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.network.commands.*
import pvt.psk.jcore.utils.*
import java.net.*

class NetworkCommandFactory(private val self: HostID,
                            private val domain: String,
                            Bus: IChannel) {

    private val ep: IChannelEndPoint
    private val curseqid = atomic(1)

    init {
        ep = Bus.getChannel(::onReceived)
    }

    private fun onReceived(@Suppress("UNUSED_PARAMETER") channel: IChannelEndPoint, packet: Message) {
        when (packet) {
            is IncomingSerializedCommand -> deserialize(packet)
            is PeerCommand -> serialize(packet)
        }
    }

    /**
     * Упаковка команды в пакет OutgoingSerializeCommand и отправка его в общую шину
     */
    private fun serialize(command: PeerCommand) {
        val sc = create(command)

        if (sc != null)
            ep.sendMessage(sc)
    }

    /**
     * Распаковка команды из пакета IncomingSerializedCommand и отправка ее в общую шину
     */
    private fun deserialize(command: IncomingSerializedCommand) {
        val cmd = create(BinaryReader(command.data), command.from) ?: return

        ep.sendMessage(HostAdmResolved(cmd.fromHost, command.from))

        ep.sendMessage(cmd)
    }

    /**
     * Упаковка команды
     */
    private fun create(command: PeerCommand): OutgoingSerializedCommand? {

        if (command.toHost == self || command.toHost == HostID.Local)
            return null // Пакеты самому себе не отправляются

        if (command.fromHost != HostID.Local && command.fromHost != self)
            return null // Пакеты не от самого себя не отправляются

        val wr = BinaryWriter()

        var fh = command.fromHost
        if (fh == HostID.Local)
            fh = self

        var th = command.toHost
        if (th == HostID.Network)
            th = HostID.All

        wr.write(command.CommandID.ordinal.toByte())
        wr.write(domain)
        wr.write(curseqid.incrementAndGet())
        wr.write(fh)
        wr.write(th)

        when (command) {
            is HostInfoCommand -> command.serialize(wr)
            is PingCommand -> wr.write(command.token)
            is PingReplyCommand -> wr.write(command.token)
        }

        val ipe = (command as? NetworkPingReplyCommand)?.from

        return OutgoingSerializedCommand(wr.toArray(), th, ipe, command is PingCommand)
    }

    /**
     * Распаковка команды
     */
    private fun create(reader: BinaryReader, from: InetSocketAddress): PeerCommand? {
        val id = CommandID.values()[reader.readByte().toInt()]

        val dom = reader.readString()

        if (dom != domain)
            return null

        val seq = reader.readInt32()

        val fromHost = HostID(reader)
        var toHost = HostID(reader)

        val r = adjustTargetHost(fromHost, toHost)

        if (!r.first)
            return null

        toHost = r.second

        val c: PeerCommand = when (id) {
            CommandID.Discovery -> DiscoveryCommand(fromHost, toHost)
            CommandID.HostInfo -> HostInfoCommand(fromHost, reader.deserialize(fromHost), toHost)
            CommandID.Ping -> NetworkPingCommand(fromHost, toHost, AckToken(reader), from)
            CommandID.PingReply -> NetworkPingReplyCommand(fromHost, toHost, AckToken(reader), from)
            else -> throw Exception()
        }

        c.sequence = seq

        return c
    }

    fun adjustTargetHost(from: HostID, to: HostID): Pair<Boolean, HostID> {
        if (from == self) // Свои пакеты отбрасываются
            return Pair(false, to)

        if (to != HostID.All && to != self)
            return Pair(false, to)

        if (from.isLocal)
            return Pair(false, to)

        if (to == HostID.All || to == self)
            return Pair(true, HostID.Local)

        return Pair(true, to)
    }
}