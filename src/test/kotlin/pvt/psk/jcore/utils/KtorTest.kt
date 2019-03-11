package pvt.psk.jcore.utils

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import java.net.*

class KtorTest {

    @Test
    fun create() {

        val sel = ActorSelectorManager(Dispatchers.IO)

        val sb = aSocket(sel).udp().bind(InetSocketAddress("::", 0))

        val p = (sb.localAddress as InetSocketAddress).port

        val s = DatagramSocket(InetSocketAddress("0.0.0.0", 0))

        s.send(DatagramPacket(byteArrayOf(0, 1, 2, 3), 4, InetSocketAddress("::1", p)))
        runBlocking { delay(10) }
        s.send(DatagramPacket(byteArrayOf(0, 1, 2, 3), 4, InetSocketAddress("::1", p)))
        runBlocking { delay(10) }
        s.send(DatagramPacket(byteArrayOf(0, 1, 2, 3), 4, InetSocketAddress("::1", p)))
        runBlocking { delay(10) }
        s.send(DatagramPacket(byteArrayOf(0, 1, 2, 3), 4, InetSocketAddress("::1", p)))

        runBlocking {
            delay(100)

            for (i in 1..4)
                println(sb.incoming.receive().packet.remaining)
            //rc.read { println(it.limit()) }
        }

    }
}