package pvt.psk.jcore.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.util.concurrent.*

import pvt.psk.jcore.utils.AckToken.Companion.New

class AckMonitor<T>
{
    companion object
    {
        @JvmStatic
        private var _lst: ConcurrentMap<AckToken, CompletableDeferred<BinaryReader>> =
            ConcurrentHashMap<AckToken, CompletableDeferred<BinaryReader>>()

        @JvmStatic
        fun Register(): Pair<AckToken, Deferred<BinaryReader>>
        {
            val tk = New()
            val cd = CompletableDeferred<BinaryReader>()

            _lst[tk] = cd

            return Pair(tk, cd)
        }

        @JvmStatic
        fun Received(Token: AckToken, Data: BinaryReader)
        {
            val cd = _lst[Token] ?: return

            cd.complete(Data)
            _lst.remove(Token)
        }
    }
}
