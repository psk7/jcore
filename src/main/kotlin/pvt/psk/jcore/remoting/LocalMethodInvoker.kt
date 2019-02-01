package pvt.psk.jcore.remoting

import kotlinx.coroutines.*

class LocalMethodInvoker(val target: Any) : IMethodInvoker {

    private val mmap = MethodsMap(target.javaClass)

    override fun invokeAsync(MethodID: MethodID, Arguments: Arguments): Deferred<Any?> {

        val m = mmap.get(MethodID)

        return CompletableDeferred(when {
                                       m != null -> invoke(MethodID, Arguments)
                                       else      -> null
                                   })

    }

    override fun invoke(MethodID: MethodID, Arguments: Arguments): Any? {

        val m = mmap.get(MethodID)

        return when {
            m != null -> m.invoke(target, *Arguments.unpacked())
            else      -> null
        }
    }
}