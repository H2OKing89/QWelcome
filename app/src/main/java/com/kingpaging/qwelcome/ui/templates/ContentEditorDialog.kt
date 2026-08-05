@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.templates

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle

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
    LaunchedEffect(Unit) {
        contentFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.label_message),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_done)
                        )
                    }
                }

                PlaceholderChipsSection(
                    onInsertPlaceholder = onInsertPlaceholder
                )

                ContentEditorField(
                    contentState = contentState,
                    contentFocusRequester = contentFocusRequester,
                    contentInteractionSource = contentInteractionSource,
                    contentError = contentError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp, max = 460.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    NeonButton(
                        onClick = onDismissRequest,
                        glowColor = MaterialTheme.colorScheme.secondary,
                        style = NeonButtonStyle.TERTIARY
                    ) {
                        Text(stringResource(R.string.action_done))
                    }
                }
            }
        }
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
    BasicTextField(
        state = contentState,
        lineLimits = TextFieldLineLimits.MultiLine(
            minHeightInLines = 8,
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
                supportingText = if (contentError != null) {
                    {
                        Text(
                            text = stringResource(
                                R.string.error_template_missing_placeholders,
                                contentError
                            ),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    null
                },
                colors = OutlinedTextFieldDefaults.colors(
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
                ),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = hasContentError,
                        interactionSource = contentInteractionSource,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (hasContentError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.secondary
                            },
                            unfocusedBorderColor = if (hasContentError) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        )
                    )
                }
            )
        }
    )
}
