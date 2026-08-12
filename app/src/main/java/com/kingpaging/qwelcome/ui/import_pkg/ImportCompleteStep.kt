@file:Suppress("FunctionNaming", "PackageNaming")

package com.kingpaging.qwelcome.ui.import_pkg

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.theme.LocalCyberColors

private const val DONE_BUTTON_WIDTH_FRACTION = 0.7f

@Composable
internal fun ImportCompleteStep(onDone: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier =
            Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(128.dp)
                    .clip(CircleShape),
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.content_desc_success),
                tint = LocalCyberColors.current.success,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.title_import_complete), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.text_import_complete_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(32.dp))
        NeonButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(DONE_BUTTON_WIDTH_FRACTION),
            glowColor = MaterialTheme.colorScheme.secondary,
        ) {
            Text(stringResource(R.string.action_done))
        }
    }
}
