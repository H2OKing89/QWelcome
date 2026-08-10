@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.templates

import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.di.LocalTemplateListViewModel
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListEvent
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListEventOwner

@Suppress("LocalContextGetResourceValueCall")
@Composable
fun TemplateEditorScreen(
    onBack: () -> Unit
) {
    val vm = LocalTemplateListViewModel.current
    val soundPlayer = LocalSoundPlayer.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val editorUiState by vm.templateEditorUiState.collectAsStateWithLifecycle()
    val template = uiState.editingTemplate

    val hasNavigatedBack = remember { mutableStateOf(false) }
    val safeNavigate: () -> Unit = remember(onBack) {
        {
            if (!hasNavigatedBack.value) {
                hasNavigatedBack.value = true
                onBack()
            }
        }
    }

    if (template == null) {
        LaunchedEffect(Unit) { safeNavigate() }
        return
    }

    LaunchedEffect(vm, lifecycleOwner) {
        vm.eventsFor(TemplateListEventOwner.EDITOR)
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collect { event ->
                when (event) {
                    is TemplateListEvent.TemplateCreated -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_template_created, event.template.name),
                            Toast.LENGTH_SHORT
                        ).show()
                        safeNavigate()
                    }

                    is TemplateListEvent.TemplateUpdated -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_template_updated, event.template.name),
                            Toast.LENGTH_SHORT
                        ).show()
                        safeNavigate()
                    }

                    is TemplateListEvent.Error -> {
                        soundPlayer.playBeep()
                        Toast.makeText(
                            context,
                            checkNotNull(event.toTemplateErrorMessage(context)),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    is TemplateListEvent.TemplateDeleted,
                    is TemplateListEvent.TemplateDuplicated,
                    is TemplateListEvent.ActiveTemplateChanged,
                    is TemplateListEvent.TemplateSelectionBlocked,
                    is TemplateListEvent.TemplateRenamed -> Unit
                }
            }
    }

    key(template.id) {
        TemplateEditorContent(
            template = template,
            defaultContent = vm.getDefaultTemplateContent(),
            editorUiState = editorUiState,
            onCreate = vm::createTemplate,
            onUpdate = vm::updateTemplate,
            onCancelEditing = {
                vm.cancelEditing()
                safeNavigate()
            },
            onNameChange = vm::updateName,
            onTagsChange = vm::updateTags,
            onNewTagInputChange = vm::updateNewTagInput,
            onContentChange = vm::updateContent,
            onNameErrorChange = vm::setNameError,
            onToggleDiscardDialog = vm::toggleDiscardDialog
        )
    }
}
