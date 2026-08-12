@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.templates

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.kingpaging.qwelcome.R

@Composable
internal fun TemplateEditorTopBar(
    isNew: Boolean,
    canSave: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    val saveIconTint =
        if (canSave) {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.38f)
        }

    TopAppBar(
        title = {
            Text(
                text =
                    if (isNew) {
                        stringResource(R.string.title_create_template)
                    } else {
                        stringResource(R.string.title_edit_template)
                    },
                color = MaterialTheme.colorScheme.secondary,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.content_desc_back),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        },
        actions = {
            IconButton(
                onClick = onSave,
                enabled = canSave,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription =
                        stringResource(
                            if (isNew) R.string.action_create else R.string.action_save,
                        ),
                    tint = saveIconTint,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}
