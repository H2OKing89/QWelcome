# JSON Import/Export — Future Roadmap

> **Status:** Active Roadmap  
> **Created:** 2026-01-25  
> **Updated:** 2026-02-11  
> **Prerequisite:** [JSON_IMPORT_EXPORT_v1_COMPLETE_2026-01-25.md](./JSON_IMPORT_EXPORT_v1_COMPLETE_2026-01-25.md)

---

## Overview

This document tracks future enhancements for the JSON Import/Export feature. The v1 implementation (Phases 1–6) is complete and documented in the archived design doc.

**What's Done (v1 + post-v1):**

- ✅ Template Pack export (team sharing)
- ✅ Full Backup export (personal restore)
- ✅ Clipboard + file-based import/export
- ✅ Conflict resolution (Replace/Keep/Copy)
- ✅ Multi-template management UI
- ✅ `{{ tech_signature }}` placeholder
- ✅ Tags/Categories (`Template.tags`) with filter chips + editor suggestions
- ✅ Template soft-limit warning banner (shows at >20 templates, dismissible per session)
- ✅ Recent share targets (remembered + quick share buttons)
- ✅ Legacy full-backup compatibility: old unknown keys (including former `loadouts`) are safely ignored on import

---

## Future Ideas 🚀

### 1) Loadout Concept (Removed)

This idea is intentionally removed. It duplicated the existing template selection flow and added UI noise.

**Decision:** Keep one switcher path: `TemplateSelector` dropdown + "Manage Templates".

---

### 2) QR Code Export

Cool but JSON gets big fast. Consider:

- Compressed format (gzip + base64)
- URL shortener integration (bit.ly/qwelcome-pack-abc123)
- Only for single templates (multi-template packs exceed QR capacity)

**When to prioritize:** If techs frequently share in-person (training sessions, ride-alongs).

---

### 3) Tags/Categories (Shipped)

```json
{
  "id": "biz_welcome",
  "name": "Business Welcome",
  "tags": ["business", "install"],
  "content": "..."
}
```

Tags are implemented and available in template management.

**Current behavior:**

- `tags: List<String>` on `Template`
- Filter chips on Template List screen
- Tag suggestions in Template Edit dialog (Residential, Business, Install, Repair, Troubleshooting)

---

### 4) Template Versioning / Revision Tracking

Track changes within a template over time:

```kotlin
data class TemplateRevision(
    val revision: Int,
    val content: String,
    val modifiedAt: String,
    val changeNote: String?
)
```

**Use cases:**

- "Undo" last edit
- Compare before/after
- Audit trail for compliance

**Storage:** Would likely require Room migration from DataStore.

---

## Schema v2 Candidates

Ideas for future schema versions (backward compatible with v1):

```json
{
  "schemaVersion": 2,
  "packageName": "com.kingpaging.qwelcome",  // Source app identifier
  "deviceName": "Quentin-S22U",              // Optional: who exported this?
  "revision": 3                               // Monotonic version for smarter merge
}
```

### New Fields

| Field | Purpose |
| ------- | --------- |
| `packageName` | Verify export came from Q Welcome (not a spoofed file) |
| `deviceName` | "Who made this?" context in group chat imports |
| `revision` | If imported revision > local revision, default to "replace" |
| `checksum` | Optional integrity check for file transfers |

### Migration Strategy

- v2 parser reads v1 files with defaults for new fields
- v1 parser (older app versions) ignores unknown fields via `ignoreUnknownKeys`
- Increment `EXPORT_SCHEMA_VERSION` constant when shipping v2

---

## Performance / Scale Enhancements

### Soft Limit Warning UI

Implemented:

- Show warning banner when template count > 20
- "You have X templates. Consider archiving unused ones."
- Dismissible for current session (not persisted)

Future:

- Optional: Archive feature (hide from selector, keep in storage)

### Large Pack Handling

If someone imports a 100-template pack:

- Paginate the preview UI
- "Select All / Deselect All" buttons
- Progress indicator during apply

---

## UX Polish Ideas

### Import Source Detection

Detect where JSON came from and customize UX:

| Source | Behavior |
| -------- | --------- |
| Clipboard | Current flow |
| `.json` file | Current flow |
| Deep link (`qwelcome://import?data=...`) | Auto-parse, skip input step |
| NFC tap | Future: instant import from another device |

### Export Sharing Shortcuts

Implemented:

- Recent share targets remembered
- Quick actions to share directly to recent packages

Future:

- Optional pinned share targets (e.g., Slack/Teams)

### Template Preview Enhancements

- Syntax highlighting for placeholders in preview
- "Preview with sample data" button (fills in example values)
- Character count / segment estimate

---

## Technical Debt / Cleanup

- [x] Add unit tests for `ImportExportRepository`
- [ ] Add UI tests for import/export flows
- [ ] Consider extracting JSON config to shared constant
- [ ] Profile DataStore performance with 50+ templates

---

## Related Files

- [JSON_IMPORT_EXPORT_v1_COMPLETE_2026-01-25.md](./JSON_IMPORT_EXPORT_v1_COMPLETE_2026-01-25.md) — Archived v1 design
- [ANDROID_APP_PLAN.md](./ANDROID_APP_PLAN.md) — Overall app architecture (archived snapshot)
- [Template.kt](../app/src/main/java/com/kingpaging/qwelcome/data/Template.kt)
- [ExportModels.kt](../app/src/main/java/com/kingpaging/qwelcome/data/ExportModels.kt)
- [ImportExportRepository.kt](../app/src/main/java/com/kingpaging/qwelcome/data/ImportExportRepository.kt)
