@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.PrivacySettings
import com.kingpaging.qwelcome.ui.components.NeonPanel

@Composable
internal fun SettingsPrivacySection(
    privacySettings: PrivacySettings?,
    onSetCrashReportingEnabled: (Boolean) -> Unit,
    onSetScreenCaptureProtectionEnabled: (Boolean) -> Unit
) {
    Text(
        stringResource(R.string.header_privacy),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )
    NeonPanel {
        PrivacySettingRow(
            title = stringResource(R.string.label_crash_reporting),
            description = stringResource(R.string.text_crash_reporting_description),
            checked = privacySettings?.crashReportingEnabled ?: false,
            enabled = privacySettings != null,
            onCheckedChange = onSetCrashReportingEnabled
        )
        Spacer(Modifier.height(16.dp))
        PrivacySettingRow(
            title = stringResource(R.string.label_screen_capture_protection),
            description = stringResource(R.string.text_screen_capture_protection_description),
            checked = privacySettings?.screenCaptureProtectionEnabled ?: false,
            enabled = privacySettings != null,
            onCheckedChange = onSetScreenCaptureProtectionEnabled
        )
    }

    Spacer(Modifier.height(16.dp))
}

@Composable
private fun PrivacySettingRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = title }
        )
    }
}
