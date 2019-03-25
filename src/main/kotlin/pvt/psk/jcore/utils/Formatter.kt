@file:Suppress("EnumEntryName")

package pvt.psk.jcore.utils

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

    fun serialize(wr: BinaryWriter, obj: Array<String>?) {
        if (obj == null) {
            wr.write(Types.Null.ordinal.toByte())
            return
        }

        wr.write(Types.StringArray.ordinal.toByte())
        wr.write(obj.size.toShort())
        obj.forEach {
            val isn = true //it != null

            wr.write(isn)

            if (isn)
                wr.write(it)
        }
    }

    fun deserialize(reader: BinaryReader): Any? {

        val id = Types.values()[reader.readByte().toInt()]

        return when (id) {
            Types.Null -> null

            Types.BytesArray -> reader.readBytes(reader.readInt32())

            Types.StringArray -> Array(reader.readInt16().toInt()) {
                if (reader.readBoolean()) reader.readString() else null
            }

            else -> TODO("BAD ARG $id")
        }
    }
}