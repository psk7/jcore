package pvt.psk.jcore.network

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.network.commands.*
import pvt.psk.jcore.utils.*
import java.net.*
import java.util.*
import kotlin.test.*

class NetworkCommandSocketTest {

    @Test
    fun resolve() {
        val r = Router()

        val dir = IPAddressDirectory()

        @Suppress("UNUSED_VARIABLE")
        val ncs = NetworkCommandSocket(r, 0, TestLogger(), dir, CancellationToken.None)

        val l = MsgQueue()

        val c = r.getChannel({ _, p -> l.add(p) })

        val remote = HostID(UUID.randomUUID(), "Remote")

        val udp = DatagramSocket(InetSocketAddress(InetAddress.getByName("::"), 0))

        // Попытка отправить сообщение на неизвестных хост remote
        // Должен прийти пакет Ping для разрешения
        c.sendMessage(OutgoingSerializedCommand(byteArrayOf(0, 1, 2, 3, 4), remote, null))

        val npc = l.tryGetMessage<NetworkPingCommand>()

        assertNull(npc.from)
        assertEquals(HostID.Local, npc.fromHost)
        assertEquals(remote, npc.toHost)

        // Имитация успешного разрешения хоста remote
        npc.token.received(InetSocketAddress(Inet6Address.getLoopbackAddress(), (udp.localPort)))

        // Посылка должна быть отправлена на udp
        val dp = DatagramPacket(ByteArray(1024), 0, 1024)
        udp.receive(dp)

        assertEquals(5, dp.length)

        udp.close()
    }

    @Test
    fun hostAdmResolved() {
        // Проверка отработки команды HostAdmResolved
        val r = Router()

        val dir = IPAddressDirectory()
        @Suppress("UNUSED_VARIABLE")
        val ncs = NetworkCommandSocket(r, 0, TestLogger(), dir, CancellationToken.None)

        val remote = HostID(UUID.randomUUID(), "Remote")

        val ipe = InetSocketAddress(Inet6Address.getLoopbackAddress(), 7654)

        r.getChannel().sendMessage(HostAdmResolved(remote, ipe))

        // В IPAddressDirectory должна быть занесена информация о полученном адресе
        assertEquals(Inet6Address.getLoopbackAddress(), dir.resolve(remote))
    }

    @Test
    fun reset() {
        // Проверка отработки команды ResetNetwork
        val r = Router()

        val dir = IPAddressDirectory()
        @Suppress("UNUSED_VARIABLE")
        val ncs = NetworkCommandSocket(r, 0, TestLogger(), dir, CancellationToken.None)

        val remote = HostID(UUID.randomUUID(), "Remote")

        val l = MsgQueue()

        val ipe = InetSocketAddress(Inet6Address.getLoopbackAddress(), 7654)

        val c = r.getChannel({ _, p -> l.add(p) })
        c.sendMessage(HostAdmResolved(remote, ipe))

        // В IPAddressDirectory должна быть занесена информация о полученном адресе
        assertEquals(Inet6Address.getLoopbackAddress(), dir.resolve(remote))

        c.sendMessage(ResetNetworkCommand())

        assertNull(dir.resolve(remote))

        // Должна произойти попытка повторного разрешения известного ранее хоста remote
        assertFalse(l.isEmpty())
    }

    @Test
    fun reResolve() {
        // Проверка очистки из кеша информации о неудачном разрешении хоста
        val r = Router()

        val dir = IPAddressDirectory()
        val ncs = NetworkCommandSocket(r, 0, TestLogger(), dir, CancellationToken.None)

        // Время ожидания разрешения
        ncs.admResolveTimeout = 200

        val l = MsgQueue()

        val c = r.getChannel({ _, p -> l.add(p) })

        val remote = HostID(UUID.randomUUID(), "Remote")

        // Попытка отправить сообщение на неизвестных хост remote
        // Должен прийти пакет Ping для разрешения
        c.sendMessage(OutgoingSerializedCommand(byteArrayOf(0, 1, 2, 3, 4), remote, null))

        var cmd = l.tryGetMessage<NetworkPingCommand>()
        assertNull(cmd.from)
        assertEquals(HostID.Local, cmd.fromHost)
        assertEquals(remote, cmd.toHost)

        // Еще одна попытка отправить сообщение на неизвестных хост remote
        // На этот раз разрешения быть не должно
        c.sendMessage(OutgoingSerializedCommand(byteArrayOf(0, 1, 2, 3, 4), remote, null))

        assertTrue(l.isEmpty())

        runBlocking { delay(250) }

        // Должна быть еще одна попытка разрешения
        c.sendMessage(OutgoingSerializedCommand(byteArrayOf(0, 1, 2, 3, 4), remote, null))

        cmd = l.tryGetMessage()
        assertNull(cmd.from)
        assertEquals(HostID.Local, cmd.fromHost)
        assertEquals(remote, cmd.toHost)
    }

    @Test
    fun onRemoteAdmEndpointChanged() {
        // Проверка принятия изменившейся административной точки удаленного хоста

        val cr = Router()
        val dir = IPAddressDirectory()

        val ipe1 = InetSocketAddress(InetAddress.getByName("::3"), 12345)
        val ipe2 = InetSocketAddress(InetAddress.getByName("::3"), 12346)

        val ncs = NetworkCommandSocket(cr, 7654, TestLogger(), dir, CancellationToken.None)

        val mq = MsgQueue()

        val cc = cr.getChannel({ _, p -> mq.put(p) })

        val hid = HostID(UUID.randomUUID(), "Remote")

        cc.sendMessage(HostAdmResolved(hid, ipe1))
        assertEquals(ipe1, ncs.admEndPoints[hid])

        // Адрес должен разрешаться в новую конечную точку ipe2

        cc.sendMessage(HostAdmResolved(hid, ipe2))
        assertEquals(ipe2, ncs.admEndPoints[hid])
    }
}