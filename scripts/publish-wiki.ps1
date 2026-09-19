#!/usr/bin/env pwsh
#
# Renders the Chinese documentation as GitHub wiki pages.
#
# The wiki is a git repository of its own, its pages are plain files named after the page, and a link
# between two pages names the page rather than a path. The Chinese pages therefore cannot be copied
# across unchanged: the language switch belongs to a repository that carries both languages, every link
# to another page has to become that page's name, and a link to anything the wiki does not carry has to
# point back at the repository instead.
#
# This addon publishes its syntax list nowhere else: it registers through Skript's addon API, which
# skUnity's automatic jar import cannot read (see CONTRIBUTION.md), so the wiki is what a user reads and
# the pages under `docs/guide/` are the source they are rendered from.
#
# This file is deliberately plain ASCII, and everything a reader sees as Chinese lives in the two data
# files beside it:
#
#   wiki-pages.tsv    which repository file becomes which page, in sidebar order
#   wiki-sidebar.md   the sidebar, which is the table of contents on every page
#
# That is not decoration. Windows PowerShell reads a script without a byte order mark as text in the
# local code page, so Chinese in here would parse on one machine and not on another, and an editor that
# drops the mark would break it again. Data files are read as UTF-8 explicitly, which is stable.
#
# The script writes files and nothing else. The caller supplies a working copy of the wiki and decides
# what to do with it, which keeps credentials out of here and lets the same rendering be run at a desk,
# against a clone, as runs in the workflow.

[CmdletBinding()]
param(
    # A working copy of the wiki repository, such as the directory a workflow checks out.
    [Parameter(Mandatory = $true)]
    [string]$WikiDirectory,

    # Where the documentation is read from. The default is the repository this script lives in.
    [string]$RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path,

    [string]$RepositoryUrl = 'https://github.com/heyhey123-git/xiaojie-gui',

    [string]$Branch = 'master'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$pagesFile = Join-Path $PSScriptRoot 'wiki-pages.tsv'
$sidebarFile = Join-Path $PSScriptRoot 'wiki-sidebar.md'

# ReadAllText defaults to UTF-8, and strips a byte order mark if the file carries one.
function Read-Text {
    param([string]$Path)
    return [System.IO.File]::ReadAllText($Path)
}

function Write-Text {
    param([string]$Path, [string]$Text)
    $utf8 = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Text, $utf8)
}

# The mapping, in the order the sidebar lists the pages. A page name may not contain a space: the wiki
# shows a dash where one was, and a link would then stop resolving.
$pages = [ordered]@{}
foreach ($line in ((Read-Text $pagesFile) -replace "`r`n", "`n") -split "`n") {
    $entry = $line.Trim()
    if ($entry -eq '' -or $entry.StartsWith('#')) {
        continue
    }
    $parts = $entry -split "`t"
    if ($parts.Count -ne 2) {
        throw "Every line of wiki-pages.tsv is a file name, a tab, and a page name: '$entry' is not."
    }
    $pages[$parts[0]] = $parts[1]
}
if ($pages.Count -eq 0) {
    throw "wiki-pages.tsv names no pages."
}

$sidebar = (Read-Text $sidebarFile) -replace "`r`n", "`n"

# Pages are recognised by file name, so a link written as `reading.zh-CN.md` resolves from any
# directory. Two sources with the same file name would make that ambiguous, so it is refused here.
$pageByFileName = @{}
$knownPages = @{}
foreach ($file in $pages.Keys) {
    $fileName = [System.IO.Path]::GetFileName($file)
    if ($pageByFileName.ContainsKey($fileName)) {
        throw "Two wiki pages would answer to the name '$fileName'."
    }
    $pageByFileName[$fileName] = $pages[$file]
    $knownPages[$pages[$file]] = $true
}

# The sidebar and the mapping are two files, and a page missing from either is the mistake that is easy
# to make, so both directions are checked before anything is written.
$problems = @()
foreach ($page in $pages.Values) {
    if ($sidebar -notmatch "\]\($([regex]::Escape($page))\)") {
        $problems += "The sidebar does not link to the page '$page'."
    }
}
foreach ($match in [regex]::Matches($sidebar, '\]\(([^)]+)\)')) {
    $target = $match.Groups[1].Value
    if ($target -match '^[a-zA-Z][a-zA-Z0-9+.-]*:') {
        continue
    }
    if (-not $knownPages.ContainsKey($target)) {
        $problems += "The sidebar links to '$target', which is not a page in wiki-pages.tsv."
    }
}

function Convert-Page {
    param([string]$Text, [string]$SourcePath)

    # The directory the page lives in, as a repository-relative path. A link that leaves the wiki is
    # resolved by walking from there with the link's own segments. That is text rather than a file
    # system, so it behaves the same on every platform, and it is the only reason this script does not
    # need a path API: an earlier version asked `System.Uri` for the difference between two paths and
    # worked on Windows while failing on the runner, where an absolute path is a relative URI.
    $directory = @()
    $sourceParts = $SourcePath -split '/'
    if ($sourceParts.Count -gt 1) {
        $directory = @($sourceParts[0..($sourceParts.Count - 2)])
    }

    $normalised = $Text -replace "`r`n", "`n"
    $kept = foreach ($line in $normalised -split "`n") {
        # The switch between the two languages sits at the top and says `... | [English](...)`. It is
        # for the repository, which carries both languages, and has no meaning on a wiki with one.
        if ($line -match '\[English\]\(' -and $line -match '\|') {
            continue
        }
        $line
    }

    # Dropping that line leaves the blank line that followed it behind.
    $body = ($kept -join "`n") -replace "`n{3,}", "`n`n"

    return [regex]::Replace($body, '\]\(([^)]+)\)', {
            param($match)

            $target = $match.Groups[1].Value
            if ($target -match '^[a-zA-Z][a-zA-Z0-9+.-]*:' -or $target.StartsWith('#')) {
                return $match.Value
            }

            $anchor = ''
            $path = $target
            $hash = $path.IndexOf('#')
            if ($hash -ge 0) {
                $anchor = $path.Substring($hash)
                $path = $path.Substring(0, $hash)
            }
            if ($path -eq '') {
                return $match.Value
            }

            $fileName = [System.IO.Path]::GetFileName($path)
            if ($pageByFileName.ContainsKey($fileName)) {
                return "]($($pageByFileName[$fileName])$anchor)"
            }

            # Not a page: the wiki carries no such file, so the link has to leave for the repository.
            $segments = @($directory)
            foreach ($segment in (($path -replace '\\', '/') -split '/')) {
                if ($segment -eq '' -or $segment -eq '.') {
                    continue
                }
                if ($segment -eq '..') {
                    if ($segments.Count -eq 0) {
                        throw "$SourcePath links to '$target', which is outside the repository."
                    }
                    $segments = @($segments | Select-Object -SkipLast 1)
                    continue
                }
                $segments += $segment
            }
            if ($segments.Count -eq 0) {
                throw "$SourcePath links to '$target', which names the repository itself."
            }

            return "]($RepositoryUrl/blob/$Branch/$($segments -join '/')$anchor)"
        })
}

# Rendered in memory first, so a page that cannot be linked is reported before anything is written.
$rendered = [ordered]@{}
foreach ($file in $pages.Keys) {
    $source = Join-Path $RepositoryRoot $file
    if (-not (Test-Path -Path $source -PathType Leaf)) {
        throw "The documentation file '$file' is missing, but wiki-pages.tsv maps it to a page."
    }
    $rendered[$pages[$file]] = Convert-Page -Text (Read-Text $source) -SourcePath $file
}

foreach ($page in $rendered.Keys) {
    foreach ($match in [regex]::Matches($rendered[$page], '\]\(([^)]+)\)')) {
        $target = $match.Groups[1].Value
        if ($target -match '^[a-zA-Z][a-zA-Z0-9+.-]*:' -or $target.StartsWith('#')) {
            continue
        }
        $path = ($target -split '#')[0]
        if (-not $knownPages.ContainsKey($path)) {
            $problems += "$page links to '$target', which is not a page in this wiki."
        }
    }
}
if ($problems.Count -gt 0) {
    throw ($problems -join [System.Environment]::NewLine)
}

if (-not (Test-Path -Path $WikiDirectory -PathType Container)) {
    throw "The wiki directory '$WikiDirectory' does not exist."
}
if (-not (Test-Path -Path (Join-Path $WikiDirectory '.git'))) {
    Write-Warning "'$WikiDirectory' is not a checkout of the wiki repository; the pages are written but nothing here will commit them."
}

# A page left over from an earlier run, or one made by hand in the web interface, would otherwise stay
# up there for good: the wiki is not a mirror of the repository unless it is emptied first.
$carried = @{}
foreach ($page in $rendered.Keys) {
    $carried["$page.md"] = $true
}
$carried['_Sidebar.md'] = $true
$carried['_Footer.md'] = $true

$stale = @(
    Get-ChildItem -Path $WikiDirectory -Recurse -File -Filter '*.md' |
        Where-Object {
            $_.FullName -notmatch '[\\/]\.git[\\/]' -and -not $carried.ContainsKey($_.Name)
        }
)

$updated = 0
$unchanged = 0
foreach ($page in $rendered.Keys) {
    $target = Join-Path $WikiDirectory "$page.md"
    $previous = if (Test-Path -Path $target -PathType Leaf) {
        (Read-Text $target) -replace "`r`n", "`n"
    } else {
        $null
    }

    if ($previous -eq $rendered[$page]) {
        $unchanged++
        continue
    }
    Write-Text -Path $target -Text $rendered[$page]
    Write-Host "publish-wiki: $page (updated)"
    $updated++
}

$sidebarPath = Join-Path $WikiDirectory '_Sidebar.md'
$previousSidebar = if (Test-Path -Path $sidebarPath -PathType Leaf) {
    (Read-Text $sidebarPath) -replace "`r`n", "`n"
} else {
    $null
}
if ($previousSidebar -ne $sidebar) {
    Write-Text -Path $sidebarPath -Text $sidebar
    Write-Host 'publish-wiki: _Sidebar (updated)'
    $updated++
} else {
    $unchanged++
}

$removed = 0
foreach ($file in $stale) {
    Remove-Item -Path $file.FullName -Force
    Write-Host "publish-wiki: $($file.BaseName) (removed, no longer a page)"
    $removed++
}

$total = $rendered.Count + 1
if ($updated -eq 0 -and $removed -eq 0) {
    Write-Host "publish-wiki: the wiki already matches the documentation ($total pages)."
} else {
    Write-Host "publish-wiki: $total pages, $updated changed, $removed removed."
}
