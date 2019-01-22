package remoting

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.remoting.*

private interface IT
{
    @MethodIdVal(123)
    fun M1()
}

class MethodIDTest {

    @Test
    fun testId()
    {
        val mid = getMethodID(IT::class.java.getMethod("M1"))

        assertEquals(123, mid.ID)
    }
}