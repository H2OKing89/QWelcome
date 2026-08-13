package com.kingpaging.qwelcome.ui

import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.di.LocalCustomerIntakeViewModel
import com.kingpaging.qwelcome.di.LocalNavigator
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.di.LocalTemplateSelectionViewModel
import com.kingpaging.qwelcome.viewmodel.UiEvent
import com.kingpaging.qwelcome.viewmodel.templates.TemplateSelectionEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun CustomerIntakeRoute(
    onOpenSettings: () -> Unit,
    onOpenTemplates: () -> Unit = {},
) {
    val customerIntakeViewModel = LocalCustomerIntakeViewModel.current
    val templateSelectionViewModel = LocalTemplateSelectionViewModel.current
    val navigator = LocalNavigator.current
    val soundPlayer = LocalSoundPlayer.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by customerIntakeViewModel.uiState.collectAsStateWithLifecycle()
    val templateUiState by templateSelectionViewModel.uiState.collectAsStateWithLifecycle()
    val formFocusTargets = remember { CustomerFormFocusTargets() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var copySuccess by remember { mutableStateOf(false) }

    LaunchedEffect(customerIntakeViewModel, lifecycleOwner) {
        var copySuccessResetJob: Job? = null
        customerIntakeViewModel.uiEvent
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collect { event ->
                when (event) {
                    is UiEvent.CopySuccess -> {
                        copySuccessResetJob?.cancel()
                        copySuccess = true
                        copySuccessResetJob =
                            launch {
                                delay(COPY_SUCCESS_DURATION_MILLIS)
                                copySuccess = false
                            }
                    }
                    is UiEvent.ShowToast -> {
                        Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    }
                    is UiEvent.ValidationFailed -> {
                        soundPlayer.playBeep()
                        formFocusTargets.firstInvalid(customerIntakeViewModel.uiState.value)?.let { target ->
                            target.focusRequester.requestFocus()
                            target.bringIntoViewRequester.bringIntoView()
                        }
                    }
                    is UiEvent.ActionFailed -> soundPlayer.playBeep()
                    is UiEvent.RateLimitExceeded -> {
                        soundPlayer.playBeep()
                        Toast
                            .makeText(
                                context,
                                resources.getString(R.string.toast_rate_limit),
                                Toast.LENGTH_LONG,
                            ).show()
                    }
                }
            }
    }

    LaunchedEffect(templateSelectionViewModel, lifecycleOwner) {
        templateSelectionViewModel
            .events
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collect { event ->
                val message =
                    when (event) {
                        is TemplateSelectionEvent.Error -> event.message
                        is TemplateSelectionEvent.SelectionBlocked ->
                            resources.getString(
                                R.string.error_template_cannot_use,
                                event.template.name,
                                event.missingPlaceholders.joinToString(", "),
                            )
                    }
                soundPlayer.playBeep()
                snackbarHostState.showSnackbar(message)
            }
    }

    CustomerIntakeScreen(
        uiState = uiState,
        templateUiState = templateUiState,
        snackbarHostState = snackbarHostState,
        formFocusTargets = formFocusTargets,
        copySuccess = copySuccess,
        actions =
            CustomerIntakeActions(
                onDismissQr = { customerIntakeViewModel.setShowQrSheet(false) },
                onClearForm = {
                    val undoToken = customerIntakeViewModel.clearFormWithUndo()
                    scope.launch {
                        var undoRestored = false
                        try {
                            val result =
                                snackbarHostState.showSnackbar(
                                    message = resources.getString(R.string.toast_form_cleared),
                                    actionLabel = resources.getString(R.string.action_undo),
                                )
                            if (result == SnackbarResult.ActionPerformed) {
                                undoRestored = customerIntakeViewModel.undoClearForm(undoToken)
                            }
                        } finally {
                            if (!undoRestored) {
                                customerIntakeViewModel.discardClearFormUndo(undoToken)
                            }
                        }
                    }
                },
                onTemplateSelected = {
                    templateSelectionViewModel.selectTemplate(it)
                },
                onCustomerNameChanged = customerIntakeViewModel::onCustomerNameChanged,
                onCustomerPhoneChanged = customerIntakeViewModel::onCustomerPhoneChanged,
                onSsidChanged = customerIntakeViewModel::onSsidChanged,
                onSecurityTypeChanged = customerIntakeViewModel::onSecurityTypeChanged,
                onHiddenNetworkChanged = customerIntakeViewModel::onHiddenNetworkChanged,
                onOpenNetworkChanged = customerIntakeViewModel::onOpenNetworkChanged,
                onPasswordChanged = customerIntakeViewModel::onPasswordChanged,
                onAccountNumberChanged = customerIntakeViewModel::onAccountNumberChanged,
                onSmsClick = { customerIntakeViewModel.onSmsClicked(navigator) },
                onShareClick = { customerIntakeViewModel.onShareClicked(navigator) },
                onCopyClick = { customerIntakeViewModel.onCopyClicked(navigator) },
                onShowQr = { customerIntakeViewModel.setShowQrSheet(true) },
            ),
        onOpenSettings = onOpenSettings,
        onOpenTemplates = onOpenTemplates,
    )
}

private const val COPY_SUCCESS_DURATION_MILLIS = 1_500L
