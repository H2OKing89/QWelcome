package com.kingpaging.qwelcome.ui.templates

import android.content.Context
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListEvent

internal fun TemplateListEvent.toTemplateErrorMessage(context: Context): String? =
    when (this) {
        is TemplateListEvent.Error -> message
        is TemplateListEvent.TemplateSelectionBlocked ->
            context.getString(
                R.string.error_template_cannot_use,
                template.name,
                missingPlaceholders.joinToString(", "),
            )
        else -> null
    }
