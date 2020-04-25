package pvt.psk.jcore

import org.joda.time.*
import org.koin.dsl.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.network.*
import pvt.psk.jcore.utils.*
import java.net.*

val jcoreModule = module {
    single<Logger> {
        object : Logger() {
            override fun writeLog(TimeStamp: DateTime, importance: LogImportance, logCat: String, message: String) {
                println(message)
            }
        }
    }
    single { DebugInfoHolder() }
    single<BaseNetworkRelay.UdpClientFactory> {
        object : BaseNetworkRelay.UdpClientFactory {
            override fun create(bindEndPoint: InetSocketAddress, received: (ByteArray, InetSocketAddress) -> Unit) =
                SafeUdpClient(bindEndPoint, received)
        }
    }
}