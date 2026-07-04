#!/usr/bin/env bash
# Convert a Tiled TMX level file into plain-text room grids.
#
# Usage:
#   ./run.sh <path/to/level.tmx>                 # print to stdout
#   ./run.sh <path/to/level.tmx> <output.txt>    # write to a file
#   ./run.sh --help                              # show script help

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

python3 "$SCRIPT_DIR/level_analyzer.py" "$@"
