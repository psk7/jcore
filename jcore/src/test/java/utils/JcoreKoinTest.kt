package utils

import org.junit.jupiter.api.extension.*
import org.koin.test.*
import pvt.psk.jcore.*

open class JcoreKoinTest : KoinTest {

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(jcoreModule)
    }
}