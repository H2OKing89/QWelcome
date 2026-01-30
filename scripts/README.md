# Scripts

## Git Hooks Setup

This project uses custom git hooks to enforce branch protection. After cloning, run:

```bash
git config core.hooksPath scripts/git-hooks
```

### What the hooks do

- **pre-commit**: Blocks direct commits to `main` or `master` branches. Forces use of feature branches and pull requests.

### Bypassing (emergency only)

If you absolutely need to bypass the hook (e.g., hotfix):

```bash
git commit --no-verify -m "hotfix: critical fix"
```

Use sparingly — PRs exist for a reason!

## Version Bumps

Use `bump-version.sh` to create releases. It updates the version, rewrites the changelog, commits, and tags in one step.

```bash
# Increment patch version (e.g., 2.0.0 -> 2.0.1)
scripts/bump-version.sh patch

# Increment minor version (e.g., 2.0.0 -> 2.1.0)
scripts/bump-version.sh minor

# Increment major version (e.g., 2.0.0 -> 3.0.0)
scripts/bump-version.sh major

# Set an explicit version
scripts/bump-version.sh 2.5.0

# Bump and push to remote in one step
scripts/bump-version.sh patch --push

# Skip changelog validation (not recommended)
scripts/bump-version.sh patch --force
```

### What the script does

1. Reads current version from `version.properties`
2. Computes the new version and auto-increments `VERSION_CODE`
3. Validates that `CHANGELOG.md` has content under `[Unreleased]`
4. Updates `version.properties` with new values
5. Moves unreleased changelog entries under a new dated heading
6. Creates a git commit (`release: vX.Y.Z (code N)`) and annotated tag (`vX.Y.Z`)
7. Optionally pushes commit and tag to remote (`--push`)

### Prerequisites

- Entries must be added under `[Unreleased]` in `CHANGELOG.md` before bumping (unless `--force` is used)
- The script uses `--no-verify` to bypass the branch-protection pre-commit hook since this is a deliberate release action
