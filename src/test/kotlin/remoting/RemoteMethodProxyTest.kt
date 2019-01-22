package remoting

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.remoting.*

internal interface A {
    val x: Int

    fun set(x: Int)
}

internal class Transport : IMethodInvoker {
    override fun InvokeAsync(MethodID: MethodID, Arguments: Arguments): Deferred<Any?> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun Invoke(MethodID: MethodID, Arguments: Arguments): Any? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class RemoteMethodProxyTest {

    @Test
    fun create() {
        val pf = RemoteMethodProxyFactory()

        val oa = pf.create(Transport(), A::class.java) as A

        assertNotNull(oa)
    }
}