# JSON Import/Export Feature Design — v1 Complete

> **Status:** ✅ ARCHIVED — v1 Implementation Complete  
> **Archived:** 2026-01-25  
> **Branch:** `feature/json-import-export`  
> **Future Work:** See [JSON_IMPORT_EXPORT_ROADMAP.md](../JSON_IMPORT_EXPORT_ROADMAP.md)

---

## Overview

JSON import/export turns Q Welcome from a personal app into something a whole crew can standardize around—without building a full backend. Techs can share templates via Slack, Teams, or any messaging app.

> **Safety Guarantee:** Template packs never overwrite Tech Profile unless explicitly chosen during import.

---

## Export Types

### 1) Template Pack (Team Sharing)

What a lead would post in Slack. Contains:

- Templates (one or many)
- Optional defaults (which template to use)
- Metadata (creator, notes)

**No techProfile** — prevents "oops I imported your name" problem.

### 2) Full Backup (Personal Restore)

For when you get a new phone. Contains:

- Templates
- Tech Profile (name, title, dept)
- App preferences

---

## Schema Design v1

> **ID Strategy:** Templates use UUID v4 for merge safety (preventing collisions when teams share).
> An optional `slug` field provides human-readable hints for debugging and manual JSON edits.
> Timestamps use ISO 8601 format with UTC timezone (e.g., `2026-01-25T18:42:00Z`).

### Template Pack

```json
{
  "schemaVersion": 1,
  "kind": "template-pack",
  "exportedAt": "2026-01-25T18:42:00Z",
  "appVersion": "1.0.0",
  "meta": {
    "name": "Residential Pack",
    "createdBy": "Team Lead",
    "notes": "Standard welcome text for Residential installs."
  },
  "templates": [
    {
      "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
      "slug": "residential_welcome",
      "name": "Residential Welcome",
      "content": "Welcome, {{ customer_name }}! 🎉\n\n📶 Wi-Fi Network (SSID): {{ ssid }}\n🔑 Password: {{ password }}\n👤 Account Number: {{ account_number }}\n\n{{ tech_signature }}",
      "createdAt": "2026-01-20T08:00:00Z",
      "modifiedAt": "2026-01-25T10:00:00Z"
    }
  ],
  "defaults": {
    "defaultTemplateId": "d290f1ee-6c54-4b01-90e6-d701748f0851"
  }
}
```

### Full Backup

```json
{
  "schemaVersion": 1,
  "kind": "full-backup",
  "exportedAt": "2026-01-25T18:42:00Z",
  "appVersion": "1.0.0",
  "techProfile": {
    "name": "Jane Doe",
    "title": "Installation Technician",
    "dept": "Fiber Services"
  },
  "templates": [
    {
      "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
      "slug": "residential_welcome",
      "name": "Residential Welcome",
      "content": "...",
      "createdAt": "2026-01-20T08:00:00Z",
      "modifiedAt": "2026-01-25T10:00:00Z"
    }
  ],
  "defaults": {
    "defaultTemplateId": "d290f1ee-6c54-4b01-90e6-d701748f0851"
  },
  "settings": {
    "signatureEnabled": true
  }
}
```

---

## The `{{ tech_signature }}` Placeholder

Instead of hardcoding name/title/dept into every template:

```text
{{ tech_signature }}
```

The app replaces this with the current device's Tech Profile.

**Result:**

- ✅ Templates can be shared freely
- ✅ Everyone's signature stays local
- ✅ No accidental name leaks

**Signature Format:**

```text
John Smith
Field Tech, Network Services
```

---

## Import Behavior: Merge (Not Replace)

Avoid "replace everything" as default—accidental nukes are how devices get thrown across rooms.

### Template Merge Rules

For each template in import:

- If `id` doesn't exist locally → **Add**
- If `id` exists locally → **Ask what to do:**
  - Replace existing
  - Keep existing  
  - Save as copy (new ID auto-generated, name becomes `"Original Name (Copy)"`)

### Default Template Protection

- Only the **built-in app default** is protected (cannot edit/delete)
- Imported templates are **never protected**—even if source marked them as such
- Any `protected` flag in imported JSON is ignored (prevents "undeleteable template" attacks)
- **Reserved ID rule:** If imported template has `id == "default"`, it cannot replace the built-in; it will be imported as a copy with a new UUID

### Import Preview Checkboxes

- ✅ Import templates (default: checked)
- ☐ Import tech profile (default: unchecked)
- ☐ Import app settings (default: unchecked)

> **Settings import is merge-safe:** Only known keys are applied; unknown keys are ignored. This is not a destructive overwrite.

---

## Validation Rules

### Required Fields

- `schemaVersion` exists and is supported (currently: 1)
- `kind` is `template-pack` or `full-backup`
- `templates` is non-empty for packs

### Template Validation

- Each template has `id`, `name`, `content`
- Content length warning if > 2000 chars (may increase number of SMS/RCS segments)
- **Known placeholder validation** (warn but don't block):
  - `{{ customer_name }}`
  - `{{ ssid }}`
  - `{{ password }}`
  - `{{ account_number }}`
  - `{{ tech_signature }}` — **warn if missing** (team sharing works best with local signatures)
- **Unknown placeholder detection**: Warn if template contains `{{ something }}` that isn't a known placeholder (catches typos like `{{ cusomer_name }}`)

### Version Handling

- If `schemaVersion` is newer than app supports:
  - Show "Created by newer version" warning
  - Allow import of known fields, ignore unknown

---

## Import Preview UI

### Summary Section

- Pack name
- Quick risk assessment:
  - `New: X` templates
  - `Conflicts: Y` (requires choice)
  - `Will replace: Z`
- Whether it contains techProfile/settings

### Template List

For each template:

- Name + ID
- Short snippet preview (~120 chars)
- Status pill:
  - 🟢 "New"
  - 🟡 "Will replace existing"
  - 🔴 "Conflict" (requires choice)

### Action Buttons

- **Apply Selected** — imports checked items
- **Cancel** — discards import

---

## Implementation Phases

### Phase 1: Foundation ✅ COMPLETE

- [x] Add `kotlinx.serialization` dependency
- [x] Create `Template` data class with UUID, timestamps
- [x] Create `TemplatePack` and `FullBackup` models
- [x] Add `{{ tech_signature }}` placeholder to `MessageTemplate`
- [x] Update `SettingsStore` for multiple templates (JSON storage)
- [x] Update `SettingsViewModel` for new template system
- [x] Update `SettingsScreen` with backward compatibility
- [x] Update `CustomerIntakeViewModel` to use signature placeholder

**Files Created:**

- `data/Template.kt`
- `data/ExportModels.kt`

**Files Modified:**

- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `data/MessageTemplate.kt`
- `data/SettingsStore.kt`
- `viewmodel/settings/SettingsViewModel.kt`
- `viewmodel/CustomerIntakeViewModel.kt`
- `ui/settings/SettingsScreen.kt`

### Phase 2: Import/Export Repository ✅ COMPLETE

- [x] Create `ImportExportRepository` class
- [x] JSON serialization with pretty print
- [x] JSON parsing with validation
- [x] Error handling with descriptive messages
- [x] Schema version checking
- [x] Template conflict detection

**Files Created:**

- `data/ImportExportRepository.kt`

**Key Features:**

- `exportTemplatePack()` - Export selected templates as JSON
- `exportFullBackup()` - Export everything including tech profile
- `validateImport()` - Parse and validate JSON, detect conflicts
- `applyTemplatePack()` / `applyFullBackup()` - Apply with conflict resolution
- `ConflictResolution` enum: REPLACE, KEEP_EXISTING, SAVE_AS_COPY
- `ImportWarning` sealed class for non-blocking warnings

### Phase 3: Export UI ✅ COMPLETE

- [x] Export screen with two buttons:
  - "Export Template Pack"
  - "Export Full Backup"
- [x] Copy to clipboard functionality
- [x] Android share sheet integration
- [x] Success/error feedback

**Files Created:**

- `ui/export/ExportScreen.kt`
- `viewmodel/export/ExportViewModel.kt`

**Files Modified:**

- `di/CompositionLocals.kt` - Added LocalExportViewModel
- `viewmodel/factory/AppViewModelProvider.kt` - Added ExportViewModel factory
- `MainActivity.kt` - Screen navigation, ExportViewModel provision
- `ui/settings/SettingsScreen.kt` - Export button with unsaved changes guard

**Key Features:**

- Two export options: Template Pack (team sharing) and Full Backup (personal)
- JSON preview panel with syntax highlighting
- One-tap clipboard copy with visual feedback
- Android share sheet integration for Slack/Teams/Email
- Per-card loading spinner via `currentlyExportingType` state
- Unsaved changes guard prevents exporting stale data

### Phase 4: Import UI ✅ COMPLETE

- [x] Paste from clipboard input
- [x] Parse and validate JSON
- [x] Import Preview screen:
  - Summary section
  - Template list with status pills
  - Checkboxes for what to import
- [x] Conflict resolution dialog
- [x] Apply import with merge logic

**Files Created:**

- `ui/import_pkg/ImportScreen.kt`
- `viewmodel/import_pkg/ImportViewModel.kt`

**Files Modified:**

- `di/CompositionLocals.kt` - Added LocalImportViewModel
- `viewmodel/factory/AppViewModelProvider.kt` - Added ImportViewModel factory
- `MainActivity.kt` - Import screen navigation
- `ui/settings/SettingsScreen.kt` - Import button

**Key Features:**

- Step-by-step import flow: INPUT → VALIDATING → PREVIEW → APPLYING → COMPLETE
- Paste from clipboard functionality
- Template preview cards with status pills (New/Will Replace/Conflict)
- Selectable templates with checkboxes
- Conflict resolution options (Replace/Skip/Copy)
- Tech profile import option (unchecked by default)
- Warnings display for schema version, long content, missing placeholders
- Animated transitions between steps

### Phase 5: File-Based Import/Export ✅ COMPLETE

- [x] Export to `.json` file (`ACTION_CREATE_DOCUMENT`)
- [x] Import from `.json` file (`ACTION_OPEN_DOCUMENT`)
- [x] File picker integration

**Files Modified:**

- `viewmodel/export/ExportViewModel.kt` - Added file save events and state
- `viewmodel/import_pkg/ImportViewModel.kt` - Added file load events
- `ui/export/ExportScreen.kt` - Added "Save to File" button with `CreateDocument` launcher
- `ui/import_pkg/ImportScreen.kt` - Added "Load from File" button with `OpenDocument` launcher

**Key Features:**

- "Save to File" button on Export screen (after generating JSON)
- Suggested filenames: `q-welcome-templates.json` / `q-welcome-backup.json`
- "Load from File" button on Import screen (alongside Paste)
- Uses Android Storage Access Framework (SAF) for cross-device compatibility
- Supports both `application/json` and `text/plain` MIME types for import
- Toast feedback for file save/load success

### Phase 6: Multi-Template UI ✅ COMPLETE

- [x] Template list/management screen
- [x] Template selector dropdown on main screen
- [x] Create/Edit/Delete template actions
- [x] Duplicate template functionality

**Files Created:**

- `viewmodel/templates/TemplateListViewModel.kt` - Full CRUD operations for templates
- `ui/templates/TemplateListScreen.kt` - Template management UI with cards

**Files Modified:**

- `di/CompositionLocals.kt` - Added `LocalTemplateListViewModel`
- `viewmodel/factory/AppViewModelProvider.kt` - Added factory for `TemplateListViewModel`
- `MainActivity.kt` - Added `TemplateList` screen and navigation wiring
- `ui/settings/SettingsScreen.kt` - Added "Manage Templates" button
- `ui/CustomerIntakeScreen.kt` - Added template selector dropdown

**Key Features:**

- **Template List Screen:** Card-based view of all templates with active indicator
- **Template Selector:** Dropdown on main screen for quick template switching
- **CRUD Operations:** Create, Edit, Delete, Duplicate templates
- **Default Protection:** Built-in "Default Template" cannot be edited or deleted
- **Toast Notifications:** Feedback for all template operations
- **Cyberpunk Theming:** Consistent with app's visual style

---

## Technical Notes

### Why kotlinx.serialization?

```kotlin
val json = Json {
    ignoreUnknownKeys = true  // Forward compatibility
    isLenient = true          // Tolerates trailing commas, unquoted strings (clipboard UX)
    prettyPrint = true        // Human readable for sharing
}
```

### ID Strategy

Template IDs use **UUID v4** (generated via `UUID.randomUUID().toString()`). This prevents collisions when importing templates from different teams or devices.

- IDs are **never shown in UI**—users see `name` only
- UUIDs ensure merge safety across packs from different sources
- The built-in default template uses the reserved ID `"default"`

### Slug Generation Rules

The optional `slug` field is auto-generated from `name` for human-readable debugging:

1. Lowercase the name
2. Replace non-alphanumeric chars with `_`
3. Collapse consecutive `_` and trim edges
4. Truncate to 50 characters
5. If result is empty, use `"template_<8-char-uuid>"`

**Reserved slugs:** `default`, `null`, `undefined` — if generated slug matches, append UUID suffix.

### Defaults Fallback Behavior

If `defaults.defaultTemplateId` references a template ID not present in `templates`:

- **Import:** Ignore the missing reference; keep current local default
- **Preview:** Show warning: "Default template missing; keeping local default"

### Timestamp Format

All timestamps **must be ISO 8601 UTC** format:

```text
2026-01-25T18:42:00Z
```

- `createdAt`: Set once when template is first created
- `modifiedAt`: Updated on every edit
- `exportedAt`: Set at export time

In Kotlin, stored as `String` but always in ISO 8601 UTC format for sorting and display ("modified 3 days ago").

### Storage Strategy

- **DataStore:** Settings + tech profile + templates JSON
- **Soft limit:** ~50 templates recommended; warn user if approaching limit
- **Room (future):** If we add customer history, template versioning, etc.

### Key Data Classes

```kotlin
// Template with full metadata (UUID + optional slug)
@Serializable
data class Template(
    val id: String,           // UUID v4
    val slug: String? = null, // Human-readable hint (auto-generated from name)
    val name: String,
    val content: String,
    val createdAt: String,    // ISO 8601 UTC
    val modifiedAt: String    // ISO 8601 UTC
)

// Team sharing format
@Serializable
data class TemplatePack(
    val schemaVersion: Int,
    val kind: String,
    val templates: List<Template>,
    val defaults: ExportDefaults,
    // ... metadata
)

// Personal backup format
@Serializable
data class FullBackup(
    val schemaVersion: Int,
    val kind: String,
    val techProfile: ExportedTechProfile,
    val templates: List<Template>,
    val defaults: ExportDefaults,
    val settings: ExportedSettings?,
    // ... 
)

// Normalized defaults object
@Serializable
data class ExportDefaults(
    val defaultTemplateId: String? = null
)

// Settings that can be exported/imported (curated subset, not full app state)
@Serializable
data class ExportedSettings(
    val signatureEnabled: Boolean = true
    // Future: dedupeWindowSeconds: Int
    // Future: lastUsedTemplateId: String?
)
```

---

## Commit History

| Date | Commit | Description |
| ------------ | ----------- | ---------------------------------- |
| 2026-01-25 | `901a039` | Phase 1: Foundation complete |
| 2026-01-25 | `313aeb5` | Phase 1: Code review fixes |
| 2026-01-25 | `612ce9f` | Phase 2: ImportExportRepository |
| 2026-01-25 | `3ec5663` | Phase 3: Export UI complete |
| 2026-01-25 | `1881493` | Phase 4: Import UI complete |
| 2026-01-25 | `97201aa` | Phase 5: File-based import/export |
| 2026-01-25 | `0bc22e2` | Phase 6: Multi-Template UI |

---

## Related Files

- [JSON_IMPORT_EXPORT_ROADMAP.md](../JSON_IMPORT_EXPORT_ROADMAP.md) — Future enhancements
- [ANDROID_APP_PLAN.md](../ANDROID_APP_PLAN.md) — Overall app architecture
- [Template.kt](../../app/src/main/java/com/kingpaging/qwelcome/data/Template.kt) — Template data class
- [ExportModels.kt](../../app/src/main/java/com/kingpaging/qwelcome/data/ExportModels.kt) — Export schema classes
- [ImportExportRepository.kt](../../app/src/main/java/com/kingpaging/qwelcome/data/ImportExportRepository.kt) — Import/export logic
