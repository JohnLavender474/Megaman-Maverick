from pathlib import Path

from level_analyzer import analyze

HERE = Path(__file__).parent
ASSET = HERE / 'assets' / 'test-level.tmx'
EXPECTED = HERE / 'expected' / 'test-level.txt'
TEMP = HERE / 'temp'


def test_analyze_matches_expected():
    TEMP.mkdir(exist_ok=True)
    out_file = TEMP / 'test-level.txt'

    output = analyze(ASSET)
    out_file.write_text(output)

    assert out_file.read_text() == EXPECTED.read_text()
