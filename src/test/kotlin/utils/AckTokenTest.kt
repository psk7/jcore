package utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import pvt.psk.jcore.utils.*

class AckTokenTest
{
    @Test
    fun TestIDsSequence()
    {
        var t1 = AckToken.New();
        var t2 = AckToken.New();

        Assertions.assertNotEquals(t1.ID, t2.ID)
    }
}