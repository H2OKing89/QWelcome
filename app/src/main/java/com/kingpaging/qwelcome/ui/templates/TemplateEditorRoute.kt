package com.kingpaging.qwelcome.ui.templates

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.viewmodel.templates.TemplateEditorEvent
import com.kingpaging.qwelcome.viewmodel.templates.TemplateEditorViewModel

@Suppress("FunctionNaming", "LocalContextGetResourceValueCall", "LongMethod")
@Composable
fun TemplateEditorRoute(
    viewModel: TemplateEditorViewModel,
    onBack: () -> Unit
) {
    val soundPlayer = LocalSoundPlayer.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val editorUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val template = editorUiState.template

    val hasNavigatedBack = remember { mutableStateOf(false) }
    val safeNavigate: () -> Unit = remember(onBack) {
        {
            if (!hasNavigatedBack.value) {
                hasNavigatedBack.value = true
                onBack()
            }
        }
    }

    if (!editorUiState.isLoading && template == null) {
        LaunchedEffect(Unit) { safeNavigate() }
        return
    }
    if (template == null) return

    LaunchedEffect(viewModel, lifecycleOwner) {
        viewModel.events
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collect { event ->
                when (event) {
                    is TemplateEditorEvent.TemplateCreated -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_template_created, event.template.name),
                            Toast.LENGTH_SHORT
                        ).show()
                        safeNavigate()
                    }

                    is TemplateEditorEvent.TemplateUpdated -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_template_updated, event.template.name),
                            Toast.LENGTH_SHORT
                        ).show()
                        safeNavigate()
                    }

                    is TemplateEditorEvent.Error -> {
                        soundPlayer.playBeep()
                        Toast.makeText(
                            context,
                            event.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
    }

    TemplateEditorScreen(
        editorUiState = editorUiState,
        onCreate = viewModel::createTemplate,
        onUpdate = viewModel::updateTemplate,
        onCancelEditing = safeNavigate,
        onNameChange = viewModel::updateName,
        onTagsChange = viewModel::updateTags,
        onNewTagInputChange = viewModel::updateNewTagInput,
        onContentChange = viewModel::updateContent,
        onNameErrorChange = viewModel::setNameError,
        onToggleDiscardDialog = viewModel::toggleDiscardDialog
    )
}
