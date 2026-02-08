#!/usr/bin/env bash
#
# bump-version.sh — Bump version, update changelog, commit, and tag.
#
# Usage:
#   scripts/bump-version.sh <major|minor|patch|X.Y.Z> [--push] [--force]
#
# Options:
#   --push   Push the release branch to remote (tag is NOT pushed —
#            it must be recreated on master after the PR is merged)
#   --force  Skip changelog content validation
#

set -euo pipefail

# ── Resolve project root ──────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

VERSION_FILE="$ROOT_DIR/version.properties"
CHANGELOG_FILE="$ROOT_DIR/CHANGELOG.md"

# ── Parse arguments ───────────────────────────────────────────────────
BUMP=""
PUSH=false
FORCE=false

for arg in "$@"; do
    case "$arg" in
        --push)  PUSH=true ;;
        --force) FORCE=true ;;
        *)       BUMP="$arg" ;;
    esac
done

if [ -z "$BUMP" ]; then
    echo "Usage: scripts/bump-version.sh <major|minor|patch|X.Y.Z> [--push] [--force]"
    exit 1
fi

# ── Read current version ──────────────────────────────────────────────
if [ ! -f "$VERSION_FILE" ]; then
    echo "Error: $VERSION_FILE not found."
    exit 1
fi

CURRENT_NAME="$(grep '^VERSION_NAME=' "$VERSION_FILE" | cut -d= -f2-)"
CURRENT_CODE="$(grep '^VERSION_CODE=' "$VERSION_FILE" | cut -d= -f2-)"

if [ -z "$CURRENT_NAME" ] || [ -z "$CURRENT_CODE" ]; then
    echo "Error: Could not read VERSION_NAME or VERSION_CODE from $VERSION_FILE"
    exit 1
fi

IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_NAME"

# ── Compute new version ──────────────────────────────────────────────
case "$BUMP" in
    major)
        MAJOR=$((MAJOR + 1))
        MINOR=0
        PATCH=0
        ;;
    minor)
        MINOR=$((MINOR + 1))
        PATCH=0
        ;;
    patch)
        PATCH=$((PATCH + 1))
        ;;
    *)
        # Treat as explicit version (X.Y.Z)
        if ! echo "$BUMP" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+$'; then
            echo "Error: Invalid version '$BUMP'. Use major, minor, patch, or X.Y.Z."
            exit 1
        fi
        IFS='.' read -r MAJOR MINOR PATCH <<< "$BUMP"
        ;;
esac

NEW_NAME="${MAJOR}.${MINOR}.${PATCH}"
NEW_CODE=$((CURRENT_CODE + 1))

echo "Version: $CURRENT_NAME -> $NEW_NAME"
echo "Code:    $CURRENT_CODE -> $NEW_CODE"

# ── Validate changelog ────────────────────────────────────────────────
if [ "$FORCE" = false ]; then
    if [ ! -f "$CHANGELOG_FILE" ]; then
        echo "Error: $CHANGELOG_FILE not found. Use --force to skip."
        exit 1
    fi

    # Extract content between [Unreleased] and the next ## heading
    UNRELEASED=$(awk '/^## \[Unreleased\]/{found=1; next} /^## \[/{found=0} found{print}' "$CHANGELOG_FILE" \
        | sed '/^[[:space:]]*$/d')

    if [ -z "$UNRELEASED" ] || [ "$UNRELEASED" = "No unreleased changes." ]; then
        echo "Error: No content under [Unreleased] in CHANGELOG.md."
        echo "       Add changelog entries before bumping, or use --force to skip."
        exit 1
    fi
fi

# ── Update version.properties ────────────────────────────────────────
printf 'VERSION_NAME=%s\nVERSION_CODE=%s\n' "$NEW_NAME" "$NEW_CODE" > "$VERSION_FILE"
echo "Updated $VERSION_FILE"

# ── Update CHANGELOG.md ──────────────────────────────────────────────
if [ -f "$CHANGELOG_FILE" ]; then
    TODAY=$(date +%Y-%m-%d)

    # Use awk to:
    # 1. When we hit [Unreleased], print it, then print the sentinel + blank + new heading
    # 2. Skip the old sentinel line if present
    # 3. Pass everything else through
    awk -v ver="$NEW_NAME" -v date="$TODAY" '
    /^## \[Unreleased\]/ {
        print $0
        print ""
        print "No unreleased changes."
        print ""
        print "## [" ver "] - " date
        found_unreleased = 1
        next
    }
    found_unreleased && /^No unreleased changes\.$/ {
        # Skip the old sentinel line (it was already reprinted above)
        found_unreleased = 0
        next
    }
    { print }
    ' "$CHANGELOG_FILE" > "$CHANGELOG_FILE.tmp" && mv "$CHANGELOG_FILE.tmp" "$CHANGELOG_FILE"

    echo "Updated $CHANGELOG_FILE"
fi

# ── Git commit and tag ────────────────────────────────────────────────
cd "$ROOT_DIR"

STAGED=$(git diff --cached --name-only)
if [ -n "$STAGED" ]; then
    echo "Error: There are already staged changes in the index:"
    echo "$STAGED"
    echo "       Please unstage them before running this script."
    exit 1
fi

git add "$VERSION_FILE"
if [ -f "$CHANGELOG_FILE" ]; then
    git add "$CHANGELOG_FILE"
fi
git commit --no-verify -m "release: v${NEW_NAME} (code ${NEW_CODE})"
git tag -a "v${NEW_NAME}" -m "Release v${NEW_NAME}"

echo ""
echo "Created commit and tag v${NEW_NAME}"

# ── Optional push ─────────────────────────────────────────────────────
# Only push the branch — NEVER the tag. The tag must be recreated on
# master after the PR is merged (see docs/RELEASE_GUIDE.md Phase 6).
if [ "$PUSH" = true ]; then
    git push -u origin HEAD
    echo "Branch pushed (tag NOT pushed — retag on master after merge)."
fi

echo ""
echo "Done! Release v${NEW_NAME} (code ${NEW_CODE}) is ready."

if [ "$PUSH" = false ]; then
    echo ""
    echo "Next steps:"
    echo "  1. git push -u origin HEAD                      # push branch (NOT tag)"
    echo "  2. Create PR, get approval, merge"
    echo "  3. git checkout master && git pull origin master"
    echo "  4. git tag -d v${NEW_NAME} || true               # delete old tag"
    echo "  5. git tag -a v${NEW_NAME} -m 'Release v${NEW_NAME}'  # retag on master"
    echo "  6. git push origin v${NEW_NAME}                  # triggers release"
fi
