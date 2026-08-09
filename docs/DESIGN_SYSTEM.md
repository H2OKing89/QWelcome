# Q Welcome Design System

This document defines the shared UI conventions for the Android app. Prefer the
components in `ui/components/` over styling Material components at individual
call sites.

## Spacing

Use a 4dp base grid. The common values have distinct roles:

| Spacing | Use |
| --- | --- |
| 4dp | Tight visual adjustment, icon-to-label correction |
| 8dp | Related inline controls and button groups |
| 12dp | Internal panel rhythm and compact list-item padding |
| 16dp | Screen edges, standard panel padding, section separation |
| 24dp | Separation between major sections |
| 32dp | Rare large separation or empty-state breathing room |

The use of both 12dp and 16dp is intentional: 12dp groups content inside a
compact component, while 16dp establishes screen and section structure.

## Icons

Use these glyph sizes consistently:

| Size | Use |
| --- | --- |
| 16dp | Chips, compact metadata, trailing dismiss icons |
| 18dp | Inline status and dense row actions |
| 24dp | Standard toolbar and standalone action icons |

Interactive icon controls must retain a minimum 48dp touch target regardless of
glyph size. Icons that repeat adjacent text or selected-state semantics are
decorative and use a null content description. Icons that communicate unique
meaning require a localized content description.

## Components

- Use `NeonPanel` for grouped form or tool content.
- Use `NeonButton` for commands. Use `PRIMARY` once per action group,
  `SECONDARY` for alternatives, and `TERTIARY` for cancel, disclosure, and
  low-emphasis actions.
- Use `NeonOutlinedField` for string-backed form fields. Stateful multiline
  editors may use `BasicTextField` with the shared editor chrome.
- Use `NeonDiscardDialog` for unsaved-change confirmation.
- Raw Material components remain appropriate for structural controls such as
  app bars, icon buttons, checkboxes, switches, chips, dropdown items, and
  dialog containers.

## Dialogs

Dialog actions follow the same hierarchy as screen actions:

- Confirm: `NeonButtonStyle.PRIMARY`
- Alternate action: `NeonButtonStyle.SECONDARY`
- Cancel or keep editing: `NeonButtonStyle.TERTIARY`
- Destructive confirm: primary style using `MaterialTheme.colorScheme.error`

Keep action labels short enough to wrap safely on compact screens and under
large font scaling.

## Scrolling And Editors

A screen-level `verticalScroll` is acceptable for forms containing single-line
fields. Do not place a multiline editor with its own scrolling behavior inside
that container. Multiline editing belongs in a dedicated dialog or un-nested
layout with `imePadding()`.
