package pvt.psk.jcore.utils

import kotlinx.atomicfu.*
import kotlinx.coroutines.*

/**
 * Служба гарантированной доставки сообщений о версионированном состоянии
 *
 * @param send Функция отправки сообщения
 * @param timeout Время ожидания ответа
 */
class RevisionEnsurator<T : Any>(private val send: (T, AckToken) -> Boolean,
                                 private val timeout: TimeSpan) {

    private val map = HashMap<T, Entry>()

    private val rev = atomic(1)

    /**
     * Текущая версия состояния
     */
    val revision
        get() = rev.value

    interface IReply {
        val ack: AckToken
        val accepted: Boolean
    }

    private data class Entry(var revision: Int)

    /**
     * Увеличение номера версии состояния
     */
    fun bumpRevision() {
        rev.incrementAndGet()
    }

    /**
     * Подтверждение что все цели имеют актуальную версию текущего состояния
     *
     * @param ids Список целей для подтверждения
     * @return Список целей, не подтвердивших прием актуальной версии состояния в заданное время
     */
    suspend inline fun ensure(ids: Array<T>): Collection<T> = supervisorScope {
        ids.map { async { ensure(it) } }.awaitAll()
            .mapIndexed { i, b -> if (b) null else ids[i] }.filterNotNull().toList()
    }

    /**
     * Подтверждение что цель имеет актуальную версию текущего состояния
     *
     * @param id Идентификатор цели для подтверждения
     * @return true - если цель подтвердила, что имеет актуальную версию. false - если цель явно подтвердила,
     * что НЕ ПРИНИМАЕТ переданную версию состояния, или время ожидания ответа от цели истекло
     */
    suspend fun ensure(id: T): Boolean {
        val rid = rev.value

        val e: Entry

        synchronized(map)
        {
            e = map[id] ?: Entry(0).also { map[id] = it }

            if (rid <= e.revision)
                return true
        }

        val tk = registerAckToken(timeout)

        if (!send(id, tk)) {
            tk.unregister()
            return false
        }

        val r = tk.await<IReply>() ?: return false

        synchronized(map)
        {
            if (rid > e.revision)
                e.revision = rid
        }

        return r.accepted
    }

    /**
     * Обработка ответа на переданный запрос передачи актуальной версии состояния
     */
    fun replyReceived(reply: IReply) {
        reply.ack.received<IReply?>(reply)
    }
}