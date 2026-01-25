# JSON Import/Export Feature Design

> **Branch:** `feature/json-import-export`  
> **Status:** Phase 1 Complete ✅  
> **Last Updated:** 2026-01-25

---

## Overview

JSON import/export turns Q Welcome from a personal app into something a whole crew can standardize around—without building a full backend. Techs can share templates via Slack, Teams, or any messaging app.

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

### Template Pack

```json
{
  "schemaVersion": 1,
  "kind": "template-pack",
  "exportedAt": "2026-01-25T18:42:00Z",
  "appVersion": "1.0",
  "meta": {
    "name": "ALLO Residential Pack",
    "createdBy": "Quentin King",
    "notes": "Standard welcome text for Residential installs."
  },
  "defaults": {
    "defaultTemplateId": "residential_default"
  },
  "templates": [
    {
      "id": "residential_default",
      "name": "Residential Welcome",
      "content": "Welcome to ALLO, {{ customer_name }}! 🎉\n\n📶 Wi-Fi Network (SSID): {{ ssid }}\n🔑 Password: {{ password }}\n👤 Account Number: {{ account_number }}\n\nSupport: 1-866-481-2556\n\n{{ tech_signature }}",
      "createdAt": "2026-01-20T08:00:00Z",
      "modifiedAt": "2026-01-25T10:00:00Z"
    }
  ]
}
```

### Full Backup

```json
{
  "schemaVersion": 1,
  "kind": "full-backup",
  "exportedAt": "2026-01-25T18:42:00Z",
  "appVersion": "1.0",
  "techProfile": {
    "name": "Quentin King",
    "title": "Installation Technician 1",
    "dept": "ALLO Fiber | Residential"
  },
  "templates": [...],
  "defaultTemplateId": "residential_default"
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
  - Save as copy (new ID auto-generated)

### Import Preview Checkboxes

- ✅ Import templates (default: checked)
- ☐ Import tech profile (default: unchecked)
- ☐ Import app settings (default: unchecked)

---

## Validation Rules

### Required Fields

- `schemaVersion` exists and is supported (currently: 1)
- `kind` is `template-pack` or `full-backup`
- `templates` is non-empty for packs

### Template Validation

- Each template has `id`, `name`, `content`
- Content length warning if > 2000 chars
- Placeholder warnings (don't block, just warn):
  - `{{ customer_name }}`
  - `{{ ssid }}`
  - `{{ password }}`
  - `{{ account_number }}`
  - `{{ tech_signature }}`

### Version Handling

- If `schemaVersion` is newer than app supports:
  - Show "Created by newer version" warning
  - Allow import of known fields, ignore unknown

---

## Import Preview UI

### Summary Section

- Pack name
- Number of templates
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

### Phase 2: Import/Export Repository ⏳ NEXT

- [ ] Create `ImportExportRepository` class
- [ ] JSON serialization with pretty print
- [ ] JSON parsing with validation
- [ ] Error handling with descriptive messages
- [ ] Schema version checking
- [ ] Template conflict detection

### Phase 3: Export UI

- [ ] Export screen with two buttons:
  - "Export Template Pack"
  - "Export Full Backup"
- [ ] Copy to clipboard functionality
- [ ] Android share sheet integration
- [ ] Success/error feedback

### Phase 4: Import UI

- [ ] Paste from clipboard input
- [ ] Parse and validate JSON
- [ ] Import Preview screen:
  - Summary section
  - Template list with status pills
  - Checkboxes for what to import
- [ ] Conflict resolution dialog
- [ ] Apply import with merge logic

### Phase 5: File-Based Import/Export (Future)

- [ ] Export to `.json` file (`ACTION_CREATE_DOCUMENT`)
- [ ] Import from `.json` file (`ACTION_OPEN_DOCUMENT`)
- [ ] File picker integration

### Phase 6: Multi-Template UI (Future)

- [ ] Template list/management screen
- [ ] Template selector dropdown on main screen
- [ ] Create/Edit/Delete template actions
- [ ] Duplicate template functionality

---

## Future Ideas 🚀

### "Loadout" Concept

A loadout is a quick-switch preset:

- Selected default template
- Signature enabled/disabled
- Optional "include support line" toggle

Techs can switch "Residential" ↔ "Business" like changing weapons in a game.

### QR Code Export

Cool but JSON gets big fast. Consider:

- Compressed format
- URL shortener integration
- Only for single templates

### Tags/Categories

```json
{
  "id": "biz_welcome",
  "name": "Business Welcome",
  "tags": ["business", "install"],
  "content": "..."
}
```

Then filter by tag in template selector.

---

## Technical Notes

### Why kotlinx.serialization?

```kotlin
val json = Json {
    ignoreUnknownKeys = true  // Forward compatibility
    prettyPrint = true        // Human readable for sharing
}
```

### Storage Strategy

- **DataStore:** Settings + tech profile + templates JSON
- **Room (future):** If we add customer history, template versioning, etc.

### Key Data Classes

```kotlin
// Template with full metadata
@Serializable
data class Template(
    val id: String,
    val name: String,
    val content: String,
    val createdAt: String,
    val modifiedAt: String
)

// Team sharing format
@Serializable
data class TemplatePack(
    val schemaVersion: Int,
    val kind: String,
    val templates: List<Template>,
    // ... metadata
)

// Personal backup format  
@Serializable
data class FullBackup(
    val schemaVersion: Int,
    val kind: String,
    val techProfile: ExportedTechProfile,
    val templates: List<Template>,
    // ... settings
)
```

---

## Commit History

| Date | Commit | Description |
|------|--------|-------------|
| 2026-01-25 | `901a039` | Phase 1: Foundation complete |

---

## Related Files

- [ANDROID_APP_PLAN.md](./ANDROID_APP_PLAN.md) - Overall app architecture
- [Template.kt](../app/src/main/java/com/example/allowelcome/data/Template.kt) - Template data class
- [ExportModels.kt](../app/src/main/java/com/example/allowelcome/data/ExportModels.kt) - Export schema classes
