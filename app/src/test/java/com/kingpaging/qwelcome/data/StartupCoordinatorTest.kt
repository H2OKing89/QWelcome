package com.kingpaging.qwelcome.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class StartupCoordinatorTest {
    @Test
    fun `malformed legacy templates show recovery and retry can succeed`() =
        runTest {
            var corrupted = true
            val coordinator =
                StartupCoordinator(
                    scope = backgroundScope,
                    initialize = {
                        if (corrupted) throw TemplatesMigrationException("{invalid", IllegalArgumentException())
                    },
                    recoverLegacyTemplates = { corrupted = false },
                )

            coordinator.retry()
            runCurrent()
            assertEquals(AppStartupState.Failed(true), coordinator.state.value)
            coordinator.continueWithoutLegacyTemplates()
            runCurrent()
            assertEquals(AppStartupState.Ready, coordinator.state.value)
        }

    @Test
    fun `ordinary IO failures allow retry without offering destructive recovery`() =
        runTest {
            var attempts = 0
            var resetCalls = 0
            val coordinator =
                StartupCoordinator(
                    scope = backgroundScope,
                    initialize = { if (attempts++ == 0) throw IOException("temporarily unavailable") },
                    recoverLegacyTemplates = { resetCalls++ },
                )
            coordinator.retry()
            runCurrent()
            assertEquals(AppStartupState.Failed(false), coordinator.state.value)
            coordinator.continueWithoutLegacyTemplates()
            runCurrent()
            assertEquals(0, resetCalls)
            coordinator.retry()
            runCurrent()
            assertEquals(AppStartupState.Ready, coordinator.state.value)
        }
}
