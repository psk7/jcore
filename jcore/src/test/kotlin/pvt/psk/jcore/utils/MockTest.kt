package pvt.psk.jcore.utils

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.*
import org.koin.core.logger.*
import org.koin.test.*
import org.koin.test.mock.*
import org.mockito.*
import org.mockito.BDDMockito.*
import utils.*

class MockTest : KoinTest {

    interface IHello {
        fun hello(): String
    }

    @JvmField
    @RegisterExtension
    val koinTestRule = KoinTestExtension.create {
        printLogger(Level.DEBUG)
        //modules(helloModule)
    }

    @JvmField
    @RegisterExtension
    val mockProvider = MockProviderExtension.create { clazz ->
        Mockito.mock(clazz.java)
    }

    @Test
    fun `mock test`() {
        val service = declareMock<IHello> {
            given(hello()).willReturn("Hello Mock")
        }

        val z = service.hello()

        assertEquals("Hello Mock", z)

        Mockito.verify(service, times(1)).hello()
    }
}