#!/bin/bash
set -e

cd "$(dirname "$0")/.."

# Build distributions
./gradlew --no-daemon --console=plain clean build -x test

OUTDIR="build/distributions"
if [ ! -d "$OUTDIR" ]; then
    echo "Distributions directory not found: $OUTDIR"
    exit 1
fi

SUMFILE="$OUTDIR/SHA256SUMS.txt"
rm -f "$SUMFILE"

# Generate checksums
cd "$OUTDIR"
for f in *.zip *.tar; do
    if [ -f "$f" ]; then
        sha256sum "$f" >> "SHA256SUMS.txt"
    fi
done

echo "Release artifacts ready in $OUTDIR"
exit 0
