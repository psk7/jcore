@file:Suppress("UNUSED_PARAMETER", "unused", "FunctionName")

package pvt.psk.jcore.remoting

import pvt.psk.jcore.utils.*
import java.lang.reflect.*

fun getMethodID(Method: Method): MethodID {

    val id = Method.getAnnotation(MethodIdVal::class.java)

    return MethodID(id.ID, Method.name)
}

class MethodID(val ID: Int, val Name: String) {

    constructor(Reader: BinaryReader) : this(Reader.readInt32(), "")

    fun Serialize(Writer: BinaryWriter) {
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MethodID

        if (ID != other.ID) return false

        return true
    }

    override fun hashCode(): Int = ID

}

@Target(AnnotationTarget.FUNCTION)
annotation class MethodIdVal(val ID: Int)