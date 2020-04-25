package pvt.psk.jcore.utils

import kotlinx.atomicfu.*
import kotlinx.coroutines.*
import java.io.*
import java.util.concurrent.*
import kotlin.coroutines.*

class ForkStream : OutputStream(), CoroutineScope {

    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val wr = atomic(0L)

    private var lsttgt: Array<OutputStream?>? = null
    private val lst = ConcurrentLinkedQueue<Deferred<OutputStream?>>()
    private val dstrdy = CompletableDeferred<Unit>()
    private val complete = CompletableDeferred<Unit>()
    private val disposes = ConcurrentHashMap<OutputStream, Boolean>()

    val written
        get() = wr.value

    /**
     * Добавляет целевой поток для передачи данных
     */
    fun addTargetStream(stream: OutputStream, leaveOpen: Boolean = false) =
        addTargetStream(CompletableDeferred(stream), leaveOpen)

    /**
     * Добавляет целевой поток для передачи данных
     */
    fun addTargetStream(stream: Deferred<OutputStream?>, leaveOpen: Boolean) {

        if (lsttgt != null)
            throw Exception("После разрешения передачи добавление новых целевых потоков невозможно");

        fun t(): Deferred<OutputStream?> = async {
            val s = stream.await()

            if (s != null)
                disposes[s] = true

            return@async s
        }

        lst.add(if (leaveOpen) stream else t())
    }

    fun enableTransferAsync(): Job {

        if (lsttgt != null)
            throw Exception()

        return launch {
            lsttgt = lst.awaitAll().filterNotNull().toTypedArray()

            dstrdy.complete(Unit)
        }
    }

    val transferEnabled: Job
        get() = dstrdy

    val transferCompleted: Job
        get() = complete

    /**
     * Writes the specified byte to this output stream. The general
     * contract for `write` is that one byte is written
     * to the output stream. The byte to be written is the eight
     * low-order bits of the argument `b`. The 24
     * high-order bits of `b` are ignored.
     *
     *
     * Subclasses of `OutputStream` must provide an
     * implementation for this method.
     *
     * @param      b   the `byte`.
     * @exception  IOException  if an I/O error occurs. In particular,
     * an `IOException` may be thrown if the
     * output stream has been closed.
     */
    override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)

    /**
     * Writes `len` bytes from the specified byte array
     * starting at offset `off` to this output stream.
     * The general contract for `write(b, off, len)` is that
     * some of the bytes in the array `b` are written to the
     * output stream in order; element `b[off]` is the first
     * byte written and `b[off+len-1]` is the last byte written
     * by this operation.
     *
     *
     * The `write` method of `OutputStream` calls
     * the write method of one argument on each of the bytes to be
     * written out. Subclasses are encouraged to override this method and
     * provide a more efficient implementation.
     *
     *
     * If `b` is `null`, a
     * `NullPointerException` is thrown.
     *
     *
     * If `off` is negative, or `len` is negative, or
     * `off+len` is greater than the length of the array
     * `b`, then an `IndexOutOfBoundsException` is thrown.
     *
     * @param      b     the data.
     * @param      off   the start offset in the data.
     * @param      len   the number of bytes to write.
     * @exception  IOException  if an I/O error occurs. In particular,
     * an `IOException` is thrown if the output
     * stream is closed.
     */
    override fun write(b: ByteArray, off: Int, len: Int) {

        dstrdy.wait()

        lsttgt?.forEachIndexed { i, st ->

            if (st == null)
                return@forEachIndexed

            try {
                st.write(b, off, len)

                wr.addAndGet(len.toLong())
            }
            catch (e: Exception) {
                st.close()

                disposes[st] = false
                lsttgt?.set(i, null)
            }
        }
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with this stream. The general contract of `close`
     * is that it closes the output stream. A closed stream cannot perform
     * output operations and cannot be reopened.
     *
     *
     * The `close` method of `OutputStream` does nothing.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    override fun close() {
        super.close()

        disposes.filter { it.value }.forEach {
            it.key.close()
        }

        complete.complete(Unit)
    }
}