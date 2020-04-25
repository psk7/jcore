package pvt.psk.jcore.network

import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.*
import org.koin.dsl.*
import org.koin.test.*
import pvt.psk.jcore.logger.*
import utils.*
import java.net.*

class SafeUdpClientTest : JcoreKoinTest() {

    /**
     * Тест для отладки в студии без проверок
     */
    @Test
    @Timeout(2)
    fun cancellation() {

        @Suppress("UNUSED_VARIABLE")
        val udp = SafeUdpClient(InetSocketAddress(InetAddress.getByName("::"), 0)) { _, _ -> }

        udp.close()
    }
}