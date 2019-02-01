package pvt.psk.jcore.network

import kotlinx.coroutines.*
import java.nio.channels.*

class UdpSelector(val selector: Selector) {

    private var _loop: Job? = null

    fun run() {
        _loop = GlobalScope.launch {
            while (isActive) {

                // Проверяем, если ли какие-либо активности -
                // входящие соединения или входящие данные в
                // существующем соединении.
                val num = selector.select(100)

                // Если никаких активностей нет, выходим из цикла
                // и снова ждём.
                if (num == 0)
                    continue

                // Получим ключи, соответствующие активности,
                // которые могут быть распознаны и обработаны один за другим.
                val keys = selector.selectedKeys()

                val it = keys.iterator()

                while (it.hasNext()) {
                    // Получим ключ, представляющий один из битов
                    // активности ввода/вывода.
                    val key = it.next() as SelectionKey

                    if (key.isReadable)
                        when (val att = key.attachment()) {
                            is SafeUdpClient -> att.processOnReceived()
                        }

                    it.remove()
                }
            }
        }
    }

    fun stop() {
        runBlocking { _loop?.cancelAndJoin() }
    }
}