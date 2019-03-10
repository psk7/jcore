package pvt.psk.jcore.network

import pvt.psk.jcore.host.*
import java.net.*
import java.util.concurrent.*
import java.util.concurrent.locks.*
import kotlin.concurrent.*

class IPAddressDirectory {

    private class List {
        val l = HashSet<InetAddress>()
        val lock = ReentrantReadWriteLock()

        var last: InetAddress? = null

        private fun ordered() = l.sortedByDescending { score(it) }.toTypedArray()

        private fun score(addr: InetAddress): Int {
            val add = if (addr == last) 1 else 0

            return when {
                addr.isLoopbackAddress -> 0
                addr is Inet4Address -> 2
                addr !is Inet6Address -> 0
                else -> if (addr.isLinkLocalAddress) 10 else 4
            } + add
        }

        fun set(addr: InetAddress) {
            lock.write {
                if (!l.add(addr))
                    return

                last = addr
                primary = ordered().firstOrNull()
            }
        }

        var primary: InetAddress? = null
            get() = lock.read { field }

        fun getAll() = lock.read { ordered() }
    }

    private val _list = ConcurrentHashMap<HostID, List>()

    /**
     * Устанавливает соответствие между идентификатором хоста и IP адресом
     *
     * @param host Идентификатор хоста
     * @param addr IP адрес хоста
     */
    fun set(host: HostID, addr: InetAddress): Unit = _list.getOrPut(host) { List() }.set(addr)

    /**
     * Возвращает предпочтительный IP адрес для указанного хоста
     *
     * @param host Идентификатор хоста
     *
     * @return Предпочтительный IP адрес
     */
    fun resolve(host: HostID) = _list.getOrPut(host) { List() }.primary

    /**
     * Возвращает все адреса для указанного хоста, отсортированные в порядке предпочтительности
     *
     * @param host Идентификатор хоста
     *
     * @return Список адресов
     */
    fun resolveAll(host: HostID) = _list.getOrPut(host) { List() }.getAll()
}