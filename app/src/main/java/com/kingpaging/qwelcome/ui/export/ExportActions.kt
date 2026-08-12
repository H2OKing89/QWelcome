package com.kingpaging.qwelcome.ui.export

data class ExportActions(
    val onBack: () -> Unit,
    val onTemplatePackRequested: () -> Unit,
    val onFullBackupRequested: () -> Unit,
    val onShareToPackageRequested: (String) -> Unit,
    val onCopy: () -> Unit,
    val onShareRequested: () -> Unit,
    val onSaveToFileRequested: () -> Unit,
    val onToggleTemplateSelection: (String) -> Unit,
    val onToggleSelectAll: () -> Unit,
    val onDismissTemplateSelection: () -> Unit,
    val onExportSelectedTemplates: () -> Unit,
)
