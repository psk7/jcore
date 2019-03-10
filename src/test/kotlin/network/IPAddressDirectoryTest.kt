package network

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.network.*
import java.net.*
import java.util.*

class IPAddressDirectoryTest {

    @Test
    fun scoring() {
        val d = IPAddressDirectory()
        val h = HostID(UUID.randomUUID(), "H1")

        fun assert(addr: String) = assertEquals(InetAddress.getByName(addr), d.resolve(h))

        d.set(h, InetAddress.getByName("192.168.0.1"))
        assert("192.168.0.1")

        d.set(h, InetAddress.getByName("127.0.0.1"))
        assert("192.168.0.1")

        d.set(h, InetAddress.getByName("::1"))
        assert("192.168.0.1")

        d.set(h, InetAddress.getByName("2001:470:dfaf:1::1"))
        assert("2001:470:dfaf:1::1")

        // LinkLocal адреса имеют максимальный приоритет
        d.set(h, InetAddress.getByName("fe80::6cd6:8446:2406:655"))
        assert("fe80::6cd6:8446:2406:655")

        d.set(h, InetAddress.getByName("2001:470:dfaf:1::2"))
        assert("fe80::6cd6:8446:2406:655")

        d.set(h, InetAddress.getByName("::1"))
        assert("fe80::6cd6:8446:2406:655")

        // Последний установленный имеет дополнительный приоритет
        d.set(h, InetAddress.getByName("fe80::6cd6:8446:2406:656"))
        assert("fe80::6cd6:8446:2406:656")
    }
}