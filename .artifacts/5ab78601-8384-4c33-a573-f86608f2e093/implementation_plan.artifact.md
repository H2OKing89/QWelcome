# Template UI Overhaul

This plan outlines the UI/UX overhaul of the Templates section, focusing on providing a clearer active template indicator, adding a live message preview in the editor, and improving the editor layout.

## Open Questions
- None currently.

## Proposed Changes

### Active Template Indicator

The active template in the list screen needs to stand out more.

#### [MODIFY] [TemplateListScreen.kt](file:///home/quentin/StudioProjects/QWelcome/app/src/main/java/com/kingpaging/qwelcome/ui/templates/TemplateListScreen.kt)
- Enhance `TemplateCard`. When `isActive` is true, use a stronger `BorderStroke` (e.g., width 2.dp) and a brighter `containerColor` (slightly higher alpha).
- Add a neon glow effect or an explicit "ACTIVE" badge to the card when active.

### Live Message Preview and Editor Layout

The editor screen needs a live preview of the message (with placeholders substituted with dummy data) and a better layout.

#### [MODIFY] [TemplateEditorState.kt](file:///home/quentin/StudioProjects/QWelcome/app/src/main/java/com/kingpaging/qwelcome/ui/templates/TemplateEditorState.kt)
- Create a composable function to render a "Live Preview" of the template text.
- Modify `TemplateEditorContent` layout:
    - Instead of just `MessageContentLauncher`, show the `ContentEditorField` directly on the screen if space allows, OR keep the launcher but add a live preview section below it.
    - Rearrange sections: Name -> Tags -> Message Editor / Live Preview.
    - Increase spacing between sections.

#### [MODIFY] [TemplateEditorScreen.kt](file:///home/quentin/StudioProjects/QWelcome/app/src/main/java/com/kingpaging/qwelcome/ui/templates/TemplateEditorScreen.kt)
- Minor adjustments if necessary to support the new layout.

## Verification Plan

### Manual Verification
- Build and run the app.
- Navigate to the Templates tab.
- Verify the active template in the list has a distinct, clear visual indicator.
- Open a template to edit.
- Verify the layout is cleaner with better spacing.
- Type in the message field (or click edit and type) and observe the live preview updating and substituting placeholders correctly.
