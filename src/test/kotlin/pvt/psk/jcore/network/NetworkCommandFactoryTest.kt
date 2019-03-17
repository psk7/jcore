@file:Suppress("UNUSED_VARIABLE", "UNCHECKED_CAST", "TestFunctionName")

package pvt.psk.jcore.network

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.network.commands.*
import pvt.psk.jcore.utils.*
import java.net.*
import java.util.*

class NetworkCommandFactoryTest {

    @Test
    fun adjustTargetHost() {
        val r = Router()
        val remote = HostID(UUID.randomUUID(), "Remote")
        val self = HostID(UUID.randomUUID(), "Self")

        val pp = NetworkCommandFactory(self, "TestDomain", r)

        fun D(from: HostID, to: HostID): PeerCommand? {
            val rr = pp.adjustTargetHost(from, to)
            return if (rr.first)
                DiscoveryCommand(from, rr.second)
            else
                null
        }

        // remote отсылал пакет с отправителя Local -> ALL. Формируется null
        var c = D(HostID.Local, HostID.All)
        assertNull(c)

        // remote отсылал пакет со своего HostID -> ALL. Формируется remote
        c = D(remote, HostID.All)
        assertEquals(remote, c?.fromHost)
        assertEquals(HostID.Local, c?.toHost)

        assertNull(D(HostID.Local, HostID.Network))
        assertNull(D(remote, HostID.Network))

        // remote отсылал пакет с отправителя Local -> self. Формируется null
        c = D(HostID.Local, self)
        assertNull(c)

        // remote отсылал пакет со своего HostID -> self. Формируется remote
        // приемник Local
        c = D(remote, self)
        assertEquals(remote, c?.fromHost)
        assertEquals(HostID.Local, c?.toHost)
    }

    private fun prep(command: CommandID, domain: String, from: HostID, to: HostID): BinaryWriter {

        val wr = BinaryWriter()

        wr.write(command.ordinal.toByte())
        wr.write(domain)
        wr.write(from)
        wr.write(to)

        return wr
    }

    private fun <T> checkCommand(command: CommandID) where T : PeerCommand {
        val r = Router()
        val remote = HostID(UUID.randomUUID(), "Remote")
        val remote2 = HostID(UUID.randomUUID(), "Remote2")
        val self = HostID(UUID.randomUUID(), "Self")

        val l = mutableListOf<Message>()

        val c = r.getChannel({ _, p ->
                                 if (p !is HostAdmResolved)
                                     l.add(p)
                             })

        var unused = NetworkCommandFactory(self, "TestDomain", r)

        // Проверка получения пакета Discovery от удаленного хоста этому
        var w = prep(command, "TestDomain", remote, self)
        c.sendMessage(IncomingSerializedCommand(w.toArray(), InetSocketAddress(Inet6Address.getLoopbackAddress(), 1234)))

        assertEquals(1, l.size)
        assertEquals(remote, (l[0] as T).fromHost)
        assertEquals(HostID.Local, (l[0] as T).toHost)

        // Проверка получения пакета Discovery от удаленного хоста другому удаленному хосту
        l.clear()
        w = prep(command, "TestDomain", remote, remote2)
        c.sendMessage(IncomingSerializedCommand(w.toArray(), InetSocketAddress(Inet6Address.getLoopbackAddress(), 1234)))

        assertEquals(0, l.size)

        // Проверка получения пакета Discovery от локального хоста (в приходящих пакетах не должно быть локального отправителя)
        l.clear()
        w = prep(command, "TestDomain", HostID.Local, self)
        c.sendMessage(IncomingSerializedCommand(w.toArray(), InetSocketAddress(Inet6Address.getLoopbackAddress(), 1234)))

        assertEquals(0, l.size)

        w = prep(command, "TestDomain", HostID.Local, remote)
        c.sendMessage(IncomingSerializedCommand(w.toArray(), InetSocketAddress(Inet6Address.getLoopbackAddress(), 1234)))

        assertEquals(0, l.size)

        // Проверка получения пакета Discovery для локального хоста (в приходящих пакетах не должно быть локального получателя)
        l.clear()
        w = prep(command, "TestDomain", self, HostID.Local)
        c.sendMessage(IncomingSerializedCommand(w.toArray(), InetSocketAddress(Inet6Address.getLoopbackAddress(), 1234)))

        assertEquals(0, l.size)

        w = prep(command, "TestDomain", remote, HostID.Local)
        c.sendMessage(IncomingSerializedCommand(w.toArray(), InetSocketAddress(Inet6Address.getLoopbackAddress(), 1234)))

        assertEquals(0, l.size)

        // Проверка получения пакета Discovery от локального хоста и для локального хоста
        l.clear()
        w = prep(command, "TestDomain", HostID.Local, HostID.Local)
        c.sendMessage(IncomingSerializedCommand(w.toArray(), InetSocketAddress(Inet6Address.getLoopbackAddress(), 1234)))

        assertEquals(0, l.size)
    }

    @Test
    fun deserializeDiscovery() {
        checkCommand<DiscoveryCommand>(CommandID.Discovery)
    }

    @Test
    fun deserializePing() {
        val r = Router()
        val remote = HostID(UUID.randomUUID(), "Remote")
        val self = HostID(UUID.randomUUID(), "Self")

        val l = mutableListOf<Message>()

        val c = r.getChannel({ _, p ->
                                 if (p !is HostAdmResolved)
                                     l.add(p)
                             })

        var unused = NetworkCommandFactory(self, "TestDomain", r)
        val tk = AckToken()

        val ipe = InetSocketAddress(Inet6Address.getLoopbackAddress(), 1234)

        val w = prep(CommandID.Ping, "TestDomain", remote, self)
        w.write(tk)
        c.sendMessage(IncomingSerializedCommand(w.toArray(), ipe))

        assertEquals(1, l.size)
        assertEquals(remote, (l[0] as NetworkPingCommand).fromHost)
        assertEquals(HostID.Local, (l[0] as NetworkPingCommand).toHost)
        assertEquals(ipe, (l[0] as NetworkPingCommand).from)
    }

    @Test
    fun deserializePingReply() {
        val r = Router()
        val remote = HostID(UUID.randomUUID(), "Remote")
        val self = HostID(UUID.randomUUID(), "Self")

        val l = mutableListOf<Message>()

        val c = r.getChannel({ _, p ->
                                 if (p !is HostAdmResolved)
                                     l.add(p)
                             })

        var unused = NetworkCommandFactory(self, "TestDomain", r)
        val tk = AckToken()

        val ipe = InetSocketAddress(Inet6Address.getLoopbackAddress(), 1234)

        val w = prep(CommandID.PingReply, "TestDomain", remote, self)
        w.write(tk)
        c.sendMessage(IncomingSerializedCommand(w.toArray(), ipe))

        assertEquals(1, l.size)
        assertEquals(remote, (l[0] as NetworkPingReplyCommand).fromHost)
        assertEquals(HostID.Local, (l[0] as NetworkPingReplyCommand).toHost)
        assertEquals(ipe, (l[0] as NetworkPingReplyCommand).from)
    }

    @Test
    fun serializeDiscovery() {
        val r = Router()
        val remote = HostID(UUID.randomUUID(), "Remote")
        val remote2 = HostID(UUID.randomUUID(), "Remote2")
        val self = HostID(UUID.randomUUID(), "Self")

        val l = mutableListOf<Message>()

        val c = r.getChannel({ _, p ->
                                 if (p !is HostAdmResolved)
                                     l.add(p)
                             })

        var unused = NetworkCommandFactory(self, "TestDomain", r)

        // Пакеты самому себе не отправляются
        c.sendMessage(DiscoveryCommand(HostID.Local, self))
        assertEquals(0, l.size)

        // Пакеты самому себе не отправляются
        c.sendMessage(DiscoveryCommand(HostID.Local, HostID.Local))
        assertEquals(0, l.size)

        // Пакеты не от себя не отправляются
        c.sendMessage(DiscoveryCommand(remote, remote2))
        assertEquals(0, l.size)

        c.sendMessage(DiscoveryCommand(self, self))
        assertEquals(0, l.size)

        c.sendMessage(DiscoveryCommand(HostID.Local, remote))
        assertEquals(1, l.size)
        assertEquals(remote, (l[0] as OutgoingSerializedCommand).toHost)
        assertNull((l[0] as OutgoingSerializedCommand).toEndPoint)

        l.clear()
        c.sendMessage(DiscoveryCommand(self, remote))
        assertEquals(1, l.size)
        assertEquals(remote, (l[0] as OutgoingSerializedCommand).toHost)
        assertNull((l[0] as OutgoingSerializedCommand).toEndPoint)

        // Пакет всем
        l.clear()
        c.sendMessage(DiscoveryCommand(HostID.Local, HostID.All))
        assertEquals(1, l.size)
        assertEquals(HostID.All, (l[0] as OutgoingSerializedCommand).toHost)
        assertNull((l[0] as OutgoingSerializedCommand).toEndPoint)
    }

    @Test
    fun serializePingReply(){
        val r = Router()
        val remote = HostID(UUID.randomUUID(), "Remote")
        val remote2 = HostID(UUID.randomUUID(), "Remote2")
        val self = HostID(UUID.randomUUID(), "Self")

        val l = mutableListOf<Message>()

        val c = r.getChannel({ _, p ->
                                 if (p !is HostAdmResolved)
                                     l.add(p)
                             })

        var unused = NetworkCommandFactory(self, "TestDomain", r)

        val tk = AckToken()
        val ipe = InetSocketAddress(Inet6Address.getLoopbackAddress(), 1234)

        // Пакеты самому себе не отправляются
        c.sendMessage(NetworkPingReplyCommand(HostID.Local, self, tk, ipe))
        assertEquals(0, l.size)

        // Пакеты самому себе не отправляются
        c.sendMessage(NetworkPingReplyCommand(HostID.Local, HostID.Local, tk, ipe))
        assertEquals(0, l.size)

        // Пакеты не от себя не отправляются
        c.sendMessage(NetworkPingReplyCommand(remote, remote2, tk, ipe))
        assertEquals(0, l.size)

        c.sendMessage(NetworkPingReplyCommand(self, self, tk, ipe))
        assertEquals(0, l.size)

        c.sendMessage(NetworkPingReplyCommand(HostID.Local, remote, tk, ipe))
        assertEquals(1, l.size)
        assertEquals(remote, (l[0] as OutgoingSerializedCommand).toHost)
        assertEquals(ipe, (l[0] as OutgoingSerializedCommand).toEndPoint)

        l.clear()
        c.sendMessage(NetworkPingReplyCommand(self, remote, tk, ipe))
        assertEquals(1, l.size)
        assertEquals(remote, (l[0] as OutgoingSerializedCommand).toHost)
        assertEquals(ipe, (l[0] as OutgoingSerializedCommand).toEndPoint)

        // Пакет всем
        l.clear()
        c.sendMessage(NetworkPingReplyCommand(HostID.Local, HostID.All, tk, ipe))
        assertEquals(1, l.size)
        assertEquals(HostID.All, (l[0] as OutgoingSerializedCommand).toHost)
        assertEquals(ipe, (l[0] as OutgoingSerializedCommand).toEndPoint)
    }
}