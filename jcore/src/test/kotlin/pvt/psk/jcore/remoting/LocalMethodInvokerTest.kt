package pvt.psk.jcore.remoting

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.remoting.*

private interface I1 {

    @MethodIdVal(10)
    fun MAdd(op1: Int, op2: Int): Double
}

private class M : I1 {
    override fun MAdd(op1: Int, op2: Int): Double = (op1 + op2).toDouble()
}

class LocalMethodInvokerTest {
    @Test
    fun invoke() {
        val o = M();
        val lmi = LocalMethodInvoker(o)

        val res = lmi.invoke(MethodID(10, "MAdd"), Arguments(arrayOf(10, 20)))

        assertEquals(30.toDouble(), res)
    }
}