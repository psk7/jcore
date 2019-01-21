package pvt.psk.jcore.remoting

import java.lang.*

interface IMethodProxyFactory {
    fun create(Transport: IMethodInvoker, Class: Class<*>): Any
}