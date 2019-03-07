package pvt.psk.jcore.remoting

import java.lang.reflect.*

class MethodsMap(type: Class<*>) {

    private val mmap = HashMap<MethodID, Method>()

    init {

        for (ifs in type.interfaces)
            buildInterfaceMap(ifs, type, mmap)
    }

    fun buildInterfaceMap(Type: Class<*>, ObjectType: Class<*>, map: HashMap<MethodID, Method>) {
        val methods = Type.methods

        for (itm in methods) {

            val nit = ObjectType.methods.first { it.name == itm.name }
            //nit.trySetAccessible()

            map[getMethodID(itm)] = nit
        }

    }

    fun get(MethodID: MethodID): Method? = mmap.get(MethodID)
}