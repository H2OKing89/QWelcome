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
| **Annotated tags only** | Tags are `vX.Y.Z` (e.g., `v2.4.0`). Pushing a `v*` tag triggers the GitHub Actions release workflow (`on: push: tags: ["v*"]`). |
| **Tag after merge** | The release tag must always point at a commit reachable from `master`. After merging the release PR, **delete the local tag and recreate it on `master` HEAD** before pushing. This guarantees the tag is correct regardless of merge method (merge commit, squash, or rebase). |

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

### Phase 0 — Merge All Pending Fix / Feature PRs First

> **This step must happen before creating the release branch.** The release branch is a snapshot of `master` at the point it is cut. Any bug fix or feature that should be included in the release must already be on `master` before you proceed.

**Required order of operations:**

1. **Fix / feature work** lives on its own branch (`fix/<desc>` or `feature/<desc>`).
2. **Open a PR** from that branch → `master`. Fill in the body, get CI green, get approval.
3. **Merge the PR** into `master`.
4. **Pull `master` locally** (`git pull origin master`) to incorporate the merged commits.
5. **Only then** proceed to Phase 1 to create the release branch.

```powershell
# Confirm master is fully up to date before cutting the release branch
git checkout master
git pull origin master

# Verify all expected fix/feature commits are present
git log --oneline -10
```

> **Why?** If you create the release branch before merging a fix PR, that fix is absent from the
> release unless you manually cherry-pick or re-merge — both of which are error-prone. Always
> let `master` converge first.

---

### Phase 1 — Prepare the Release Branch

```powershell
# 1. Make sure you are on master and up to date (repeat git pull if needed after Phase 0)
git checkout master
git pull origin master

# 2. Create and switch to a release branch
git checkout -b release/vX.Y.Z
```

### Phase 2 — Review Changes & Write the Changelog

Before writing anything, the AI agent **must** review all changes that will be in this release:

```powershell
# 0. Find the last release tag automatically
git fetch --tags
$LAST_TAG = git describe --tags --abbrev=0 --match "v*"
# (Bash equivalent: LAST_TAG=$(git describe --tags --abbrev=0 --match "v*"))
# If no v* tags exist yet (first release), diff from the root commit:
#   $LAST_TAG = git rev-list --max-parents=0 HEAD

# 1. List all commits since last release tag
git log $LAST_TAG..HEAD --oneline

# 2. List all changed files
git diff $LAST_TAG..HEAD --name-only

# 3. Review a summary of what changed
git diff $LAST_TAG..HEAD --stat

# 4. Review the actual diffs to understand what changed
git diff $LAST_TAG..HEAD
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
# Use explicit refspec; NEVER use --follow-tags (it will push the annotated tag)
git push -u origin HEAD
```

> **Warning:** `git push --follow-tags` pushes annotated tags reachable from the pushed commits. Always use the explicit command above to avoid pushing the tag prematurely.

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

### Phase 6 — Merge & Retag on Master & Push the Tag

After PR approval and CI passes:

1. **Merge the PR** into `master` (any merge method is safe: merge commit, squash, or rebase)
2. **Retag on the actual `master` HEAD** so the tag points at what shipped:

```powershell
git checkout master
git pull origin master

# Delete the old tag (created on the release branch) and recreate on master
# (2>$null makes this idempotent — safe if the tag was already deleted)
git tag -d vX.Y.Z 2>$null
git tag -a vX.Y.Z -m "Release vX.Y.Z"

# Push the tag to trigger the release workflow
git push origin vX.Y.Z
```

**Bash equivalent** (use `|| true` for idempotence):

```bash
git tag -d vX.Y.Z || true
git tag -a vX.Y.Z -m "Release vX.Y.Z"
git push origin vX.Y.Z
```

> **Why retag?** If the PR was squash-merged or rebased, GitHub creates new commit(s) on `master`.
> The original tag from the release branch would point at a commit that no longer exists in `master`'s
> history. Retagging guarantees the release is built from the code that actually shipped.
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
# ── Phase 0: Land all fix/feature PRs into master first ──────────────────────
0a. For each pending fix/feature:
    a. git checkout -b fix/<desc> (or feature/<desc>) from master
    b. Commit the changes
    c. git push -u origin HEAD
    d. Create PR → master via API/MCP tools
    e. Wait for CI green + approval
    f. Merge the PR on GitHub
0b. git checkout master && git pull origin master
    # Confirm all expected commits are present: git log --oneline -10
# ─────────────────────────────────────────────────────────────────────────────

1.  git checkout master && git pull origin master
2.  git fetch --tags
3.  $LAST_TAG = git describe --tags --abbrev=0 --match "v*"
    # (first release fallback: $LAST_TAG = git rev-list --max-parents=0 HEAD)
4.  Review changes: git log $LAST_TAG..HEAD --oneline
5.  Review files:   git diff $LAST_TAG..HEAD --name-only
6.  Review diffs:   git diff $LAST_TAG..HEAD
7.  Determine bump type (major/minor/patch) from the changes
8.  git checkout -b release/vX.Y.Z
9.  Edit CHANGELOG.md — write [Unreleased] entries from the diff review
10. .\scripts\bump-version.ps1 <patch|minor|major>       # Windows
    scripts/bump-version.sh <patch|minor|major>           # Linux/Mac
11. git push -u origin HEAD                               # push branch, NOT tag
12. Review: git log master..HEAD --oneline && git diff master..HEAD --name-only
13. Create PR via API/MCP tools:
    - Title: "release: vX.Y.Z"
    - Body: auto-generated summary, changelog, file list, checked boxes
14. (Wait for PR approval & CI green)
15. Merge the PR on GitHub (any merge method is safe)
16. git checkout master && git pull origin master
17. git tag -d vX.Y.Z 2>$null                             # delete old tag (idempotent)
18. git tag -a vX.Y.Z -m "Release vX.Y.Z"                 # retag on master HEAD
19. git push origin vX.Y.Z                                 # push tag → triggers release
20. Verify GitHub Release page
```

---

## Hotfix Process (Emergency Only)

For critical bugs on the current release:

```text
1. git checkout master && git pull
2. git checkout -b hotfix/<description>
3. Fix the bug, update CHANGELOG.md [Unreleased]
4. .\scripts\bump-version.ps1 patch
5. git push -u origin HEAD                               # push branch, NOT tag
6. Create PR: hotfix/<description> → master
7. After merge:
   git checkout master && git pull origin master
   git tag -d vX.Y.Z 2>$null                             # retag on master HEAD (idempotent)
   git tag -a vX.Y.Z -m "Release vX.Y.Z"
   git push origin vX.Y.Z                                # push tag → triggers release
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
| Tag accidentally pushed to remote | Someone ran `git push --follow-tags` or pushed the tag early | Delete remote then local: `git push --delete origin vX.Y.Z` then `git tag -d vX.Y.Z`, then retag on the correct commit and push again. |
| CI fails on release | Missing secrets (keystore, passwords) | Ensure `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_PASSWORD` are set in repo secrets. |
| Tag points to wrong commit after amend | Amended the release commit after tagging | Delete tag (`git tag -d vX.Y.Z`), amend, re-tag (`git tag -a vX.Y.Z -m "Release vX.Y.Z"`). |
| Tag points to wrong commit after squash/rebase merge | Tag was created on the release branch; squash/rebase creates new commits on `master` | Always retag on `master` after merge: `git tag -d vX.Y.Z && git tag -a vX.Y.Z -m "Release vX.Y.Z"` then push. |
| Tag pushed prematurely with `--follow-tags` | `git push --follow-tags` pushes annotated tags reachable from pushed commits | Always use `git push -u origin HEAD` (no `--follow-tags`) when pushing the branch. Recovery: `git push --delete origin vX.Y.Z` to remove the remote tag, then retag after merge. |
| Feature PR not merged before release | Started release branch from stale master | Always merge all feature PRs into master and `git pull` **before** creating the release branch. |

---

## Lessons Learned (v2.3.3 Retrospective)

Issues discovered during the first AI-driven release and the fixes applied:

1. **Bump script CHANGELOG bug (fixed):** The PowerShell bump script originally used a regex that only matched the sentinel text `No unreleased changes.`. When the `[Unreleased]` section had real entries, the regex silently did nothing — the entries were NOT moved under the new version heading. **Fix:** Replaced with a line-by-line state machine (`before` → `collecting` → `after`) that properly collects unreleased content and moves it. Both PS1 and Bash scripts now use equivalent state-machine approaches.

2. **Amending a tagged commit breaks the tag:** If you amend the release commit after the bump script creates the tag, the tag still points to the pre-amend commit. You must delete and recreate the tag. The bump script now handles this correctly so amending should not be necessary.

3. **Always merge feature PRs first:** The release branch should be created from an up-to-date `master` that already contains all feature work. If you create the release branch before merging feature PRs, you'll need to merge master into the release branch (messy) or restart.

4. **Local merge vs GitHub merge:** Merging the release PR locally with `git merge --no-ff` and pushing closes the GitHub PR automatically (GitHub detects the merge). Both approaches work, but using the local flow requires you to push master first, then push the tag separately.

5. **Squash/rebase merge invalidates the branch tag (fixed in guide):** When the bump script creates the tag on the release branch and the PR is later squash-merged, the tagged commit no longer exists in `master`'s history. The workflow now always retags on `master` after merge, making it safe to use any merge method.
