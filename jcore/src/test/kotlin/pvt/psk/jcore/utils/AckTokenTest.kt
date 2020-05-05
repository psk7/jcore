package pvt.psk.jcore.utils

import org.junit.jupiter.api.*
import pvt.psk.jcore.utils.*

class AckTokenTest
{
    @Test
    fun TestIDsSequence()
    {
        val t1 = AckToken()
        val t2 = AckToken()

        Assertions.assertNotEquals(t1.ID, t2.ID)
    }
}