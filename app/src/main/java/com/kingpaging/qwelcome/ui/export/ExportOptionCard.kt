@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.export

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.ui.theme.LocalDarkTheme
import com.kingpaging.qwelcome.util.rememberHapticFeedback

@Composable
internal fun ExportOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val isDark = LocalDarkTheme.current
    val haptic = rememberHapticFeedback()

    Card(
        onClick = {
            haptic()
            onClick()
        },
        enabled = !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isDark) {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = if (isDark) 0.dp else 2.dp,
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        ExportOptionCardContent(title, description, icon, iconTint, isLoading)
    }
}

@Composable
private fun ExportOptionCardContent(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    isLoading: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExportOptionIcon(title, icon, iconTint, isLoading)

        Spacer(Modifier.width(16.dp))

        ExportOptionText(title, description)
    }
}

@Composable
private fun ExportOptionIcon(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    isLoading: Boolean,
) {
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = iconTint,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun RowScope.ExportOptionText(
    title: String,
    description: String,
) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
