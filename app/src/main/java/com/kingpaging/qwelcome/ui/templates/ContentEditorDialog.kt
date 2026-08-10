@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.templates

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kingpaging.qwelcome.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ContentEditorDialog(
    contentState: TextFieldState,
    contentFocusRequester: FocusRequester,
    contentInteractionSource: MutableInteractionSource,
    contentError: String?,
    onDismissRequest: () -> Unit,
    onInsertPlaceholder: (String) -> Unit
) {
    val parentFocusManager = LocalFocusManager.current
    val parentKeyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        parentFocusManager.clearFocus(force = true)
        parentKeyboardController?.hide()
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val workspaceKeyboardController = LocalSoftwareKeyboardController.current

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            shape = RectangleShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.label_message),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.content_desc_back),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = onDismissRequest) {
                            Text(stringResource(R.string.action_done))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PlaceholderToolbar { placeholder ->
                        onInsertPlaceholder(placeholder)
                        workspaceKeyboardController?.show()
                    }

                    MessageValidationStrip(contentError = contentError)

                    ContentEditorField(
                        contentState = contentState,
                        contentFocusRequester = contentFocusRequester,
                        contentInteractionSource = contentInteractionSource,
                        contentError = contentError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun MessageValidationStrip(contentError: String?) {
    if (contentError == null) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Text(
            text = stringResource(
                R.string.error_template_missing_placeholders,
                contentError
            ),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
internal fun ContentEditorField(
    contentState: TextFieldState,
    contentFocusRequester: FocusRequester,
    contentInteractionSource: MutableInteractionSource,
    contentError: String?,
    modifier: Modifier = Modifier
) {
    val hasContentError = contentError != null
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = if (hasContentError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.secondary
        },
        unfocusedBorderColor = if (hasContentError) {
            MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        } else {
            MaterialTheme.colorScheme.outline
        },
        cursorColor = MaterialTheme.colorScheme.secondary,
        focusedLabelColor = if (hasContentError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.secondary
        },
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    BasicTextField(
        state = contentState,
        lineLimits = TextFieldLineLimits.MultiLine(
            minHeightInLines = 1,
            maxHeightInLines = Int.MAX_VALUE
        ),
        modifier = modifier.focusRequester(contentFocusRequester),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.secondary),
        interactionSource = contentInteractionSource,
        decorator = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = contentState.text.toString(),
                innerTextField = innerTextField,
                enabled = true,
                singleLine = false,
                visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                interactionSource = contentInteractionSource,
                label = { Text(stringResource(R.string.label_message)) },
                isError = hasContentError,
                colors = fieldColors
            )
        }
    )
}
