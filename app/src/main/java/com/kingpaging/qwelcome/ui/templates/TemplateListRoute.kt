package com.kingpaging.qwelcome.ui.templates

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.di.LocalTemplateListViewModel
import com.kingpaging.qwelcome.ui.EventEmission
import com.kingpaging.qwelcome.util.SoundPlayer
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListEvent
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListEventOwner
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Suppress("FunctionNaming")
@Composable
fun TemplateListRoute(
    onBack: () -> Unit,
    onOpenEditor: (String) -> Unit,
) {
    val viewModel = LocalTemplateListViewModel.current
    val soundPlayer = LocalSoundPlayer.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    CollectTemplateListEffects(
        viewModel = viewModel,
        soundPlayer = soundPlayer,
        snackbarHostState = snackbarHostState,
        onOpenEditor = onOpenEditor,
    )

    TemplateListScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onOpenEditor = onOpenEditor,
        onDeleteTemplate = viewModel::deleteTemplate,
        onDismissDeleteConfirmation = viewModel::dismissDeleteConfirmation,
        onUpdateTagFilter = viewModel::updateTagFilter,
        onClearTagFilter = viewModel::clearTagFilter,
        onRenameTemplate = viewModel::renameTemplate,
        onUpdateSearchQuery = viewModel::updateSearchQuery,
        onDismissTemplateLimitWarning = viewModel::dismissTemplateLimitWarning,
        onSetActiveTemplate = viewModel::setActiveTemplate,
        onDuplicateAndEdit = viewModel::duplicateAndEdit,
        onDuplicateTemplate = viewModel::duplicateTemplate,
        onShowDeleteConfirmation = viewModel::showDeleteConfirmation,
    )
}

@Suppress("FunctionNaming", "LocalContextGetResourceValueCall")
@Composable
private fun CollectTemplateListEffects(
    viewModel: TemplateListViewModel,
    soundPlayer: SoundPlayer,
    snackbarHostState: SnackbarHostState,
    onOpenEditor: (String) -> Unit,
) {
    val context = LocalContext.current
    val currentOnOpenEditor by rememberUpdatedState(onOpenEditor)
    val scope = rememberCoroutineScope()
    val eventEmission by remember(viewModel) {
        viewModel.eventsFor(TemplateListEventOwner.LIBRARY).map(::EventEmission)
    }.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(eventEmission) {
        val event = eventEmission?.event ?: return@LaunchedEffect
        if (event is TemplateListEvent.TemplateDuplicated && event.openEditor) {
            currentOnOpenEditor(event.template.id)
            return@LaunchedEffect
        }
        val snackbar = event.toSnackbar(context) ?: return@LaunchedEffect
        scope.launch {
            if (event.shouldBeep()) {
                soundPlayer.playBeep()
            }
            val result =
                snackbarHostState.showSnackbar(
                    message = snackbar.message,
                    actionLabel = snackbar.actionLabel,
                    duration = snackbar.duration,
                )
            if (result == SnackbarResult.ActionPerformed) {
                event.performSnackbarAction(viewModel, currentOnOpenEditor)
            }
        }
    }
}

private data class TemplateSnackbar(
    val message: String,
    val actionLabel: String? = null,
    val duration: SnackbarDuration = SnackbarDuration.Short,
)

private fun TemplateListEvent.toSnackbar(context: Context): TemplateSnackbar? =
    when (this) {
        is TemplateListEvent.Error ->
            TemplateSnackbar(
                message = checkNotNull(toTemplateErrorMessage(context)),
                duration = SnackbarDuration.Long,
            )
        is TemplateListEvent.TemplateDeleted ->
            TemplateSnackbar(
                context.getString(R.string.toast_template_deleted, name),
            )
        is TemplateListEvent.TemplateDuplicated ->
            TemplateSnackbar(
                context.getString(R.string.toast_template_duplicated, template.name),
            )
        is TemplateListEvent.ActiveTemplateChanged ->
            TemplateSnackbar(
                message = context.getString(R.string.toast_template_active, template.name),
                actionLabel = context.getString(R.string.action_undo),
                duration = SnackbarDuration.Long,
            )
        is TemplateListEvent.TemplateSelectionBlocked ->
            TemplateSnackbar(
                message = checkNotNull(toTemplateErrorMessage(context)),
                actionLabel = context.getString(R.string.action_fix),
                duration = SnackbarDuration.Long,
            )
        is TemplateListEvent.TemplateRenamed ->
            TemplateSnackbar(
                context.getString(R.string.toast_template_renamed, template.name),
            )
    }

private fun TemplateListEvent.shouldBeep(): Boolean =
    this is TemplateListEvent.Error || this is TemplateListEvent.TemplateSelectionBlocked

private fun TemplateListEvent.performSnackbarAction(
    viewModel: TemplateListViewModel,
    onOpenEditor: (String) -> Unit,
) {
    when (this) {
        is TemplateListEvent.ActiveTemplateChanged -> viewModel.undoTemplateSelection(change)
        is TemplateListEvent.TemplateSelectionBlocked -> onOpenEditor(template.id)
        else -> Unit
    }
}
