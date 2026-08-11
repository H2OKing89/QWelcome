# Maintainability and Scalability Roadmap

## Purpose

This document is the living plan for improving Q Welcome without rewriting working code. The app already has strong foundations: modern Compose UI, MVVM state management, Proto DataStore, typed navigation, privacy-aware Crashlytics, import/export services, release automation, and substantial automated tests.

The goal is to make feature ownership clearer, process restoration safer, and future changes easier to test. Each phase should remain independently buildable and releasable.

## Current Baseline

Audit date: 2026-08-10

- Production Kotlin files: 68
- Production Kotlin lines: approximately 13,500
- Unit test files: 22
- Unit test methods: 303
- Instrumentation test files: 9
- Instrumentation test methods: 73
- Existing modules: `app` and `proto`
- CI workflows: build/lint, release, CodeQL, and dependency review
- Largest production files:
  - `CustomerIntakeScreen.kt`: 776 lines
  - `NeonComponents.kt`: 751 lines
  - `ExportScreen.kt`: 712 lines
  - `SettingsScreen.kt`: 686 lines
  - `TemplateListViewModel.kt`: 586 lines

These numbers are indicators, not targets by themselves. A long file is a problem only when it mixes responsibilities or makes changes difficult to understand and test.

## Principles

1. Prefer incremental refactoring over a rewrite.
2. Preserve public behavior while changing ownership.
3. Add or adjust tests before risky structural changes.
4. Keep business rules out of composables.
5. Keep persistent operations atomic when users expect all-or-nothing behavior.
6. Add abstractions only when they remove demonstrated coupling or duplication.
7. Keep the current two-module structure until another module has a clear ownership or build-time benefit.
8. Keep manual dependency injection until its maintenance cost justifies a framework.

## Findings and Work Items

### P0: Correctness and Recovery

#### 1. Make full-backup restoration atomic

**Status:** Completed 2026-08-10

Before the 2026-08-10 change, `ImportApplyService.applyFullBackup()` saved templates, the technician profile, and active template selection through separate DataStore transactions. An exception or process stop between writes could leave a partially restored backup.

**Plan:**

- Add a single `SettingsStore` operation that applies all selected backup fields in one `DataStore.updateData` transaction.
- Preserve conflict-resolution ID mapping before entering the transaction.
- Verify active-template fallback rules in the same transaction.
- Add tests proving profile/template/default restoration succeeds together and invalid active IDs fall back safely.

**Acceptance criteria:**

- Full backup persistence uses one DataStore transaction.
- Template-pack-only import remains unchanged.
- Existing import/export tests pass.
- Tests cover importing with and without technician profile/default-template options.

#### 2. Make template-editor navigation restorable

**Status:** Complete

`Routes.TemplateEditor` carries the template ID, including the `NEW_TEMPLATE_ID` sentinel for creation. The destination-scoped `TemplateEditorViewModel` reloads the persisted template and restores draft fields through `SavedStateHandle`, so editor identity no longer lives in `TemplateListViewModel.uiState`.

**Plan:**

- Change the route to a serializable data class carrying a template ID or the `NEW_TEMPLATE_ID` sentinel.
- Create a destination-scoped `TemplateEditorViewModel` using `SavedStateHandle`.
- Load the template from persistence by route ID.
- Move editor fields, validation, save/update, and discard state out of `TemplateListViewModel`.
- Replace the `navigateToEditor` SharedFlow with the existing library effect stream carrying the duplicate ID.
- Move editor effects to `TemplateEditorViewModel`; `TemplateListEventOwner.EDITOR` has been removed.

**Acceptance criteria:**

- Opening an existing or new template is represented entirely by the route.
- Editor state survives activity/process recreation where supported by saved state.
- `TemplateListViewModel` owns list/filter/list-action behavior only.
- Editor and list ViewModels have separate focused tests.

### P1: Remove Duplication and Clarify Ownership

#### 3. Remove obsolete template APIs from SettingsViewModel

**Status:** Completed 2026-08-10

Before the 2026-08-10 cleanup, template management had moved to the template library, but `SettingsViewModel` still exposed template CRUD methods, an unused `allTemplates` state flow, and an `errorEvents` flow that no production UI collected. Tests preserved these obsolete APIs.

**Plan:**

- Keep `activeTemplate`, because Settings displays the current template name.
- Remove unused template CRUD and default-content APIs from `SettingsViewModel`.
- Remove `allTemplates` and `errorEvents` when no production caller remains.
- Remove or replace tests that only cover dead APIs.

**Acceptance criteria:**

- Settings owns technician profile, privacy settings, update checks, and the read-only active-template summary.
- No uncollected event flow remains.
- Settings tests cover only production behavior.

#### 4. Split the template list and editor responsibilities

**Status:** Complete

`TemplateListViewModel` now owns list loading, search, tag filters, selection, rename, duplicate, and delete. Editor state, validation, persistence, and effects live in the destination-scoped `TemplateEditorViewModel`.

**Plan:**

- Completed the editor extraction described in item 2.
- Keep template selection rules in the data/domain boundary.
- Keep library-specific rename, duplicate, delete, search, and filtering in `TemplateListViewModel`.
- Reassess whether Intake needs a small template-selection collaborator instead of sharing the entire list ViewModel.

**Acceptance criteria:**

- No editor state or editor navigation event exists in `TemplateListViewModel`.
- Event ownership no longer requires an `EDITOR` enum value.
- List tests do not initialize editor state.

#### 5. Introduce explicit Route/Screen boundaries

**Status:** Complete 2026-08-10

All top-level feature composables now have explicit Route/Screen boundaries. Route composables retrieve ViewModels through CompositionLocals and own state collection and effects, while screen composables render plain state through explicit callbacks.

Template Library, Editor, Settings, Import, Export, and Customer Intake use dedicated route files for ViewModel access, lifecycle-aware state collection, one-shot effects, and activity-result handling. Their screen composables accept plain UI state and callbacks, with instrumentation coverage that renders each screen without a ViewModel.

**Plan:**

- Introduce one route composable at a time, for example `SettingsRoute`.
- Route composables obtain ViewModels, collect state, handle one-shot effects, and supply navigation callbacks.
- Screen composables accept plain state and callbacks and contain rendering only.
- Keep navigation callbacks in `AppNavGraph`; they are simple and testable.
- Migrate feature by feature, not all screens at once.

**Acceptance criteria:**

- Pure screen composables can render with fake state without CompositionLocal ViewModels.
- Route files clearly expose each feature's dependencies.
- Navigation remains type-safe.

#### 6. Simplify dependency construction

**Status:** Not started

`AppViewModelProvider` stores process-wide static dependencies and requires `resetForTesting()` throughout tests. This works at the current scale but spreads global reset knowledge into unrelated tests.

**Plan:**

- First move shared dependency ownership into an application-level `AppContainer`.
- Inject the container or focused factories into route-level ViewModel creation.
- Remove static mutable singleton fields and unrelated test resets.
- Consider Hilt only if constructor/factory wiring becomes a demonstrated maintenance problem after route scoping.

**Acceptance criteria:**

- Tests can construct dependencies without resetting global static state.
- Production has one documented dependency graph.
- No Activity context is retained by process-wide objects.

### P2: UI Decomposition

#### 7. Split large feature files by responsibility

**Status:** In progress; Customer Intake completed 2026-08-10

Several files contain multiple independently meaningful UI sections. File extraction should follow ownership, not an arbitrary line limit.

Customer Intake now keeps route wiring and screen-level presentation state in `CustomerIntakeScreen.kt`; its template selector, form and advanced Wi-Fi controls, send actions, and QR action live in focused feature files. A dedicated QR action test covers the extracted component boundary.

**Initial candidates:**

- Customer intake: template selector, customer fields, WiFi fields, send actions, and QR section.
- Settings: profile section, privacy section, data-management navigation, and updater section.
- Export/import: option state, selection dialogs, confirmation/result sections, and file actions.
- `NeonComponents.kt`: fields, buttons, panels, banners, and placeholder components.
- QR sheet: presentation, bitmap generation, gallery persistence, and sharing.

**Rules:**

- Keep feature-specific components in their feature package.
- Put only genuinely reusable controls in `ui/components`.
- Do not move business logic into extracted composables.
- Add focused previews or Compose tests when extraction creates a stable reusable component.

**Acceptance criteria:**

- Top-level screen flow is understandable without scrolling through component implementations.
- Extracted components have narrow inputs and explicit callbacks.
- No behavior changes are introduced solely for file-size reduction.

#### 8. Consolidate editor text ownership carefully

**Status:** Not started; evaluate during editor extraction

The editor synchronizes Compose `TextFieldState` with ViewModel `contentText` using two effects. This is necessary for selection-aware editing today, but it adds synchronization complexity.

**Plan:**

- During `TemplateEditorViewModel` extraction, choose one authoritative persisted text representation.
- Keep cursor/selection as UI state unless restoration requires it.
- Avoid bidirectional updates when a one-way snapshot at save time is sufficient.
- Preserve placeholder insertion and IME behavior with focused tests.

### P3: Persistence and Domain Boundaries

#### 9. Keep Proto DataStore, split interfaces only when useful

**Status:** Monitor

`SettingsStore` handles privacy, technician profile, templates, and recent share targets. This is acceptable for one small Proto document because DataStore guarantees serialized updates. Splitting it into multiple wrappers over the same file would not create independent persistence.

**Trigger for change:**

- Constructor dependencies need only one domain but test setup remains broad.
- Template operations continue to dominate the store.
- Another storage technology or synchronization source is introduced.

**Possible incremental change:**

- Define narrow interfaces such as `TemplateStore`, `ProfileStore`, and `PrivacyStore` implemented by the existing `SettingsStore`.
- Do not create separate DataStore instances for the same file.

#### 10. Reassess storage only if product scope changes

**Status:** Monitor

Proto DataStore remains appropriate for settings and approximately 20 templates. Consider Room only if Q Welcome adds hundreds of templates, customer/job history, relational queries, pagination, or cloud synchronization.

### P4: Events, Errors, and Lifecycle

#### 11. Standardize one-shot event behavior per feature

**Status:** Not started

The project uses several SharedFlow configurations: unbuffered, extra-buffered, and replayed flows with manual replay-cache resets. These may all be intentional, but the contract is not documented consistently.

**Plan:**

- Document whether each event may be dropped while no screen is active.
- Use state for outcomes that must survive recreation.
- Use one-shot effects only for transient actions such as toasts and external intents.
- Remove event owner routing as ViewModels become destination scoped.
- Prefer feature-specific event types over a single application-wide event hierarchy.

#### 12. Keep lifecycle ownership explicit

**Status:** Not started

The intake inactivity timer is coordinated by Activity lifecycle callbacks and ViewModel methods. It is tested, but future lifecycle-sensitive behavior should not be added to `MainActivity` by default.

**Plan:**

- Preserve current behavior while route/ViewModel scoping changes.
- Add an integration test for background timeout and resume.
- Consider `ProcessLifecycleOwner` only if the rule is truly application-wide.

### P5: Tests and Continuous Integration

#### 13. Run instrumentation tests in automation

**Status:** Not started

The repository has 73 instrumentation test methods, and the suite compiles, but `android.yml` does not execute it.

**Plan:**

- Add a Gradle-managed emulator or emulator-runner CI job.
- Start with a smoke subset on pull requests.
- Run the complete suite nightly or before release.
- Cache Android system images where practical.

**Acceptance criteria:**

- At least intake, template list, template editor, and DataStore migration smoke tests run automatically.
- Failures publish test reports and logs.

#### 14. Add targeted integration tests

**Status:** Not started

Priority additions:

- Atomic full-backup restore.
- Template editor restoration from a typed route.
- Export/import UI handoff.
- Intake inactivity timeout across background/resume.
- Release-build smoke coverage for serialization and QR generation after R8.

#### 15. Track coverage as information, not a vanity target

**Status:** Not started

Add a coverage report for critical business logic. Do not require every composable or trivial mapper to reach an arbitrary percentage.

### P6: Quality Tooling and Build Health

#### 16. Configure and prune Detekt

**Status:** Not started

The baseline contains approximately 195 findings, including normal Compose PascalCase function names and stale declaration references. This hides useful signals.

**Plan:**

- Add explicit Detekt configuration for Compose naming and accepted project conventions.
- Regenerate the baseline after configuration.
- Address genuine complexity findings incrementally, beginning with code already being changed.
- Do not perform a repository-wide formatting/refactoring churn solely to reduce counts.

#### 17. Fix Gradle 10 compatibility warning

**Status:** Not started

The Protobuf plugin resolves `protoc` using deprecated multi-string dependency notation under Gradle 9.6.1. Determine whether a newer protobuf Gradle plugin fixes it; avoid fragile platform-specific artifact strings unless necessary.

#### 18. Add dependency update automation

**Status:** Not started

Add weekly Dependabot updates for Gradle and GitHub Actions. Do not auto-merge initially. Group compatible patch updates after observing CI reliability.

#### 19. Harden release verification

**Status:** Not started

The release workflow derives the release version from a `v*` tag but does not verify it against `version.properties`, and it does not rerun the complete verification suite before publishing.

**Plan:**

- Verify tag version equals `VERSION_NAME`.
- Verify the changelog contains that version.
- Run unit tests and static checks in the release workflow or make release depend on a reusable verified workflow.
- Add an optional release APK smoke test before publishing.

### P7: Documentation and Contributor Experience

#### 20. Add an architecture document

**Status:** Not started

Create `docs/ARCHITECTURE.md` after the template editor and route boundaries stabilize. Include:

- Dependency graph.
- State and event flow.
- Persistence ownership.
- Navigation routes.
- Import/export transaction model.
- Rules for adding a feature.

#### 21. Add dependency and troubleshooting guides

**Status:** Not started

Document safe dependency updates, common Gradle/Proto issues, device installation signature mismatches, release signing prerequisites, and debugging commands.

## Decisions: What Not to Overhaul Now

- **No rewrite:** Existing behavior and tests provide a strong base.
- **No Hilt migration yet:** Manual construction is understandable; remove static global state first and reassess.
- **No Room migration yet:** Current data volume and access patterns fit Proto DataStore.
- **No additional feature modules yet:** Two modules are sufficient until ownership/build performance provides a concrete reason.
- **No universal `AppEvent`:** Feature-specific events retain clearer contracts.
- **No removal of navigation callbacks:** They keep pure screens testable and navigation centralized.
- **No blanket `@Immutable` pass:** Add stability annotations only after verifying member types and using Compose compiler reports to demonstrate value.
- **No arbitrary file-size rule:** Responsibility and change coupling matter more than line count.

## Suggested Delivery Sequence

### Milestone 1: Safe cleanup and atomic persistence

- [x] Remove obsolete Settings template APIs and tests.
- [x] Implement atomic full-backup persistence.
- [x] Add focused tests.
- [x] Run unit, lint, Ktlint, Detekt, and instrumentation compilation.

### Milestone 2: Restorable template editor

- [x] Add route template ID.
- [x] Add `TemplateEditorViewModel` with `SavedStateHandle`.
- [x] Move editor state/actions/events.
- [x] Remove list-to-editor event routing.
- [x] Add recreation and navigation tests.

### Milestone 3: Route/Screen boundaries

- [x] Convert Template Library and Editor first.
- [x] Convert Settings.
- [x] Convert Import and Export.
- [x] Convert Customer Intake last because it coordinates two feature states.

### Milestone 4: UI decomposition (Complete)

- [x] Split Customer Intake feature sections while preserving behavior.
- [x] Add focused Customer Intake QR section test.
- [x] Split remaining feature sections while preserving behavior.
- [x] Add focused previews/tests for remaining extractions.
- [x] Split reusable design-system controls by component family.

### Milestone 5: Dependency construction

- [ ] Introduce `AppContainer`.
- [ ] Remove static provider state and global test resets.
- [ ] Reassess whether Hilt is justified.

### Milestone 6: Automation and documentation

- [ ] Add emulator CI.
- [ ] Configure Detekt and prune baseline.
- [ ] Add Dependabot.
- [ ] Harden release version verification.
- [ ] Add architecture, dependency-update, and troubleshooting guides.

## Verification Commands

Run focused checks while developing, followed by the full local gate before merging:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew :app:ktlintCheck
./gradlew :app:detekt
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

Run device tests when an emulator or device is available:

```bash
./gradlew :app:connectedDebugAndroidTest
```

## Progress Log

- 2026-08-10: Initial audit completed and roadmap created on `refactor/maintainability-overhaul`.
- 2026-08-10: Existing intake IME inset fix retained on the overhaul branch.
- 2026-08-10: Removed obsolete template-management APIs and uncollected errors from Settings.
- 2026-08-10: Made full-backup persistence atomic and added option-contract tests.
- 2026-08-10: Made template editor routes restorable and moved editor ownership into a destination-scoped ViewModel.
- 2026-08-10: Started Milestone 3 by splitting Template Library and Editor into explicit Route/Screen boundaries.
- 2026-08-10: Split Settings into an explicit Route/Screen boundary and verified it on a wireless debug device.
- 2026-08-10: Split Import and Export into explicit Route/Screen boundaries, keeping file and clipboard I/O in routes.
- 2026-08-10: Completed Milestone 3 by splitting Customer Intake into an explicit Route/Screen boundary while preserving its validated IME behavior.
- 2026-08-10: Started Milestone 4 by extracting Customer Intake's template, form, send-action, and QR presentation sections into focused files.
- 2026-08-10: Completed Milestone 4 by splitting reusable component families and Settings, Export, Import, and QR-sheet presentation into focused UI components with Compose coverage.
