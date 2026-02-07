# Release & Tagging Guide (AI Agent Reference)

> **Audience:** AI coding agents (Copilot, Claude, etc.) acting on behalf of the repo owner.
> This is a step-by-step runbook — follow it literally.

---

## Golden Rules

| Rule | Detail |
| ------ | -------- |
| **No direct commits to `master`/`main`** | The pre-commit hook (`scripts/git-hooks/pre-commit`) rejects them. All work goes through feature/release branches + PR. |
| **Single version source** | `version.properties` at repo root (`VERSION_NAME`, `VERSION_CODE`). Never edit `app/build.gradle.kts` version values. |
| **Changelog before release** | `CHANGELOG.md` must have real entries under `## [Unreleased]` before bumping. The bump script validates this. |
| **Annotated tags only** | Tags are `vX.Y.Z` (e.g., `v2.4.0`). Pushing a `v*` tag triggers the GitHub Actions release workflow. |

---

## Branch Naming Conventions

| Purpose | Pattern | Example |
| --------- | --------- | --------- |
| New feature | `feature/<short-description>` | `feature/haptic-feedback` |
| Bug fix | `fix/<short-description>` | `fix/import-crash` |
| Release prep | `release/vX.Y.Z` | `release/v2.4.0` |
| Hotfix (rare) | `hotfix/<short-description>` | `hotfix/critical-crash` |

---

## Full Release Workflow

### Phase 1 — Prepare the Release Branch

```powershell
# 1. Make sure you are on master and up to date
git checkout master
git pull origin master

# 2. Create and switch to a release branch
git checkout -b release/vX.Y.Z
```

### Phase 2 — Review Changes & Write the Changelog

Before writing anything, the AI agent **must** review all changes that will be in this release:

```powershell
# 1. List all commits since last release tag
git log <last-tag>..HEAD --oneline

# 2. List all changed files
git diff <last-tag>..HEAD --name-only

# 3. Review the actual diffs to understand what changed
git diff <last-tag>..HEAD --stat
```

Using the review above, write comprehensive changelog entries under `## [Unreleased]` in `CHANGELOG.md`:

```markdown
## [Unreleased]

### Added
- **Feature Name** - Brief description

### Changed
- **Thing Changed** - Brief description

### Fixed
- **Bug Fixed** - Brief description

### Removed
- (if applicable)
```

**Rules for changelog entries:**

- Start each bullet with `- **Bold Title** - Description`
- Group by `Added`, `Changed`, `Fixed`, `Removed` (omit empty groups)
- Write from a user perspective, not implementation details
- Reference issue numbers where applicable
- Every meaningful code change must be represented — do not skip files

### Phase 3 — Run the Bump Script

The script updates `version.properties`, rewrites the changelog, creates a commit, and creates an annotated tag — all in one step.

**Windows (PowerShell):**

```powershell
.\scripts\bump-version.ps1 <major|minor|patch>

# Or explicit version:
.\scripts\bump-version.ps1 2.4.0
```

**Linux / Mac / Git Bash:**

```bash
scripts/bump-version.sh <major|minor|patch>

# Or explicit version:
scripts/bump-version.sh 2.4.0
```

**What the script does automatically:**

1. Reads current version from `version.properties`
2. Computes the new `VERSION_NAME` and increments `VERSION_CODE` by 1
3. Validates that `CHANGELOG.md` has content under `[Unreleased]`
4. Updates `version.properties` with new values
5. Moves `[Unreleased]` entries under a new `## [X.Y.Z] - YYYY-MM-DD` heading
6. Resets `[Unreleased]` to `No unreleased changes.`
7. Creates a git commit: `release: vX.Y.Z (code N)` (uses `--no-verify` to bypass the master-block hook)
8. Creates an annotated git tag: `vX.Y.Z`

> **Note:** The script commits with `--no-verify` internally, but this happens on the **release branch**, not master.

### Phase 4 — Push the Release Branch (Without the Tag)

```powershell
# Push the branch only — do NOT push the tag yet
git push -u origin release/vX.Y.Z
```

### Phase 5 — Create the Pull Request (AI Agent Fills Everything)

The AI agent is responsible for creating the PR **and writing the full PR body**. Do not leave placeholders — fill in every field from the actual changes.

**Step 1 — Gather context for the PR body:**

```powershell
# Review commits on this release branch
git log master..HEAD --oneline

# List all changed files
git diff master..HEAD --name-only

# Read the new changelog section from CHANGELOG.md
# (the bump script already moved [Unreleased] to [X.Y.Z] - date)
```

**Step 2 — Create the PR** from `release/vX.Y.Z` → `master` using the GitHub API or MCP tools:

| Field | Value |
| ------- | ------- |
| **Title** | `release: vX.Y.Z` |
| **Body** | AI-generated from template below — filled with real data |
| **Labels** | `release` (if available) |

**Step 3 — PR body template (AI fills every section):**

```markdown
## Release vX.Y.Z

### Summary
(1-2 sentence summary of what this release includes — written by the AI from the diff review)

### Changes
(paste the full changelog section for this version from CHANGELOG.md)

### Files Changed
(list the files changed, grouped by category: data/, ui/, viewmodel/, docs/, etc.)

### Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

### Testing
- [ ] `./gradlew test` passes
- [ ] `./gradlew lintDebug` passes
- [ ] Verified dark mode
- [ ] Verified light mode

### Checklist
- [ ] Changelog entries are complete and accurate
- [ ] `version.properties` updated (VERSION_NAME=X.Y.Z, VERSION_CODE=N)
- [ ] All CI checks pass
- [ ] APK builds successfully
```

> **Key rule:** The AI agent must check the appropriate boxes in "Type of Change" and "Testing" based on what was actually done (e.g., if tests were run and passed, check those boxes). Leave unchecked only items that were not verified.

### Phase 6 — Merge & Push the Tag

After PR approval and CI passes:

1. **Merge the PR** into `master` (use "Merge commit" or "Squash and merge" per preference)
2. **Push the tag** to trigger the release workflow:

```powershell
git checkout master
git pull origin master
git push origin vX.Y.Z
```

> Pushing the `vX.Y.Z` tag triggers `.github/workflows/release.yml` which:
>
> - Builds a signed release APK
> - Extracts the changelog for this version
> - Creates a GitHub Release named `Q Welcome vX.Y.Z`
> - Attaches `QWelcome-vX.Y.Z.apk` as a downloadable asset
> - Populates the release body with changelog + install instructions

### Phase 7 — Verify

- [ ] GitHub Release page exists at `https://github.com/H2OKing89/QWelcome/releases/tag/vX.Y.Z`
- [ ] APK is attached and downloadable
- [ ] Release body contains correct changelog
- [ ] CI workflow completed successfully

---

## Deciding the Bump Type

| When to use | Bump | Example |
| ------------- | ------ | --------- |
| Breaking changes, major redesigns | `major` | 2.3.2 → 3.0.0 |
| New features, non-breaking additions | `minor` | 2.3.2 → 2.4.0 |
| Bug fixes, minor tweaks, dependency updates | `patch` | 2.3.2 → 2.3.3 |

Follow [Semantic Versioning 2.0.0](https://semver.org/).

---

## Quick-Reference: AI Agent Command Sequence

For an AI agent performing a release, here is the exact sequence of operations.
**The agent handles everything — no manual steps required.**

```text
1.  git checkout master && git pull origin master
2.  Review changes: git log <last-tag>..HEAD --oneline
3.  Review files:   git diff <last-tag>..HEAD --name-only
4.  Determine bump type (major/minor/patch) from the changes
5.  git checkout -b release/vX.Y.Z
6.  Edit CHANGELOG.md — write [Unreleased] entries from the diff review
7.  .\scripts\bump-version.ps1 <patch|minor|major>       # Windows
    scripts/bump-version.sh <patch|minor|major>           # Linux/Mac
8.  git push -u origin release/vX.Y.Z                    # push branch, NOT tag
9.  Review: git log master..HEAD --oneline && git diff master..HEAD --name-only
10. Create PR via API/MCP tools:
    - Title: "release: vX.Y.Z"
    - Body: auto-generated summary, changelog, file list, checked boxes
11. (Wait for PR approval & CI green)
12. Merge the PR on GitHub
13. git checkout master && git pull origin master
14. git push origin vX.Y.Z                                # push tag → triggers release
15. Verify GitHub Release page
```

---

## Hotfix Process (Emergency Only)

For critical bugs on the current release:

```text
1. git checkout master && git pull
2. git checkout -b hotfix/<description>
3. Fix the bug, update CHANGELOG.md [Unreleased]
4. .\scripts\bump-version.ps1 patch
5. git push -u origin hotfix/<description>
6. Create PR: hotfix/<description> → master
7. After merge: git push origin vX.Y.Z
```

---

## File Reference

| File | Purpose |
| ------ | --------- |
| `version.properties` | `VERSION_NAME` and `VERSION_CODE` — single source of truth |
| `CHANGELOG.md` | Keep a Changelog format; `[Unreleased]` section is consumed by bump script |
| `scripts/bump-version.ps1` | Windows release script (PowerShell) |
| `scripts/bump-version.sh` | Linux/Mac release script (Bash) |
| `scripts/git-hooks/pre-commit` | Blocks direct commits to `master`/`main` |
| `.github/workflows/release.yml` | GitHub Actions: builds signed APK + creates GitHub Release on `v*` tag push |
| `.github/workflows/android.yml` | GitHub Actions: CI build + lint + tests on push/PR to master |

---

## Common Pitfalls

| Problem | Cause | Fix |
| --------- | ------- | ----- |
| `COMMIT REJECTED: Direct commits to 'master'` | Pre-commit hook blocked it | Create a branch first. Only the bump script uses `--no-verify`. |
| `No content under [Unreleased]` | Bump script validation failed | Add changelog entries before running the bump script, or use `-Force` (not recommended). |
| GitHub Release has no changelog | Tag pushed before changelog was written | Always write changelog → bump → merge PR → then push tag. |
| Tag already exists | Re-running bump for same version | Delete the local tag (`git tag -d vX.Y.Z`) and let the script recreate it. |
| CI fails on release | Missing secrets (keystore, passwords) | Ensure `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD` are set in repo secrets. |
| Tag points to wrong commit after amend | Amended the release commit after tagging | Delete tag (`git tag -d vX.Y.Z`), amend, re-tag (`git tag -a vX.Y.Z -m "Release vX.Y.Z"`). |
| Feature PR not merged before release | Started release branch from stale master | Always merge all feature PRs into master and `git pull` **before** creating the release branch. |

---

## Lessons Learned (v2.3.3 Retrospective)

Issues discovered during the first AI-driven release and the fixes applied:

1. **Bump script CHANGELOG bug (fixed):** The PowerShell bump script originally used a regex that only matched the sentinel text `No unreleased changes.`. When the `[Unreleased]` section had real entries, the regex silently did nothing — the entries were NOT moved under the new version heading. **Fix:** Replaced with a line-by-line state machine (`before` → `collecting` → `after`) that properly collects unreleased content and moves it. Both PS1 and Bash scripts now use equivalent state-machine approaches.

2. **Amending a tagged commit breaks the tag:** If you amend the release commit after the bump script creates the tag, the tag still points to the pre-amend commit. You must delete and recreate the tag. The bump script now handles this correctly so amending should not be necessary.

3. **Always merge feature PRs first:** The release branch should be created from an up-to-date `master` that already contains all feature work. If you create the release branch before merging feature PRs, you'll need to merge master into the release branch (messy) or restart.

4. **Local merge vs GitHub merge:** Merging the release PR locally with `git merge --no-ff` and pushing closes the GitHub PR automatically (GitHub detects the merge). Both approaches work, but using the local flow requires you to push master first, then push the tag separately.

