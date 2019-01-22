package remoting

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.remoting.*
import java.lang.reflect.*

class MethodsMapTest {

    private interface IIntf {
        @MethodIdVal(10)
        fun M()
    }

    private class obj : IIntf {
        override fun M() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    @Test
    fun createMethodsMap() {
        val mm = MethodsMap(obj().javaClass)
        val m = HashMap<MethodID, Method>()

        mm.buildInterfaceMap(IIntf::class.java, m.javaClass, m)

        assertEquals(1, m.count())
        assertEquals(10, m.keys.toList()[0].ID)
    }
}