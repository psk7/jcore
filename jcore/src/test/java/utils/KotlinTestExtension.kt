package utils

import org.junit.jupiter.api.extension.*
import org.koin.core.*
import org.koin.core.context.*
import org.koin.dsl.*

// From: https://github.com/InsertKoinIO/koin/issues/763 (Koin-test support for Junit 5)

/**
 * [Extension] which will automatically start and stop Koin.
 * @author Nick Cipollo
 * @author Greg Hibberd
 */

class KoinTestExtension private constructor(private val appDeclaration: KoinAppDeclaration) : BeforeEachCallback, AfterEachCallback {

    var _koin: Koin? = null
    val koin: Koin
        get() = _koin ?: error("No Koin application found")

    override fun beforeEach(context: ExtensionContext) {
        _koin = startKoin(appDeclaration = appDeclaration).koin
    }

    override fun afterEach(context: ExtensionContext?) {
        stopKoin()
        _koin = null
    }

    companion object {
        fun create(appDeclaration: KoinAppDeclaration): KoinTestExtension {
            return KoinTestExtension(appDeclaration)
        }
    }
}