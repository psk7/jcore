package pvt.psk.jcore.remoting

import kotlinx.coroutines.*

interface IMethodInvoker {
    fun InvokeAsync(MethodID:MethodID, Arguments:Arguments) : Deferred<Any?>

    fun Invoke(MethodID:MethodID, Arguments:Arguments) : Any?
}