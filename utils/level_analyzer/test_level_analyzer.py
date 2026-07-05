import json
from pathlib import Path

from level_analyzer import analyze, analyze_json

HERE = Path(__file__).parent
ASSET = HERE / 'assets' / 'test-level.tmx'
EXPECTED = HERE / 'expected' / 'test-level.txt'
EXPECTED_JSON = HERE / 'expected' / 'test-level.json'
TEMP = HERE / 'temp'


def test_analyze_matches_expected():
    TEMP.mkdir(exist_ok=True)
    out_file = TEMP / 'test-level.txt'

    output = analyze(ASSET)
    out_file.write_text(output)

    assert out_file.read_text() == EXPECTED.read_text()


def test_analyze_json_matches_expected():
    TEMP.mkdir(exist_ok=True)
    out_file = TEMP / 'test-level.json'

    output = json.dumps(analyze_json(ASSET), indent=2) + '\n'
    out_file.write_text(output)

    assert out_file.read_text() == EXPECTED_JSON.read_text()
