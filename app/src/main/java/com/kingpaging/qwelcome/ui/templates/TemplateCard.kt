package com.kingpaging.qwelcome.ui.templates

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.ui.theme.LocalDarkTheme
import com.kingpaging.qwelcome.util.rememberHapticFeedback

private val PreviewWhitespace = Regex("\\s+")
private const val MAX_VISIBLE_TAGS = 2

internal fun compactTemplatePreview(content: String): String = content
    .replace(PreviewWhitespace, " ")
    .trim()

@OptIn(ExperimentalFoundationApi::class)
@Suppress("FunctionNaming", "LongMethod")
@Composable
internal fun TemplateCard(
    template: Template,
    isActive: Boolean,
    isDefault: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val colors = MaterialTheme.colorScheme
    val useDescription = stringResource(R.string.content_desc_use_named_template, template.name)
    val previewDescription = stringResource(R.string.content_desc_preview_named_template, template.name)
    val isValid = remember(template.content) {
        Template.hasRequiredPlaceholders(template.content)
    }
    val preview = remember(template.content) { compactTemplatePreview(template.content) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val containerColor = when {
        isPressed -> colors.primaryContainer.copy(alpha = if (isDark) 0.36f else 0.55f)
        isFocused || isHovered -> colors.surfaceVariant.copy(alpha = if (isDark) 0.82f else 0.65f)
        isDark -> colors.surface.copy(alpha = 0.72f)
        else -> colors.surface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = useDescription
                selected = isActive
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClickLabel = useDescription,
                onLongClickLabel = previewDescription,
                onClick = onSelect,
                onLongClick = onPreview
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 0.5.dp,
            color = if (isActive || isFocused) {
                colors.primary.copy(alpha = 0.9f)
            } else {
                colors.outlineVariant.copy(alpha = if (isDark) 0.45f else 0.75f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            TemplateCardHeader(
                template = template,
                isActive = isActive,
                isDefault = isDefault,
                isValid = isValid,
                onPreview = onPreview,
                onEdit = onEdit,
                onRename = onRename,
                onDuplicate = onDuplicate,
                onDelete = onDelete
            )

            Text(
                text = preview,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Suppress("FunctionNaming", "LongParameterList")
@Composable
private fun TemplateCardHeader(
    template: Template,
    isActive: Boolean,
    isDefault: Boolean,
    isValid: Boolean,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isDefault) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.content_desc_builtin_template),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(16.dp)
                    )
                }
            }
            TemplateTagMetadata(tags = template.tags)
        }

        if (!isValid) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = stringResource(R.string.content_desc_invalid_template),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(18.dp)
            )
        }

        if (isActive) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.content_desc_active_template),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(20.dp)
            )
        }

        TemplateActionsMenu(
            templateName = template.name,
            isDefault = isDefault,
            isValid = isValid,
            onPreview = onPreview,
            onEdit = onEdit,
            onRename = onRename,
            onDuplicate = onDuplicate,
            onDelete = onDelete
        )
    }
}

@Suppress("FunctionNaming")
@Composable
private fun TemplateActionsMenu(
    templateName: String,
    isDefault: Boolean,
    isValid: Boolean,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = {
            haptic()
            expanded = true
        }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(
                    R.string.content_desc_template_actions_named,
                    templateName
                ),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            val dismissAndRun: (() -> Unit) -> Unit = { action ->
                expanded = false
                action()
            }
            if (isDefault) {
                DefaultTemplateMenuItems(
                    onCustomize = { dismissAndRun(onEdit) },
                    onPreview = { dismissAndRun(onPreview) }
                )
            } else {
                UserTemplateMenuItems(
                    isValid = isValid,
                    onEdit = { dismissAndRun(onEdit) },
                    onRename = { dismissAndRun(onRename) },
                    onPreview = { dismissAndRun(onPreview) },
                    onDuplicate = { dismissAndRun(onDuplicate) },
                    onDelete = { dismissAndRun(onDelete) }
                )
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun DefaultTemplateMenuItems(
    onCustomize: () -> Unit,
    onPreview: () -> Unit
) {
    TemplateActionMenuItem(
        labelRes = R.string.action_customize_copy,
        icon = Icons.Default.Edit,
        onClick = onCustomize
    )
    TemplateActionMenuItem(
        labelRes = R.string.action_preview_template,
        icon = Icons.Default.Visibility,
        onClick = onPreview
    )
}

@Suppress("FunctionNaming", "LongParameterList")
@Composable
private fun UserTemplateMenuItems(
    isValid: Boolean,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onPreview: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    TemplateActionMenuItem(
        labelRes = if (isValid) R.string.action_edit_template else R.string.action_fix,
        icon = if (isValid) Icons.Default.Edit else Icons.Default.Warning,
        onClick = onEdit
    )
    TemplateActionMenuItem(R.string.action_rename_template, Icons.Default.Edit, onRename)
    TemplateActionMenuItem(R.string.action_preview_template, Icons.Default.Visibility, onPreview)
    TemplateActionMenuItem(R.string.action_duplicate_template, Icons.Default.ContentCopy, onDuplicate)
    HorizontalDivider()
    TemplateActionMenuItem(
        labelRes = R.string.action_delete_template,
        icon = Icons.Default.Delete,
        onClick = onDelete,
        tint = MaterialTheme.colorScheme.error
    )
}

@Suppress("FunctionNaming")
@Composable
private fun TemplateActionMenuItem(
    @StringRes labelRes: Int,
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = LocalContentColor.current
) {
    DropdownMenuItem(
        text = { Text(text = stringResource(labelRes), color = tint) },
        leadingIcon = { Icon(imageVector = icon, contentDescription = null, tint = tint) },
        onClick = onClick
    )
}

@Suppress("FunctionNaming")
@Composable
private fun TemplateTagMetadata(tags: List<String>) {
    val overflowCount = tags.size - MAX_VISIBLE_TAGS
    val visibleTags = tags.take(MAX_VISIBLE_TAGS).joinToString(" • ")
    val summary = when {
        tags.isEmpty() -> stringResource(R.string.text_no_tags)
        overflowCount > 0 -> "$visibleTags  ${stringResource(R.string.text_more_tags, overflowCount)}"
        else -> visibleTags
    }
    Text(
        text = summary,
        style = MaterialTheme.typography.labelSmall,
        color = if (tags.isEmpty()) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.secondary
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(top = 2.dp)
            .heightIn(min = 16.dp)
    )
}

