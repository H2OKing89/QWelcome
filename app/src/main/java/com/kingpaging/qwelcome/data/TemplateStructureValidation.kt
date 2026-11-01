package com.kingpaging.qwelcome.data

import androidx.annotation.StringRes
import com.kingpaging.qwelcome.R

internal object TemplateStructureValidation {
    enum class StructureError(
        @param:StringRes val messageResId: Int,
    ) {
        BLANK_ID(R.string.error_import_blank_template_id),
        RESERVED_ID(R.string.error_import_reserved_template_id),
        DUPLICATE_ID(R.string.error_import_duplicate_template_id),
        BLANK_NAME(R.string.error_import_blank_template_name),
        BLANK_CONTENT(R.string.error_import_blank_template_content),
    }

    fun findError(templates: List<Template>): StructureError? {
        val ids = mutableSetOf<String>()
        return templates.firstNotNullOfOrNull { template ->
            when {
                template.id.isBlank() -> StructureError.BLANK_ID
                template.id == DEFAULT_TEMPLATE_ID || template.id == NEW_TEMPLATE_ID -> StructureError.RESERVED_ID
                !ids.add(template.id) -> StructureError.DUPLICATE_ID
                template.name.isBlank() -> StructureError.BLANK_NAME
                template.content.isBlank() -> StructureError.BLANK_CONTENT
                else -> null
            }
        }
    }

    fun requireValidUserTemplates(templates: List<Template>) {
        require(findError(templates) == null) { "Invalid user template structure" }
    }
}
