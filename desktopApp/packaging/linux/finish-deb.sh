#!/usr/bin/env bash
# Completes the deb that jpackage builds with what Linux software centers read, and rebuilds it in place:
#  - control: a Homepage field (the "Project Website" link) and a long description;
#  - AppStream metainfo in /usr/share/metainfo, which lists the installed app with its icon, license and links;
#  - the icon in the hicolor theme, which the metainfo and the desktop file refer to.
# Before installation a software center shows only the control fields: PackageKit reports every deb's license as
# "unknown" and reads no icon from a package file, so those appear once the app is installed.
#
# Run by the deb packaging tasks (DesktopApplicationPlugin); needs dpkg-deb, so Linux only.
# Usage: finish-deb.sh <deb> <homepage URL>
set -euo pipefail

deb=$1
homepage=$2
packaging=$(cd "$(dirname "$0")" && pwd)
app_id=io.buszi.nascope

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
root=$work/root
dpkg-deb --raw-extract "$deb" "$root"
control=$root/DEBIAN/control

version=$(sed -n 's/^Version: //p' "$control")
sed -e "s|@HOMEPAGE@|$homepage|g" \
    -e "s|@VERSION@|${version%%-*}|g" \
    -e "s|@DATE@|$(date -u +%F)|g" \
    "$packaging/$app_id.metainfo.xml" > "$work/metainfo.xml"
install -D -m 644 "$work/metainfo.xml" "$root/usr/share/metainfo/$app_id.metainfo.xml"
for size in 128 256 512; do
    install -D -m 644 "$packaging/icons/$size.png" "$root/usr/share/icons/hicolor/${size}x${size}/apps/$app_id.png"
done
# jpackage's desktop file points at one PNG by path; the themed icon gives menus, docks and software centers every size.
sed -i "s|^Icon=.*|Icon=$app_id|" "$root"/opt/nascope/lib/*.desktop

# The long description goes right after the Description line, as continuation lines (" ." for an empty line).
# Lines this script added before are dropped first, so running it twice changes nothing; so are empty lines (jpackage
# ends the file with one), which would start a second paragraph.
awk -v description="$packaging/description.txt" -v homepage="$homepage" '
    /^Homepage:/ || /^Installed-Size:/ { next }
    /^ / || /^$/ { next }
    /^Description:/ {
        print
        while ((getline line < description) > 0) print (line == "" ? " ." : " " line)
        next
    }
    { print }
    END { print "Homepage: " homepage }
' "$control" > "$work/control"
installed_size=$(du -sk --exclude=DEBIAN "$root" | cut -f1)
echo "Installed-Size: $installed_size" >> "$work/control"
install -m 644 "$work/control" "$control"

dpkg-deb --root-owner-group --build "$root" "$deb" >/dev/null
echo "Finished $deb"
