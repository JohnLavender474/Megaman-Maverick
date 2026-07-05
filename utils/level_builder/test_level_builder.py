import shutil
import subprocess
from pathlib import Path

HERE = Path(__file__).parent
ASSET = HERE / 'assets' / 'test-level.tmx'
EXPECTED = HERE / 'expected'
TEMP = HERE / 'temp'
ANALYZER = HERE.parent / 'level_analyzer' / 'run.sh'
WORK = 'temp/work.tmx'


def _run(cmd):
    result = subprocess.run(cmd, shell=True, cwd=HERE, capture_output=True, text=True)
    assert result.returncode == 0, f'command failed: {cmd}\n{result.stdout}\n{result.stderr}'
    return result


def _analyze(label, path):
    result = subprocess.run([str(ANALYZER), str(path)], capture_output=True, text=True)
    output = (result.stdout if result.returncode == 0 else result.stderr).strip()
    print(f'\n--- analyzer: {label} ---')
    print(output or '(no output)')


def _pause(interactive, msg):
    if interactive:
        input(f'\n[interactive] {msg} — press Enter to continue... ')


def test_build_scenario_matches_expected_at_each_step(interactive):
    TEMP.mkdir(exist_ok=True)
    work = TEMP / 'work.tmx'
    shutil.copy(ASSET, work)

    # Each command below is copy/pasteable as-is in a terminal from this
    # directory (utils/level_builder), e.g. `./run.sh add-room ...`.
    steps = [
        ('01_add_room1', f'./run.sh add-room {WORK} room1 --col 0 --row 0 --w 16 --h 14'),
        ('02_add_room2', f'./run.sh add-room {WORK} room2 --col 16 --row 0 --w 16 --h 14'),
        ('03_add_floor', f'./run.sh add-block {WORK} --col 0 --row 0 --w 32 --h 2'),
        ('04_add_leftwall', f'./run.sh add-block {WORK} --col 0 --row 0 --w 1 --h 14'),
        ('05_add_spawn', f'./run.sh add-spawn {WORK} 0 --col 1 --row 2'),
        ('06_add_enemy', f"./run.sh add-marker {WORK} --layer enemies --name '?' --col 20 --row 2"),
        ('07_insert_cols', f'./run.sh insert-cols {WORK} --at-col 16 --count 16'),
        ('08_resize_block', f'./run.sh resize {WORK} --id 4 --w 2'),
        # Ledge: cols 2-5, a single tile row, top surface at row 8 (row 7 + h 1).
        ('09_add_ledge', f'./run.sh add-block {WORK} --col 2 --row 7 --w 4 --h 1'),
        # Spawn moved onto the ledge: within its col range [2, 5], standing on its top surface (row 8).
        ('10_move_spawn', f'./run.sh remove {WORK} --id 5 && ./run.sh add-spawn {WORK} 0 --col 4 --row 8'),
        ('11_resize_room', f'./run.sh resize {WORK} --id 1 --w 32'),
        # Widen the floor under room1 (id 3) from 2 to 5 tiles tall before placing water against it.
        ('12_resize_floor', f'./run.sh resize {WORK} --id 3 --h 5'),
        # Match the floor under room2 (id 7) to the same height as id 3.
        ('13_resize_floor2', f'./run.sh resize {WORK} --id 7 --h 5'),
        # The enemy (on top of id 7) would now be buried inside the taller floor; move it up onto
        # the new top surface (row 5: floor top row 4 + 1). Its col (36) already reflects insert-cols.
        ('14_move_enemy',
         f'./run.sh remove {WORK} --id 6 && '
         f"./run.sh add-marker {WORK} --layer enemies --name '?' --col 36 --row 5"),
        # Add 5 blank rows below row 0; row 0 and everything above (the rooms, floors) shift their
        # row number up by 5. Do this before adding water so "row 0" below refers to the final map.
        ('15_insert_rows', f'./run.sh insert-rows {WORK} --at-row 0 --count 5'),
        # Extend the three ground blocks (ids 3, 4, 7) down to the new row 0, keeping their tops
        # fixed. All three currently bottom out at row 5 (5 rows above the new bottom), so growing
        # each downward by 5 tiles brings its bottom to row 0 without disturbing its top or its id.
        ('16_extend_blocks_to_row0',
         f'./run.sh translate-height {WORK} --id 3 --by 5 --downward && '
         f'./run.sh translate-height {WORK} --id 4 --by 5 --downward && '
         f'./run.sh translate-height {WORK} --id 7 --by 5 --downward'),
        # Water fills the gap between the two floor blocks: col 16 through col 31, row 0 through
        # row 7 (row=0, h=8) — i.e. the 5 newly inserted rows plus the bottom 3 rows of the rooms.
        ('17_add_water', f'./run.sh add-water {WORK} --col 16 --row 0 --w 16 --h 8'),
        # Death spans row 0 through row 3 (row=0, h=4), full width. Its top (row 3) leaves exactly
        # one empty tile (row 4) between it and the rooms' bottom (row 5), and it's off-screen
        # (below the rooms' own row range) so it only catches a player who falls through the water.
        ('18_add_death', f'./run.sh add-death {WORK} --col 0 --row 0 --w 48 --h 4'),
    ]

    _analyze('start', work)

    for stem, cmd in steps:
        _pause(interactive, f'about to run step {stem}:\n  {cmd}')
        _run(cmd)
        expected = (EXPECTED / f'{stem}.tmx').read_text()
        assert work.read_text() == expected, f'mismatch after step {stem}'
        _analyze(stem, work)
        _pause(interactive, f'step {stem} complete')

    _analyze('end', work)
