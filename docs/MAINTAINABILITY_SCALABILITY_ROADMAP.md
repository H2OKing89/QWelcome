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

## Post-Merge Review

Review date: 2026-08-11

The maintainability overhaul was merged into `master` and verified with the full local gate: unit tests, instrumentation-test compilation, Ktlint, Detekt, Android lint, and debug assembly. Hosted CI also passed build, lint, instrumentation smoke, CodeQL, and dependency review.

The 2026-08-10 baseline remains an historical audit snapshot. At this review, the workspace contained 100 production Kotlin files totaling 14,904 lines, 18 unit-test files, and 16 instrumentation-test files.

### Findings

- No blocking production defects were identified in the merged route, persistence, export, dependency-construction, or CI changes.
- Replayed Export and Import events are explicitly consumed by their routes. Template-editor events use `MutableSharedFlow` with `replay = 0`, so they cannot replay when returning to that destination and do not need a replay-cache reset.
- Export and Import screen and ViewModel tests cover most behavior, but the route boundary lacks direct tests for lifecycle restarts, activity-result cancellation, URI handoff, and duplicate-effect prevention.
- A broad connected-test run on the API 36 Samsung can lose Compose test hierarchies after many tests in one instrumentation process. Fresh class-level runs passed Customer Intake (6/6) and Template List (21/21), so this is a physical-device/UTP batching limitation rather than a confirmed application regression. Emulator CI remains green.
- Editor text ownership remains a deliberate two-way synchronization between `TextFieldState` and ViewModel `contentText`. It is working but still carries more synchronization complexity than a single authoritative representation.

### Recommended Next

1. Add Export and Import route integration tests covering file-picker cancellation, successful URI handoff, lifecycle restart, and one-shot effect consumption.
2. Complete P4 event standardization: document each feature flow's replay and drop behavior, retain state for restorable outcomes, and reserve effects for transient UI and external intents.
3. Decide whether editor text synchronization should remain as-is or move to a single persisted text owner; preserve placeholder insertion, selection, IME, and restoration behavior with focused tests.
4. Add a release-build/R8 smoke test for typed-route serialization, QR generation, and import/export serialization.
5. Investigate the Protobuf Gradle plugin's Gradle 10 compatibility warning when a compatible upgrade is available.
6. Keep broad Samsung Compose validation in fresh class batches until the UTP/device issue is resolved; continue using emulator CI as the merge gate.
7. Evaluate destination-scoping the remaining activity-scoped feature ViewModels, starting with Import and Export; preserve their intended reset and restoration semantics before changing their lifetimes.
8. Establish a theme-token boundary for mode-specific visual effects, complete the branded Material typography scale, and add JVM contrast checks before considering additional themes or a user preference.

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

**Status:** Complete 2026-08-10

`QWelcomeApplication` now owns one lazy `AppContainer`, which documents and constructs the process-lifetime dependency graph. `AppViewModelProvider` is a stateless ViewModel factory over that container, and the template editor receives the same container while retaining its destination-scoped `SavedStateHandle`.

The container owns application-context-backed `SettingsStore`, `ResourceProvider`, `AppUpdater`, `ImportExportRepository`, and `PackageManager` access. `QWelcomeApplication` also uses the container's `SettingsStore` for Crashlytics preference collection, so production no longer creates a parallel store wrapper during startup.

All static mutable provider fields and `resetForTesting()` calls were removed. Tests continue to construct focused stores, repositories, fakes, and ViewModels directly. Focused container tests verify application ownership, within-container memoization, and independence between separately constructed containers.

Hilt is not justified at the current scale: the explicit container has one production owner, two factory call sites, and no generated wiring requirement. Reassess only if constructor/factory wiring grows enough to create demonstrated maintenance cost.

**Dependency graph:**

```text
QWelcomeApplication
└── AppContainer
  ├── SettingsStore
  ├── ResourceProvider
  ├── AppUpdater
  ├── ImportExportRepository
  └── PackageManager
MainActivity
└── AppViewModelProvider (uses AppContainer)
  └── Activity-scoped feature ViewModels
AppNavGraph (uses AppContainer)
└── AppViewModelProvider
  └── Destination-scoped TemplateEditorViewModel
```

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

Customer Intake now keeps route wiring in `CustomerIntakeRoute.kt`, while `CustomerIntakeScreen.kt` owns screen presentation and callbacks; its template selector, form and advanced Wi-Fi controls, send actions, and QR action live in focused feature files. A dedicated QR action test covers the extracted component boundary.

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

**Status:** Complete 2026-08-10

`android.yml` now runs a four-class emulator smoke suite for pull requests and default-branch pushes. Scheduled and manually dispatched workflow runs execute the complete instrumentation suite. Both jobs upload connected-test reports after every workflow run.

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

**Status:** Complete 2026-08-10

`app/detekt.yml` now accepts PascalCase `@Composable` functions and the repository's `import_pkg` package convention. Regenerating the baseline removed those convention-only entries and reduced it from 195 to 72 findings, leaving complexity and other active debt visible.

**Plan:**

- Add explicit Detekt configuration for Compose naming and accepted project conventions.
- Regenerate the baseline after configuration.
- Address genuine complexity findings incrementally, beginning with code already being changed.
- Do not perform a repository-wide formatting/refactoring churn solely to reduce counts.

#### 17. Fix Gradle 10 compatibility warning

**Status:** Not started

The Protobuf plugin resolves `protoc` using deprecated multi-string dependency notation under Gradle 9.6.1. Determine whether a newer protobuf Gradle plugin fixes it; avoid fragile platform-specific artifact strings unless necessary.

#### 18. Add dependency update automation

**Status:** Complete 2026-08-10

`.github/dependabot.yml` opens weekly grouped update pull requests for Gradle dependencies and GitHub Actions. It does not enable auto-merge; each update remains subject to normal review and CI.

#### 19. Harden release verification

**Status:** Complete 2026-08-10

The release workflow now verifies that a `vX.Y.Z` tag matches `VERSION_NAME`, requires a non-empty changelog section for that version, and runs unit tests, Ktlint, Detekt, and debug lint before decoding the release keystore.

**Plan:**

- Verify tag version equals `VERSION_NAME`.
- Verify the changelog contains that version.
- Run unit tests and static checks in the release workflow or make release depend on a reusable verified workflow.
- Add an optional release APK smoke test before publishing.

### P7: Documentation and Contributor Experience

#### 20. Add an architecture document

**Status:** Complete 2026-08-10

`docs/ARCHITECTURE.md` documents the stabilized dependency lifetimes, Route/Screen and event flow, Proto persistence, type-safe navigation, atomic import behavior, and feature-addition rules. It includes:

- Dependency graph.
- State and event flow.
- Persistence ownership.
- Navigation routes.
- Import/export transaction model.
- Rules for adding a feature.

#### 21. Add dependency and troubleshooting guides

**Status:** Complete 2026-08-10

`docs/DEPENDENCY_UPDATE_GUIDE.md` and `docs/TROUBLESHOOTING.md` document safe update review, Gradle and Proto diagnostics, device signature mismatches, release signing prerequisites, CI behavior, and focused debugging commands.

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

- [x] Introduce `AppContainer`.
- [x] Remove static provider state and global test resets.
- [x] Reassess whether Hilt is justified.

### Milestone 6: Automation and documentation (Complete)

- [x] Add emulator CI.
- [x] Configure Detekt and prune baseline.
- [x] Add Dependabot.
- [x] Harden release version verification.
- [x] Add architecture, dependency-update, and troubleshooting guides.

### Milestone 7: Feature ViewModel lifetime reassessment (Complete 2026-08-12)

Import, Export, Template Library, and Template Editor are destination-scoped with a navigation `ViewModelStoreOwner`, while Customer Intake, Settings, and focused template selection remain activity-scoped. The lifetime split is now explicit, but further migration is not mechanical: Customer Intake has lifecycle-sensitive form state, and Settings participates in activity-level screen-capture protection.

- [x] Define the desired restoration and cancellation behavior for Import and Export: both start fresh when reopened, so staged imports, export results, selections, pending file actions, and transient effects end with their destination.
- [x] Cover Export document-picker cancellation at the Route boundary: a cancelled picker clears the pending export and allows a second save request.
- [x] Cover Import file-picker URI handoff at the Route boundary: a selected `content://` URI is read, validated, and presented for confirmation.
- [x] Verify route lifecycle, activity-result, process-restoration, and one-shot-effect behavior with focused tests for each migrated feature. Import and Export picker effects survive a stopped route, are consumed exactly once after resume, and do not relaunch on a second lifecycle restart. Fresh ViewModel tests define the intentional process-loss reset contract.
- [x] Define a focused template-selection boundary for Customer Intake: `TemplateSelectionViewModel` owns only selectable templates, active selection, and selection feedback, while `TemplateListViewModel` remains library-only.
- [x] Destination-scope `TemplateListViewModel`: preserve the same library instance while Template Editor is above it, then create a fresh instance after the library is popped and reopened.
- [x] Cover both Template Library lifetime cases with focused navigation instrumentation tests on the physical Samsung test device.
- [x] Retain Customer Intake, Settings, and focused template selection at activity scope: Intake has explicit pause/resume behavior, Settings drives activity-level screen-capture protection, and template selection is shared with Intake.
- [x] Update `CLAUDE.md` after the scoping decision so contributor guidance accurately distinguishes activity-scoped and destination-scoped ViewModels, and removes its obsolete singleton/reset-for-testing description.
- [x] Keep the current package layout; this reassessment found no concrete ownership or build-time problem that justifies a feature-first reorganization.

### Milestone 8: Theme tokens and outdoor readability (Candidate)

Fourteen production files currently read `LocalDarkTheme`, with components deciding mode-specific glow, borders, elevation, opacity, and backdrop presentation themselves. That works for the current light/dark pair but makes another visual theme a cross-component migration. `CyberpunkTheme` also supplies no Material `shapes` set; its `CyberTypography` defines six of Material 3's twelve styles; and `CyberpunkBackdrop` duplicates palette literals instead of consuming theme values.

- [ ] Delete the unused `CyberColors` object and deprecated `CyberScheme` accessor after confirming no external source-set or documentation dependency remains.
- [ ] Define theme-owned effect tokens for the visual decisions now selected by `if (isDark)`, then migrate one component family at a time so components consume tokens rather than theme mode. Keep standard colors in `MaterialTheme.colorScheme` and semantic extras in `LocalCyberColors`.
- [ ] Define all twelve Material 3 typography styles with the app's display and body font families. Preserve the existing dark-mode text-glow intent through the theme layer, and avoid broad text-size changes as part of this correction.
- [ ] Introduce a shared Material `Shapes` set only after specifying the intended corner language. Replace repeated shape literals incrementally; do not use a bulk replacement to force unrelated controls into one shape.
- [ ] Move backdrop palette and grid-effect values to theme-owned tokens so they cannot drift from the active color scheme. Consider a spacing scale only where repeated layout values show an actual consistency or density problem.
- [ ] Add pure JVM contrast-ratio tests for text/background and semantic color pairs in every supported scheme. Use failures to tune tokens rather than relying only on visual inspection.
- [ ] Validate whether technicians need a persistent System/Light/Dark preference for outdoor readability before extending the Proto schema and Settings UI. If adopted, persist the preference, retain a system default, and test migration plus theme selection.

**Acceptance criteria:**

- Adding a supported visual theme does not require individual components to branch on a light/dark boolean for presentation.
- Every Material typography slot uses an intentional Q Welcome text style.
- Backdrop colors and effects derive from the active theme rather than duplicate palette literals.
- Every supported scheme has automated contrast checks for its defined semantic pairs.
- A persisted theme override exists only after the product decision and migration behavior are explicitly defined.

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
- 2026-08-10: Completed Milestone 5 by introducing an application-owned `AppContainer`, removing static provider state and test resets, and retaining manual dependency construction after reassessing Hilt.
- 2026-08-10: Completed Milestone 6 with emulator smoke and full-suite automation, focused Detekt conventions and baseline pruning, weekly Dependabot updates, fail-fast release metadata checks, and contributor architecture, dependency, and troubleshooting guides.
- 2026-08-12: Started Milestone 7 by destination-scoping Import and Export, defining fresh-on-return behavior, and removing their Activity `CompositionLocal` providers.
- 2026-08-12: Added Samsung-verified navigation scope coverage and Export document-picker cancellation coverage for the destination-scoped routes.
- 2026-08-12: Added Samsung-verified Import file-picker URI handoff coverage with an instrumentation-only content provider fixture.
- 2026-08-12: Added exact one-shot picker-launch assertions and split Customer Intake template selection from Template Library state, with focused unit and Samsung instrumentation coverage.
- 2026-08-12: Completed Milestone 7 by destination-scoping Template Library, verifying retained/recreated back-stack lifetimes, proving picker effects survive one lifecycle restart without duplicate launches, and defining fresh process-recreation state contracts.
