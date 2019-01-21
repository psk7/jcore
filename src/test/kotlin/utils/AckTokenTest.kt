package utils

import org.junit.jupiter.api.*
import pvt.psk.jcore.utils.*

class AckTokenTest
{
    @Test
    fun TestIDsSequence()
    {
        var t1 = AckToken();
        var t2 = AckToken();

        Assertions.assertNotEquals(t1.ID, t2.ID)
    }
}