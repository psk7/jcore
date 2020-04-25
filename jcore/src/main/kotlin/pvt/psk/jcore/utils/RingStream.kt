package pvt.psk.jcore.utils

import kotlinx.atomicfu.*
import java.io.*

class RingStream(bufSize: Int = 65536) {

    private val trr = atomic(0L)
    private val trw = atomic(0L)

    val written
        get() = trw.value

    val readed
        get() = trr.value

    data class InputStreamWrapper(val readBytes: (ByteArray?, Int, Int) -> Int, val onClose: () -> Unit) : InputStream() {
        override fun read(): Int {
            val b = byteArrayOf(0)

            return when (readBytes(b, 0, 1)) {
                -1   -> -1
                else -> b[0].toInt()
            }
        }

        override fun read(b: ByteArray?, off: Int, len: Int): Int = readBytes(b, off, len)

        override fun close() = onClose()
    }

    data class OutputStreamWrapper(val writeBytes: (ByteArray?, Int, Int) -> Unit, val onClose: () -> Unit) : OutputStream() {

        override fun write(b: Int) = writeBytes(byteArrayOf(b.toByte()), 0, 1)

        override fun write(b: ByteArray?, off: Int, len: Int): Unit = writeBytes(b, off, len)

        override fun close() = onClose()
    }

    //private val hasdata = Object()
    private val l = Any()

    private val buf = ByteArray(bufSize)

    private var readpos = 0
    private var writepos = 0
    private var avail = 0
    private var free = bufSize
    private var closed = false

    fun getOutputStream(): OutputStream = OutputStreamWrapper(::write, ::close)

    fun getInputStream(): InputStream = InputStreamWrapper(::read, ::close)

    fun read(buffer: ByteArray?, offset: Int, count: Int): Int {

        if (buffer == null)
            return -1

        synchronized(l) {
            while (avail == 0 && !closed) // Ожидание, пока появятся данные или поток будет закрыт
                (l as Object).wait(1)

            if (avail == 0)
                return -1

            val toRead = if (avail > count) count else avail

            if (readpos + toRead <= buf.size)
                buf.copyInto(buffer, offset, readpos, readpos + toRead)
            else {
                val p = buf.size - readpos

                buf.copyInto(buffer, offset, readpos, readpos + p)
                buf.copyInto(buffer, offset + p, 0, toRead - p)
            }

            avail -= toRead
            free += toRead

            readpos += toRead

            if (readpos >= buf.size)
                readpos -= buf.size

            trr.addAndGet(toRead.toLong())

            (l as Object).notify()

            return toRead
        }
    }

    val flushed = atomic(0L)

    fun write(buffer: ByteArray?, offset: Int, count: Int) {
        var o = offset
        var c = count

        while (c > 0 && !closed) {
            val written = writeInternal(buffer, o, c)

            if (written == 0) {
                flushed.addAndGet(c.toLong())
                return
            }

            o += written
            c -= written
        }

        flushed.addAndGet(c.toLong())
    }

    private fun writeInternal(buffer: ByteArray?, offset: Int, count: Int): Int {

        if (buffer == null)
            return 0

        return synchronized(l) {
            while (free == 0 && !closed)
                (l as Object).wait(1)

            if (free == 0 || closed)
                return 0

            val c = if (count > free) free else count

            if (writepos + c <= buf.size)
                buffer.copyInto(buf, writepos, offset, offset + c)
            else {
                val p = buf.size - writepos
                buffer.copyInto(buf, writepos, offset, offset + p)
                buffer.copyInto(buf, 0, offset + p, offset + c)
            }

            writepos += c
            if (writepos >= buf.size)
                writepos -= buf.size

            avail += c
            free -= c

            trw.addAndGet(c.toLong())

            (l as Object).notify()

            return c
        }
    }

    fun close() = synchronized(l) {
        closed = true
        (l as Object).notify()
    }
}