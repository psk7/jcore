package pvt.psk.jcore.remoting

import java.lang.reflect.*

inline fun <reified T> createMethodProxy(Transport: IMethodInvoker): T =
    Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), arrayOf(T::class.java), RemoteMethodProxy(Transport)) as T
