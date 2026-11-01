package com.kingpaging.qwelcome.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal sealed interface AppStartupState {
    data object Loading : AppStartupState

    data object Ready : AppStartupState

    data class Failed(
        val canRecoverLegacyTemplates: Boolean,
    ) : AppStartupState
}

/** Gates feature collectors until persistence can be read, including legacy migration. */
internal class StartupCoordinator(
    private val scope: CoroutineScope,
    private val initialize: suspend () -> Unit,
    private val recoverLegacyTemplates: suspend () -> Unit,
) {
    private val mutableState = MutableStateFlow<AppStartupState>(AppStartupState.Loading)
    val state = mutableState.asStateFlow()
    private var job: Job? = null

    fun retry() {
        start(recover = false)
    }

    fun continueWithoutLegacyTemplates() {
        if ((state.value as? AppStartupState.Failed)?.canRecoverLegacyTemplates != true) return
        start(recover = true)
    }

    // Every initialization failure must keep feature collectors gated behind the recovery UI.
    @Suppress("TooGenericExceptionCaught")
    private fun start(recover: Boolean) {
        if (job?.isActive == true) return
        mutableState.value = AppStartupState.Loading
        job =
            scope.launch {
                try {
                    if (recover) recoverLegacyTemplates()
                    initialize()
                    mutableState.value = AppStartupState.Ready
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    mutableState.value =
                        AppStartupState.Failed(
                            canRecoverLegacyTemplates = exception is TemplatesMigrationException,
                        )
                }
            }
    }
}
