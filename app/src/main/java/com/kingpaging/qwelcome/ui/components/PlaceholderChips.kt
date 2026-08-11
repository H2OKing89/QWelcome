@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.ui.theme.LocalDarkTheme

@Composable
fun PlaceholderChip(
    text: String,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false
) {
    val isDark = LocalDarkTheme.current
    val colorScheme = MaterialTheme.colorScheme
    val chipColor = if (isRequired) colorScheme.secondary else colorScheme.tertiary

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = if (isDark) {
            chipColor.copy(alpha = 0.15f)
        } else {
            chipColor.copy(alpha = 0.1f)
        },
        border = BorderStroke(
            width = 0.5.dp,
            color = chipColor.copy(alpha = if (isDark) 0.4f else 0.3f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = chipColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaceholderChipsRow(
    placeholders: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        placeholders.forEach { (placeholder, _) ->
            PlaceholderChip(
                text = placeholder,
                isRequired = placeholder in Template.REQUIRED_PLACEHOLDERS
            )
        }
    }
}

object PlaceholderLabels {
    private val shortLabels = mapOf(
        "{{ customer_name }}" to "Customer",
        "{{ ssid }}" to "SSID",
        "{{ password }}" to "Password",
        "{{ account_number }}" to "Account #",
        "{{ tech_signature }}" to "Signature"
    )

    fun getShortLabel(placeholder: String): String =
        shortLabels[placeholder] ?: placeholder.removePrefix("{{ ").removeSuffix(" }}")
}

@Composable
fun InteractivePlaceholderChip(
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false
) {
    val isDark = LocalDarkTheme.current
    val colorScheme = MaterialTheme.colorScheme
    val chipColor = colorScheme.secondary
    val haptic = LocalHapticFeedback.current
    val containerAlpha = if (isRequired) {
        if (isDark) 0.22f else 0.16f
    } else {
        if (isDark) 0.12f else 0.08f
    }
    val borderAlpha = if (isRequired) 1f else 0.6f
    val borderWidth = if (isRequired) 1.5.dp else 1.dp
    val textAlpha = if (isRequired) 1f else 0.8f

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = chipColor.copy(alpha = containerAlpha),
        border = BorderStroke(
            width = borderWidth,
            color = chipColor.copy(alpha = borderAlpha)
        )
    ) {
        Text(
            text = PlaceholderLabels.getShortLabel(placeholder),
            style = MaterialTheme.typography.labelSmall,
            color = chipColor.copy(alpha = textAlpha),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollapsiblePlaceholderChips(
    placeholders: List<Pair<String, String>>,
    onChipClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    collapsedMaxRows: Int = 2,
    showOverflowToggle: Boolean = true
) {
    var isExpanded by remember { mutableStateOf(false) }
    val overflow = if (showOverflowToggle && !isExpanded && collapsedMaxRows > 0) {
        FlowRowOverflow.expandIndicator {
            PlaceholderOverflowToggle(
                isExpanded = false,
                onClick = { isExpanded = true }
            )
        }
    } else {
        FlowRowOverflow.Clip
    }

    Column(modifier = modifier) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            maxLines = if (!isExpanded && collapsedMaxRows > 0) collapsedMaxRows else Int.MAX_VALUE,
            overflow = overflow
        ) {
            placeholders.forEach { (placeholder, _) ->
                InteractivePlaceholderChip(
                    placeholder = placeholder,
                    onClick = { onChipClick(placeholder) },
                    isRequired = placeholder in Template.REQUIRED_PLACEHOLDERS
                )
            }
        }

        if (showOverflowToggle && isExpanded && collapsedMaxRows > 0) {
            PlaceholderOverflowToggle(
                isExpanded = true,
                onClick = { isExpanded = false },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun PlaceholderOverflowToggle(
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NeonButton(
        onClick = onClick,
        style = NeonButtonStyle.TERTIARY,
        modifier = modifier
    ) {
        Text(
            if (isExpanded) {
                stringResource(R.string.action_less)
            } else {
                stringResource(R.string.action_more)
            },
            style = MaterialTheme.typography.labelSmall
        )
        Icon(
            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) {
                stringResource(R.string.content_desc_show_less_placeholders)
            } else {
                stringResource(R.string.content_desc_show_more_placeholders)
            },
            modifier = Modifier.size(16.dp)
        )
    }
}
