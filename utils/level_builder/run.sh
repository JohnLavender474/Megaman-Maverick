#!/usr/bin/env bash
# Build and edit Megaman-Maverick TMX levels via tile-unit subcommands.
#
# Usage:
#   ./run.sh <command> [args...]
#   ./run.sh --help                    # list all commands
#   ./run.sh <command> --help          # help for one command
#
# Examples:
#   ./run.sh init MyLevel.tmx --width 350 --height 100
#   ./run.sh add-room MyLevel.tmx room1 --col 2 --row 78 --w 76 --h 14
#   ./run.sh add-block MyLevel.tmx --col 0 --row 92 --w 23 --h 2
#   ./run.sh add-spawn MyLevel.tmx 0 --col 1 --row 90
#   ./run.sh add-marker MyLevel.tmx --layer enemies --name '?' --col 10 --row 88
#   ./run.sh insert-cols MyLevel.tmx --at-col 78 --count 16
#
# The script uses only the Python standard library, so no virtual environment
# or dependencies are required. TMX files live under assets/tiled_maps/tmx/.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

python3 "$SCRIPT_DIR/level_builder.py" "$@"
