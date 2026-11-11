package io.kotest.provided

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.extensions.Extension
import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.spec.Spec
import io.mockk.clearAllMocks

object ProjectConfig : AbstractProjectConfig() {

    // TODO: release the mocks after each spec (MockK 1.14.12 clearAllStubsFromMemory) when the companion region
    //  caches no longer hold the first spec's mocks (item 028); see docs/issues/2026-11-11-core-test-heap-leak.md
    override fun extensions(): List<Extension> = listOf(object : AfterSpecListener {
        override suspend fun afterSpec(spec: Spec) =
            clearAllMocks(answers = false, recordedCalls = true, childMocks = false)
    })
}
