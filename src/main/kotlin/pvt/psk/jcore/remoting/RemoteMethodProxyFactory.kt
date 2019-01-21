package pvt.psk.jcore.remoting

import java.lang.reflect.*

class RemoteMethodProxyFactory : IMethodProxyFactory {

    override fun create(Transport: IMethodInvoker, Class: Class<*>): Any {

        return Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), arrayOf(Class), RemoteMethodProxy(Transport))
    }

}