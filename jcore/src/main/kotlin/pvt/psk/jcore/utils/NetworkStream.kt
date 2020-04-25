package pvt.psk.jcore.utils

import java.io.*

class NetworkInputStream(private val c: InputStream) : InputStream() {

    override fun read(): Int =
        try {
            c.read()
        }
        catch (ignored: IOException) {
            -1
        }

    override fun read(b: ByteArray, off: Int, len: Int) = try {
        c.read(b, off, len)
    }
    catch (ignored: IOException) {
        -1
    }
}

class NetworkOutputStream(private val c: OutputStream, private val closer: (() -> Unit)?) : OutputStream() {

    override fun write(b: Int) = c.write(b)

    override fun write(b: ByteArray, off: Int, len: Int) = c.write(b, off, len)

    override fun close() {
        flush()

        closer?.invoke()
    }
}