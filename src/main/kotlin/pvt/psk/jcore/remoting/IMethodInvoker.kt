package pvt.psk.jcore.remoting

import kotlinx.coroutines.*

interface IMethodInvoker {
    fun invokeAsync(MethodID:MethodID, Arguments:Arguments) : Deferred<Any?>

    fun invoke(MethodID:MethodID, Arguments:Arguments) : Any?
}