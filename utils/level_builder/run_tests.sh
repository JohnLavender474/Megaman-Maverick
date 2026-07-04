#!/usr/bin/env bash
# Run the level-builder unit tests inside a local virtual environment.
#
# Usage:
#   ./run_tests.sh                # run all tests
#   ./run_tests.sh -k scenario    # pass through pytest args
#
# The venv is created on first run and reused. Re-run after editing
# requirements-test.txt to pick up new packages.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENV_DIR="$SCRIPT_DIR/.venv"
REQ_FILE="$SCRIPT_DIR/requirements-test.txt"

if [ ! -d "$VENV_DIR" ]; then
    echo "[run_tests.sh] Creating virtual environment at $VENV_DIR …"
    python3 -m venv "$VENV_DIR"
fi

# shellcheck disable=SC1091
source "$VENV_DIR/bin/activate"

pip install --quiet --upgrade pip
pip install --quiet -r "$REQ_FILE"

cd "$SCRIPT_DIR"
# -vv: full names + untruncated assertion diffs; -s: show the tool's own print()
# output live; -rA: summary line for every test. Extra args are passed through.
python3 -m pytest -vv -s -rA "$@"
