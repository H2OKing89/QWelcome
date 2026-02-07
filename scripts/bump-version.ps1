<#
.SYNOPSIS
    Bump version, update changelog, commit, tag, and optionally push.

.DESCRIPTION
    A Windows-friendly version bump script for Q Welcome.
    Updates version.properties, CHANGELOG.md, creates a git commit and tag.

.PARAMETER Bump
    Version bump type: major, minor, patch, or explicit X.Y.Z

.PARAMETER Push
    Push commit and tag to remote after creation

.PARAMETER Force
    Skip changelog content validation

.EXAMPLE
    .\bump-version.ps1 patch
    .\bump-version.ps1 minor -Push
    .\bump-version.ps1 2.4.0 -Force -Push

.NOTES
    Requires git to be installed and in PATH.
#>

param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Bump,

    [switch]$Push,
    [switch]$Force
)

$ErrorActionPreference = "Stop"

# ── Resolve project root ──────────────────────────────────────────────
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir

$VersionFile = Join-Path $RootDir "version.properties"
$ChangelogFile = Join-Path $RootDir "CHANGELOG.md"

# ── Read current version ──────────────────────────────────────────────
if (-not (Test-Path $VersionFile)) {
    Write-Error "Error: $VersionFile not found."
}

$versionContent = Get-Content $VersionFile -Raw
if ($versionContent -match 'VERSION_NAME=(\d+)\.(\d+)\.(\d+)') {
    $Major = [int]$Matches[1]
    $Minor = [int]$Matches[2]
    $Patch = [int]$Matches[3]
    $CurrentName = "$Major.$Minor.$Patch"
} else {
    Write-Error "Could not parse VERSION_NAME from $VersionFile"
}

if ($versionContent -match 'VERSION_CODE=(\d+)') {
    $CurrentCode = [int]$Matches[1]
} else {
    Write-Error "Could not parse VERSION_CODE from $VersionFile"
}

# ── Compute new version ──────────────────────────────────────────────
switch ($Bump.ToLower()) {
    "major" {
        $Major++
        $Minor = 0
        $Patch = 0
    }
    "minor" {
        $Minor++
        $Patch = 0
    }
    "patch" {
        $Patch++
    }
    default {
        # Treat as explicit version (X.Y.Z)
        if ($Bump -match '^(\d+)\.(\d+)\.(\d+)$') {
            $Major = [int]$Matches[1]
            $Minor = [int]$Matches[2]
            $Patch = [int]$Matches[3]
        } else {
            Write-Error "Invalid version '$Bump'. Use major, minor, patch, or X.Y.Z."
        }
    }
}

$NewName = "$Major.$Minor.$Patch"
$NewCode = $CurrentCode + 1

Write-Host "Version: $CurrentName -> $NewName" -ForegroundColor Cyan
Write-Host "Code:    $CurrentCode -> $NewCode" -ForegroundColor Cyan

# ── Validate changelog ────────────────────────────────────────────────
if (-not $Force) {
    if (-not (Test-Path $ChangelogFile)) {
        Write-Error "Error: $ChangelogFile not found. Use -Force to skip."
    }

    $changelogContent = Get-Content $ChangelogFile -Raw
    
    # Extract content between [Unreleased] and the next ## heading
    if ($changelogContent -match '(?s)## \[Unreleased\]\s*\n(.*?)(?=\n## \[)') {
        $unreleasedContent = $Matches[1].Trim()
        if ([string]::IsNullOrWhiteSpace($unreleasedContent) -or $unreleasedContent -eq "No unreleased changes.") {
            Write-Host ""
            Write-Error "Error: No content under [Unreleased] in CHANGELOG.md.`n       Add changelog entries before bumping, or use -Force to skip."
        }
    }
}

# ── Check for staged changes ──────────────────────────────────────────
Push-Location $RootDir
try {
    $staged = git diff --cached --name-only 2>$null
    if ($staged) {
        Write-Host "`nError: There are already staged changes:" -ForegroundColor Red
        Write-Host $staged -ForegroundColor Yellow
        Write-Error "Please unstage them before running this script."
    }

    # ── Update version.properties ────────────────────────────────────────
    @"
VERSION_NAME=$NewName
VERSION_CODE=$NewCode
"@ | Set-Content $VersionFile -NoNewline
    Write-Host "Updated $VersionFile" -ForegroundColor Green

    # ── Update CHANGELOG.md ──────────────────────────────────────────────
    # Uses a state machine to collect [Unreleased] content, move it under a
    # new versioned heading, and reset [Unreleased] to the sentinel text.
    if (Test-Path $ChangelogFile) {
        $today = Get-Date -Format "yyyy-MM-dd"
        $changelogLines = Get-Content $ChangelogFile
        $newLines = [System.Collections.Generic.List[string]]::new()
        $unreleasedLines = [System.Collections.Generic.List[string]]::new()
        $state = "before"  # before | collecting | after

        foreach ($line in $changelogLines) {
            switch ($state) {
                "before" {
                    if ($line -match '^\s*## \[Unreleased\]') {
                        $state = "collecting"
                    } else {
                        $newLines.Add($line)
                    }
                }
                "collecting" {
                    if ($line -match '^\s*## \[') {
                        # Hit the next version heading — done collecting
                        # Now emit the restructured sections
                        $newLines.Add("## [Unreleased]")
                        $newLines.Add("")
                        $newLines.Add("No unreleased changes.")
                        $newLines.Add("")
                        $newLines.Add("## [$NewName] - $today")

                        # Add collected unreleased content (skip old "No unreleased changes." sentinel)
                        foreach ($ul in $unreleasedLines) {
                            if ($ul.Trim() -ne "No unreleased changes.") {
                                $newLines.Add($ul)
                            }
                        }

                        # Add the version heading we just hit
                        $newLines.Add($line)
                        $state = "after"
                    } else {
                        $unreleasedLines.Add($line)
                    }
                }
                "after" {
                    $newLines.Add($line)
                }
            }
        }

        # Handle case where [Unreleased] is the last section (no next ## heading found)
        if ($state -eq "collecting") {
            $newLines.Add("## [Unreleased]")
            $newLines.Add("")
            $newLines.Add("No unreleased changes.")
            $newLines.Add("")
            $newLines.Add("## [$NewName] - $today")
            foreach ($ul in $unreleasedLines) {
                if ($ul.Trim() -ne "No unreleased changes.") {
                    $newLines.Add($ul)
                }
            }
        }

        $newLines -join "`n" | Set-Content $ChangelogFile -NoNewline
        Write-Host "Updated $ChangelogFile" -ForegroundColor Green
    }

    # ── Git commit and tag ────────────────────────────────────────────────
    git add $VersionFile
    if (Test-Path $ChangelogFile) {
        git add $ChangelogFile
    }
    
    # Use --no-verify to skip pre-commit hook (which blocks direct commits to master)
    git commit --no-verify -m "release: v$NewName (code $NewCode)"
    git tag -a "v$NewName" -m "Release v$NewName"

    Write-Host "`nCreated commit and tag v$NewName" -ForegroundColor Green

    # ── Optional push ─────────────────────────────────────────────────────
    if ($Push) {
        Write-Host "`nPushing to remote..." -ForegroundColor Yellow
        git push --follow-tags
        Write-Host "Pushed to remote." -ForegroundColor Green
    }

    Write-Host "`n✅ Done! Release v$NewName (code $NewCode) is ready." -ForegroundColor Cyan
    
    if (-not $Push) {
        Write-Host "`nTo push and trigger the release build:" -ForegroundColor Yellow
        Write-Host "  git push --follow-tags" -ForegroundColor White
    }

} finally {
    Pop-Location
}
