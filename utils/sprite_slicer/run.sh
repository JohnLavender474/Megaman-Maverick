#!/usr/bin/env bash
# Cut a tightly packed sprite sheet into fixed-size animation strips.
#
# Usage:
#   ./run.sh sheets/MegamanX.yaml --frames-dir ../../assets/sprites/frames/MegamanX \
#       --atlas-dir ../../assets/sprites/sprite_sheets --preview-dir /tmp/x-preview
#
# The venv is created on first run and reused. Re-run after editing
# requirements.txt to pick up new packages.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENV_DIR="$SCRIPT_DIR/.venv"
REQ_FILE="$SCRIPT_DIR/requirements.txt"

if [ ! -d "$VENV_DIR" ]; then
    echo "[run.sh] Creating virtual environment at $VENV_DIR …"
    python3 -m venv "$VENV_DIR"
fi

# shellcheck disable=SC1091
source "$VENV_DIR/bin/activate"

pip install --quiet --upgrade pip
pip install --quiet -r "$REQ_FILE"

python3 "$SCRIPT_DIR/sprite_slicer.py" "$@"
