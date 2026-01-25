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
