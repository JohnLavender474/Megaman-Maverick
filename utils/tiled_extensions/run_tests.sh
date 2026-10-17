#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

node --test "$@" "$SCRIPT_DIR/test_generate_blocks.js"
