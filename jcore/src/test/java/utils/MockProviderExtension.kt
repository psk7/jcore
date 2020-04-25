package utils

import org.junit.jupiter.api.extension.*
import org.koin.test.mock.*

// From: https://github.com/InsertKoinIO/koin/issues/763 (Koin-test support for Junit 5)

/**
 * @author Greg Hibberd
 * @since March 28, 2020
 */
class MockProviderExtension private constructor(private val mockProvider: Provider<*>) : BeforeEachCallback {

    override fun beforeEach(context: ExtensionContext) {
        MockProvider.register(mockProvider)
    }

    companion object {
        fun create(mockProvider: Provider<*>): MockProviderExtension {
            return MockProviderExtension(mockProvider)
        }
    }
}