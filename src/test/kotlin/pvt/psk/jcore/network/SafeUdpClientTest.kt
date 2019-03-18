package pvt.psk.jcore.network

import io.ktor.util.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import pvt.psk.jcore.utils.*
import java.net.*

class SafeUdpClientTest {

    /**
     * Тест для отладки в студии без проверок
     */
    @Test
    fun cancellation() {

        val ct = CancellationTokenSource()

        @Suppress("UNUSED_VARIABLE")
        val udp = SafeUdpClient(InetSocketAddress(InetAddress.getByName("::"), 0), ct.token, false) { _, _ -> }

        ct.cancel()
    }
}