package com.kingpaging.qwelcome.ui.templates

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.kingpaging.qwelcome.viewmodel.templates.TemplateEditorUiState

@Suppress("FunctionNaming", "LongParameterList")
@Composable
fun TemplateEditorScreen(
    editorUiState: TemplateEditorUiState,
    onCreate: (name: String, content: String, tags: List<String>) -> Unit,
    onUpdate: (name: String, content: String, tags: List<String>) -> Unit,
    onCancelEditing: () -> Unit,
    onNameChange: (String) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    onNewTagInputChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onNameErrorChange: (Int?) -> Unit,
    onToggleDiscardDialog: (Boolean) -> Unit
) {
    val template = editorUiState.template ?: return
    key(template.id) {
        TemplateEditorContent(
            template = template,
            defaultContent = template.content,
            editorUiState = editorUiState,
            onCreate = onCreate,
            onUpdate = onUpdate,
            onCancelEditing = onCancelEditing,
            onNameChange = onNameChange,
            onTagsChange = onTagsChange,
            onNewTagInputChange = onNewTagInputChange,
            onContentChange = onContentChange,
            onNameErrorChange = onNameErrorChange,
            onToggleDiscardDialog = onToggleDiscardDialog
        )
    }
}
