#!/usr/bin/env bash
# Run the diagnostics analyzer inside a local virtual environment.
#
# Usage:
#   ./run.sh                          # auto-detect newest diagnostics file
#   ./run.sh path/to/file.txt         # explicit file
#   ./run.sh --smooth 60              # wider smoothing window
#   ./run.sh --output-dir /tmp/out    # custom output directory
#   ./run.sh --help                   # show all options
#
# The venv is created on first run and reused on subsequent runs.
# Re-run the script after editing requirements.txt to pick up new packages.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENV_DIR="$SCRIPT_DIR/.venv"
REQ_FILE="$SCRIPT_DIR/requirements.txt"

# ---- create venv if it doesn't exist ----
if [ ! -d "$VENV_DIR" ]; then
    echo "[run.sh] Creating virtual environment at $VENV_DIR …"
    python3 -m venv "$VENV_DIR"
fi

# ---- activate ----
# shellcheck disable=SC1091
source "$VENV_DIR/bin/activate"

# ---- install / sync dependencies ----
pip install --quiet --upgrade pip
pip install --quiet -r "$REQ_FILE"

# ---- run ----
python3 "$SCRIPT_DIR/analyze.py" "$@"
