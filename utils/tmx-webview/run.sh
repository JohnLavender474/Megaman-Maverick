#!/usr/bin/env bash
#
# Usage:
#   ./run.sh INPUT.viz.txt                  # render to /tmp/tmx-webview.html
#   ./run.sh INPUT.viz.txt -o out.html      # render to a specific path
#   ./run.sh --validate INPUT.viz.txt       # validate only; exit 1 if errors
#   ./run.sh --help

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENV_DIR="$SCRIPT_DIR/.venv"
REQ_FILE="$SCRIPT_DIR/requirements.txt"

if [ ! -d "$VENV_DIR" ]; then
    echo "[run.sh] Creating virtual environment at $VENV_DIR …"
    python3 -m venv "$VENV_DIR"
fi

source "$VENV_DIR/bin/activate"

pip install --quiet --upgrade pip
pip install --quiet -r "$REQ_FILE"

python3 "$SCRIPT_DIR/webview.py" "$@"
