package remoting

import org.junit.jupiter.api.*
import pvt.psk.jcore.remoting.*
import java.lang.reflect.*

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

        oa.set(5)
        oa.x
    }
}