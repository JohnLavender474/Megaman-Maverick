import shutil
import types
from pathlib import Path

import level_builder as lb

HERE = Path(__file__).parent
ASSET = HERE / 'assets' / 'test-level.tmx'
EXPECTED = HERE / 'expected'
TEMP = HERE / 'temp'


def _ns(**kwargs):
    return types.SimpleNamespace(**kwargs)


def test_build_scenario_matches_expected_at_each_step():
    TEMP.mkdir(exist_ok=True)
    work = TEMP / 'work.tmx'
    shutil.copy(ASSET, work)
    f = str(work)

    # (expected-file-stem, action) — must match how expected/ was generated.
    steps = [
        ('01_add_room1',
         lambda: lb.cmd_add_room(_ns(file=f, name='room1', col=0, row=0, w=16, h=14))),
        ('02_add_room2',
         lambda: lb.cmd_add_room(_ns(file=f, name='room2', col=16, row=0, w=16, h=14))),
        ('03_add_floor',
         lambda: lb.cmd_add_block(_ns(file=f, col=0, row=12, w=32, h=2))),
        ('04_add_leftwall',
         lambda: lb.cmd_add_block(_ns(file=f, col=0, row=0, w=1, h=14))),
        ('05_add_spawn',
         lambda: lb.cmd_add_spawn(_ns(file=f, name='0', col=1, row=11))),
        ('06_add_enemy',
         lambda: lb.cmd_add_marker(_ns(file=f, layer='enemies', name='?', col=20, row=11))),
        ('07_insert_cols',
         lambda: lb.cmd_insert_cols(_ns(file=f, at_col=16, count=16))),
    ]

    for stem, action in steps:
        action()
        expected = (EXPECTED / f'{stem}.tmx').read_text()
        assert work.read_text() == expected, f'mismatch after step {stem}'
