#!/usr/bin/env bash
# Prints the CHANGELOG.md entries of one version: everything between its "## [<version>]" heading and the next
# "## [" heading, without the heading itself. Fails when the version has no section or the section is empty, so a
# release never goes out without its notes.
# Usage: changelog-section.sh <version> [changelog file, default CHANGELOG.md]
set -euo pipefail

version=$1
changelog=${2:-CHANGELOG.md}

section=$(awk -v heading="## [$version]" '
    index($0, heading) == 1 { found = 1; next }
    found && /^## \[/ { exit }
    found { print }
' "$changelog" | sed -e '/./,$!d') # drops the blank lines after the heading; $(...) drops the trailing ones

if [ -z "${section//[[:space:]]/}" ]; then
    echo "::error::$changelog has no entries under \"## [$version]\"" >&2
    exit 1
fi
printf '%s\n' "$section"
