@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.PrivacySettings

@Composable
internal fun SettingsPrivacySection(
    privacySettings: PrivacySettings?,
    onSetCrashReportingEnabled: (Boolean) -> Unit,
    onSetScreenCaptureProtectionEnabled: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_privacy_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    PrivacySettingRow(
        title = stringResource(R.string.label_crash_reporting),
        description = stringResource(R.string.settings_crash_reporting_summary),
        checked = privacySettings?.crashReportingEnabled ?: false,
        enabled = privacySettings != null,
        onCheckedChange = onSetCrashReportingEnabled,
    )
    PrivacySettingRow(
        title = stringResource(R.string.label_screen_capture_protection),
        description = stringResource(R.string.settings_capture_protection_summary),
        checked = privacySettings?.screenCaptureProtectionEnabled ?: false,
        enabled = privacySettings != null,
        onCheckedChange = onSetScreenCaptureProtectionEnabled,
    )
}

@Composable
private fun PrivacySettingRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val status =
        stringResource(
            when {
                !enabled -> R.string.settings_status_loading
                checked -> R.string.settings_status_on
                else -> R.string.settings_status_off
            },
        )
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 112.dp)
                    .semantics(mergeDescendants = true) { stateDescription = status }
                    .toggleable(
                        value = checked,
                        enabled = enabled,
                        role = Role.Switch,
                        onValueChange = onCheckedChange,
                    ).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (checked && enabled) colors.primary else colors.onSurfaceVariant,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}
