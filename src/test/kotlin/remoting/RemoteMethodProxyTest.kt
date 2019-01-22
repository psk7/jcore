package remoting

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.remoting.*

internal interface A {
    val x: Int

    fun set(x: Int)
}

internal class Transport : IMethodInvoker{}

class RemoteMethodProxyTest {

    @Test
    fun create() {
        val pf = RemoteMethodProxyFactory()

        val oa = pf.create(Transport(), A::class.java) as A

        assertNotNull(oa)
    }
}