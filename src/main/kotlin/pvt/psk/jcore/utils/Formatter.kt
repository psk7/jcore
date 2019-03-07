package pvt.psk.jcore.utils

import java.lang.Exception

class Formatter {

    private enum class Types {
        Null,
        Char,
        Boolean,
        String,
        Byte,
        SByte,
        Int32,
        UInt32,
        Int16,
        UInt16,
        Int64,
        UInt64,
        Single,
        Double,
        Guid,
        BytesArray,
        StringArray,
        ObjectArray,
        ObjectArray_Utils_ISerializable,
        Utils_ISerializable,
        Utils_ISerializableWithID,
        BinaryFormatter,
        Arguments
    }

    fun serialize(wr: BinaryWriter, obj: Any?) {
        when (obj) {
            null -> wr.write(Types.Null.ordinal.toByte())

            is ByteArray -> {
                wr.write(Types.BytesArray.ordinal.toByte())
                wr.write(obj.size)
                wr.write(obj)
            }

            else -> throw Exception("bad value")
        }
    }

    fun deserialize(reader: BinaryReader): Any? {

        val id = Types.values()[reader.readByte().toInt()]

        return when (id) {
            Types.Null -> null

            Types.BytesArray ->{
                reader.readBytes(reader.readInt32())
            }

            else -> TODO("BAD ARG $id")
        }
    }
}