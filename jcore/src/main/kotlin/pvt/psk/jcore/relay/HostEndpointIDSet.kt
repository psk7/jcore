package pvt.psk.jcore.relay

import pvt.psk.jcore.host.*
import pvt.psk.jcore.utils.*

class HostEndpointIDSet(source: Collection<HostEndpointID>) : Collection<HostEndpointID> {

    constructor(source: Array<HostEndpointID>) : this(source.toList())

    constructor(reader: BinaryReader) : this(Array(reader.readInt16().toInt()) { HostEndpointID(reader) })

    private val ids: List<HostEndpointID> = source.toList()

    fun containsHost(key: HostID) = ids.any { it.hostID == key }

    val singleValue: HostEndpointID?
        get() = if (ids.size == 1) ids[0] else null

    /**
     *  Раскрытие списка хостов.
     *  HostID.All заменяется на предоставленный список. Дубликаты удаляются.
     */
    fun expandBroadcasts(allHosts: () -> Collection<HostID>): HostEndpointIDSet {

        if (singleValue?.hostID?.isBroadcast == false)
            return this

        var nAll = true

        val l = mutableSetOf<HostEndpointID>()

        for (h in ids)
            if (h.hostID.isBroadcast && nAll) {
                l.addAll(allHosts().map { HostEndpointID(it, h.endpointID) })
                nAll = false
            } else
                l.add(h)

        return HostEndpointIDSet(l)
    }

    fun serialize(writer: BinaryWriter) {
        writer.write(ids.size.toUShort())
        ids.forEach(writer::write)
    }

    fun filter(key: HostID) = HostEndpointIDSet(ids.filter { it.hostID == key })

    fun filterHosts(key: Iterable<HostID>) = HostEndpointIDSet(ids.filter { key.contains(it.hostID) }.toTypedArray())

    override fun iterator(): Iterator<HostEndpointID> = ids.iterator()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HostEndpointIDSet

        if (ids.size != other.ids.size)
            return false

        for (i in 0 until ids.size)
            if (ids[i] != other.ids[i])
                return false

        return true
    }

    override fun hashCode() = ids.hashCode()

    override val size
        get() = ids.size

    override fun contains(element: HostEndpointID) = ids.contains(element)

    override fun containsAll(elements: Collection<HostEndpointID>) = ids.containsAll(elements)

    override fun isEmpty() = ids.isEmpty()
}