package pvt.psk.jcore.network

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.network.*
import pvt.psk.jcore.utils.*
import java.net.*
import java.nio.*
import java.nio.channels.*
import java.util.concurrent.*

class SafeUdpClientTest {

    @Test
    fun test() = runBlocking {
        withTimeout(1000) {

            val sa = InetSocketAddress("::", 4567)

            assertFalse(sa.isUnresolved)

            val l = ConcurrentLinkedQueue<ByteArray>()

            val c = SafeUdpClient(sa, CancellationToken.None) { dd, _ -> l += dd }

            val d = DatagramChannel.open()
            d.bind(InetSocketAddress("::", 0))

            runBlocking {

                for (i in 1..5) {
                    d.send(ByteBuffer.wrap(byteArrayOf(i.toByte())), InetSocketAddress("::1", 4567))

                    if (i == 2)
                        d.send(ByteBuffer.wrap(byteArrayOf(i.toByte())), InetSocketAddress("::1", 4567))
                    //delay(1)
                }

                while (l.size < 5)
                    delay(1)
            }

            for (bytes in l) {
                print(bytes[0])
            }

            println()
        }
    }

    @Test
    fun two() {

        val c1 = SafeUdpClient(InetSocketAddress("::", 0), CancellationToken.None) { d, a ->
            println("$d from $a in c1")
        }
        val c2 = SafeUdpClient(InetSocketAddress("::", 0), CancellationToken.None) { d, a ->
            println("$d from $a in c2")
        }

        val p1 = c1.localEndPoint.port
        val p2 = c2.localEndPoint.port

        for (i in 1..5) {
            c1.send(byteArrayOf(i.toByte()), InetSocketAddress("::1", p2))
            c2.send(byteArrayOf(i.toByte()), InetSocketAddress("::1", p1 + 10))
        }

        runBlocking { delay(500) }
    }

    @Test
    fun reuse() {
        val dc1 = DatagramChannel.open().bind(InetSocketAddress("::", 0))
        val p = (dc1.localAddress as InetSocketAddress).port

        assertThrows<BindException> {
            DatagramChannel.open().bind(InetSocketAddress("::", p))
        }
    }

    @Test
    fun unav() {
        val s = DatagramSocket(InetSocketAddress("0.0.0.0", 0))
        s.connect(InetSocketAddress("127.0.0.1", 34565))

        assert(s.isBound)

        s.send(DatagramPacket(byteArrayOf(1, 2, 3), 3, InetSocketAddress("127.0.0.1", 34565)))

        runBlocking { delay(100) }

        var p = DatagramPacket(ByteArray(1024), 1024)

        try {
            var n = s.receive(p)
        } catch (e: Exception) {
            val a = 1
        }


        runBlocking { delay(100) }
    }
}