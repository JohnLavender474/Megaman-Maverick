  #!/bin/bash
  # convert-to-ogg.sh
  # Converts all .wav and .mp3 audio files in assets/music and assets/sounds to .ogg
  # Originals are preserved with a .bak extension unless --delete-originals is passed

  set -e

  DRY_RUN=false
  DELETE_ORIGINALS=false

  for arg in "$@"; do
      case $arg in
          --dry-run) DRY_RUN=true ;;
          --delete-originals) DELETE_ORIGINALS=true ;;
      esac
  done

  if ! command -v ffmpeg &>/dev/null; then
      echo "Error: ffmpeg is not installed. Run: sudo apt install ffmpeg"
      exit 1
  fi

  convert_to_ogg() {
      local input="$1"
      local output="${input%.*}.ogg"

      if [ -f "$output" ]; then
          echo "  SKIP (ogg already exists): $output"
          return
      fi

      echo "  Converting: $input -> $output"
      if [ "$DRY_RUN" = false ]; then
          ffmpeg -i "$input" -c:a libvorbis -q:a 5 "$output" -loglevel error
          if [ "$DELETE_ORIGINALS" = true ]; then
              rm "$input"
          fi
      fi
  }

  echo "=== Converting music ==="
  for f in ../assets/music/*.wav ../assets/music/*.mp3; do
      [ -f "$f" ] || continue
      convert_to_ogg "$f"
  done

  echo "=== Converting sounds ==="
  for f in ../assets/sounds/*.wav ../assets/sounds/*.mp3; do
      [ -f "$f" ] || continue
      convert_to_ogg "$f"
  done

  echo "Done."
