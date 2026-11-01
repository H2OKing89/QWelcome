# UI/UX redesign — 2026-11-01

## Printable WiFi sign

Save and Share produce a white portrait sign with a subtle purple Wi-Fi icon, a rounded border inside generous print margins, the heading “Connect to Wi-Fi,” and a short camera instruction. The QR is black on white and sized to the space between the measured heading and credential fields. Network name and password are now separate labeled fields. Network names use bold sans text, passwords use monospace text, and both preserve every character while wrapping without ellipsis or automatic hyphenation. Open networks show “No password.”

The sign has no app branding, illustrative-placeholder label, or manual-connection banner. The sheet continues to omit the extra scan instruction and resolution hint. Android’s native “Print / Save PDF” flow adds paper-size selection, initially US Letter portrait; A4 and other supported media are laid out to their available content area. Native paths and text go directly to PDF, with no print-sized bitmap allocation. PNG Save and Share remain available.

The original PNG is 3508 × 4961 pixels, sufficient for A3 at approximately 300 pixels per inch or 11×17 with fit-to-page margins. This is a pixel-resolution guarantee, not an embedded printer-DPI setting. Printing at larger dimensions reduces effective pixel density. The phone shows the same artwork at 1280 pixels wide; it does not allocate the full print bitmap just to open the preview.

One immutable request supplies both QR payload and visible credentials. Export uses integer-aligned modules and a four-module white quiet zone. Artwork rendering, PDF writing, and PNG writing run off Main. Export bitmaps, PDF pages/documents, and output descriptors are closed on success, failure, and cancellation. All output actions share a busy guard. Portrait artwork also fits landscape media without clipping.

Earlier verification covered all 338 unit tests. The current redesign passed all 16 targeted QR tests: PNG encoding and cleanup; 12 downsampled QR scans across standard, hidden/escaped, WPA3, open, maximum-length, and Unicode networks; long-credential Letter artwork; actual Letter/A4 PDF generation and rasterized QR decoding; cancellation; large-text action access; and disabled output states. Final assembly, instrumentation compilation, ktlint, and Detekt passed. Android Lint passed. The original PNG was visually reviewed and the app installed on the Samsung; the final physical interaction review is awaiting phone unlock.

For repeatable visual QA, run WifiQrArtworkRenderTest and WifiQrPrintDocumentAdapterTest with instrumentation argument `qrArtworkExportPreview=true` on an isolated API29+ emulator. They publish synthetic King/12345678 examples to Downloads as `qwelcome-wifi-qr-print-preview.png`, `qwelcome-wifi-sign-letter.pdf`, and `qwelcome-wifi-sign-a4.pdf`. Regular tests do not publish files.

## WiFi security selection

The Security menu offers SecurityOff, WPA2-Personal, WPA-WPA2-Personal, WPA3-Personal, and WPA2-WPA3-Personal. Fresh and reset forms default to WPA2-WPA3-Personal. SecurityOff clears/disables the password and uses the same selector as protected modes; the separate open-network toggle was removed. The enum is authoritative for intake validation, messages, QR availability, and the sheet summary. Legacy open flags at QR entry points are normalized before encoding and drawing.

QR payloads use `nopass` without a password for SecurityOff, `SAE` for WPA3-only, and the compatible `WPA` token for PSK and mixed modes. WPA2/WPA3 transition encoding follows [AOSP's QR generation](https://android.googlesource.com/platform/packages/apps/Settings/+/master/src/com/android/settings/wifi/dpp/WifiDppUtils.java); the QR format does not have a separate mixed-mode token.

Security-change verification: all 344 unit tests and 14 targeted device tests passed. Coverage includes all five dropdown choices, the mixed default/reset, password clearing only for SecurityOff, protected-mode password preservation, legacy request normalization, each QR mode, sheet/accessibility labels, and Letter/A4 PDFs. Assembly, ktlint, Detekt, and Android Lint passed.

## Settings follow-up

Settings now opens to a read-only overview with a technician summary and direct access to message templates, backup/restore, privacy/protection, and About/updates. Profile, privacy, backup, and About have focused pages within the existing Settings route. Section selection and each page's scroll position are saved across recreation.

The profile editor shows a signature example and a keyboard-aware persistent Save area. Successful saves stay in the editor with saved status and a confirmation toast. Leaving a dirty profile offers Keep editing or Discard; discarding resets both field values and edit ownership so later persisted changes can load normally. External Back navigation happens from the overview.

Privacy controls expose one labeled switch target per row and retain asynchronous loading states. Backup actions explain their scope and retain export protection for unsaved profiles. About separates app identity, update status, progress, and actions; download and installation behavior remains in the updater.

Verification: all 338 unit tests passed. The settings run covered 17 distinct UI cases, including the corrected accessibility selector on its four-test rerun. Coverage includes Save in place, draft discard, internal Back behavior, section/draft restoration, loading/disabled toggles, backup callbacks, update confirmation, download progress, and a 320 × 560 dp viewport at 2× text size with the keyboard. Final assembly, ktlint, Detekt, and Android Lint passed.

Physical review on the Samsung confirmed that all five overview destinations fit without scrolling (the final destination ends at y = 2595 px, above the y = 3067 px safe content edge). The profile editor, Samsung keyboard, privacy page, backup page, and About page were reviewed. Returning from Export restored the Backup page. Profile and privacy values were unchanged, and the phone was left on the overview.

## Initial redesign scope

The customer intake, shared visual system, template library/editor, settings, import/export, and recovery screens now use a consistent native design. Existing template preview and external SMS/share behavior are retained; no intake review screen or additional handoff step was added.

- Opaque navy surfaces, purple primary actions, restrained cyan accents, system sans typography, readable outlines, and consistent shapes.
- Customer and WiFi sections, template-dependent Required labels, a For SMS phone label, and no empty validation-support rows.
- Explicit keyboard progression: name → phone → account → SSID → password → Done. Open networks finish at SSID.
- Persistent SMS, Share, and Copy actions above the keyboard.
- Compact branded app bar with an accessible New customer action and a shorter label at large text sizes.
- Full-width template search, separate tag filtering, clear active-template cards, direct access to the existing preview, labeled editor actions, and keyboard-aware template viewports.
- Profile Save immediately after profile fields; readable privacy, backup, and update sections.
- Consistent import/export app bars, clear import stages, and actions that can grow with larger text.

Manual dependency injection, ViewModels, navigation, Proto DataStore, validators, QR generation, import/export behavior, and security controls remain in place.

## Measured before/after observations

Measurements use the same Samsung SM-S938U at 1440 × 3120 pixels, with the existing display and text settings. Bounds come from the device UI hierarchy.

| Check | Before | After |
| --- | --- | --- |
| Empty intake field block height | 277 px, including reserved empty support space | 224 px; 53 px / 19.1% less vertical space per field |
| Input control height | 196 px | 196 px; the space reduction preserves the input target |
| Settings profile Save/No changes top position in the initial viewport | y = 2407 px, after the templates section | y = 1541 px, directly after the profile; 866 px higher |
| Message action access while typing | Actions belonged to the scrolling form | All three actions verified visible above the keyboard while the password field remained visible |
| Additional intake preview/handoff steps | 0 | 0 |

The two form sections add headings and boundaries, so the per-field reduction is not a claim that total page height fell by 19%. Technician completion time and real-world error rate have not been benchmarked; the observations above measure layout and action access.

## Verification

- Debug build and instrumentation compilation passed.
- All 337 unit tests passed.
- Final debug assembly, ktlint, Detekt, and Android Lint passed after the selector contrast adjustment.
- API 37 emulator UI suite: 90 of 91 tests passed initially. The remaining profile test mounted sibling sections without their production scrolling parent; its harness was corrected.
- Final affected-screen rerun: all 34 tests passed, including the corrected profile test and the additional template-search keyboard test.
- Regression checks cover pinned actions after scrolling and with the keyboard, keyboard field order, a 320 × 560 dp viewport at 2× text size, template search resizing/last-result access, profile draft restoration, and template-library scroll restoration.

## Physical phone review

Installed and reviewed intake, password entry with the keyboard, settings, template browsing, the existing template preview, the unsaved template editor, and import/export entry screens. Profile and template values remained intact. No messages were sent, and no backup was imported.

Runtime validation on the phone used dark mode. The light-mode intake was also visually reviewed on the API 37 emulator, prompting a final selector-outline contrast adjustment. TalkBack interaction, outdoor readability, and timed technician trials remain useful follow-up checks.

## Repeatable checks

Run the following Gradle tasks separately from device review:

```text
:app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
:app:ktlintCheck :app:detekt
:app:lintDebug --no-daemon --max-workers=1
```

Select an isolated emulator with ANDROID_SERIAL before running connected tests, since existing test fixtures can reset app preferences.

```text
:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.kingpaging.qwelcome.ui
```
