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
        # Ladder in col 6, just right of the ledge (id 8, cols 2-5). It stands on the floor (id 3,
        # top surface row 10) and its top edge (row 10 + h 3 = row 13) is level with the ledge top.
        # The LadderTop goes in blocks: 32x8 px, same x and y as the ladder.
        ('19_add_ladder', f'./run.sh add-ladder {WORK} --col 6 --row 10 --h 3'),
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


GATES = 'temp/gates.tmx'


def _run_fails(cmd, message):
    result = subprocess.run(cmd, shell=True, cwd=HERE, capture_output=True, text=True)
    assert result.returncode != 0, f'command did not fail: {cmd}\n{result.stdout}'
    assert message in result.stderr, f'unexpected error for: {cmd}\n{result.stderr}'


def test_gates_and_boss_room_match_expected_at_each_step(interactive):
    TEMP.mkdir(exist_ok=True)
    work = TEMP / 'gates.tmx'
    work.unlink(missing_ok=True)

    # Layout (rows 0-13 at the bottom): room1 | room2 (boss) | room3, and room4 above room3.
    steps = [
        ('20_gates_rooms',
         f'./run.sh init {GATES} --width 48 --height 28 && '
         f'./run.sh add-room {GATES} room1 --col 0 --row 0 --w 16 --h 14 && '
         f'./run.sh add-room {GATES} room2 --col 16 --row 0 --w 16 --h 14 && '
         f'./run.sh add-room {GATES} room3 --col 32 --row 0 --w 16 --h 14 && '
         f'./run.sh add-room {GATES} room4 --col 32 --row 14 --w 16 --h 14 && '
         f'./run.sh add-block {GATES} --col 15 --row 0 --w 2 --h 14 && '
         f'./run.sh add-block {GATES} --col 31 --row 0 --w 2 --h 14 && '
         f'./run.sh add-block {GATES} --col 32 --row 13 --w 16 --h 2'),
        # Entry gate on the room1/room2 edge (col 16): player from the left, so no direction and
        # no key. Exit gate on the room2/room3 edge (col 32) takes the new boss_key 1.
        ('21_add_boss_room',
         f"./run.sh add-boss-room {GATES} room2 --boss '?' --boss-col 24 --boss-row 2 --mini "
         f'--entry-from room1 --entry-col 15 --entry-row 2 '
         f'--exit-to room3 --exit-col 31 --exit-row 2'),
        # A second entry into the boss room, from room3 on the right: direction=right.
        ('22_add_gate_from_right',
         f'./run.sh add-gate {GATES} --col 31 --row 8 --to room2 --from right'),
        # Up gate on the room3/room4 edge (row 14): 3x2 tiles, player from below: direction=down.
        ('23_add_gate_from_down',
         f'./run.sh add-gate {GATES} --col 39 --row 13 --to room4 --from down'),
    ]

    for stem, cmd in steps:
        _pause(interactive, f'about to run step {stem}:\n  {cmd}')
        _run(cmd)
        expected = (EXPECTED / f'{stem}.tmx').read_text()
        assert work.read_text() == expected, f'mismatch after step {stem}'
        _analyze(stem, work)
        _pause(interactive, f'step {stem} complete')

    before = work.read_text()
    _run_fails(f'./run.sh add-gate {GATES} --col 31 --row 8 --to room2 --from right --boss-key 1',
               'Refused: "room2" is a boss room')
    _run_fails(f'./run.sh add-gate {GATES} --col 31 --row 8 --to room2 --from left',
               '--from left does not match the rooms')
    _run_fails(f'./run.sh add-gate {GATES} --col 32 --row 8 --to room2 --from right',
               'does not sit on the shared edge')
    _run_fails(f'./run.sh add-gate {GATES} --col 31 --row 8 --to nowhere --from right',
               'No room named "nowhere"')
    _run_fails(f'./run.sh add-gate {GATES} --col 31 --row 2 --to room3 --from left --boss-key 2',
               'does not match boss_key=1')
    assert work.read_text() == before, 'a refused command changed the file'
