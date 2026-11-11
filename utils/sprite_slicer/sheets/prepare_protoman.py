import sys

import numpy as np
from PIL import Image

CELL_COLOR = (147, 187, 236)
HIGHLIGHT_COLOR = (38, 123, 218)
CELL_SIZE = 48
TOP = 12
CELLS = {"idle1": 3, "idle2": 52, "step": 102, "w1": 152, "w2": 201, "w3": 250, "j1": 300}
ROWS = [["idle1", "idle2"], ["step"], ["w1", "w2", "w3", "w2"], ["j1"]]
SLOT = 52


def cell(sheet, name):
    left = CELLS[name]
    image = sheet[TOP:TOP + CELL_SIZE, left:left + CELL_SIZE].copy()
    rgb = image[..., :3]
    keyed = (rgb == CELL_COLOR).all(2)
    if not keyed.any():
        keyed |= (rgb == HIGHLIGHT_COLOR).all(2)
    image[keyed] = 0
    return image[:, ::-1]


def main(source, target):
    sheet = np.array(Image.open(source).convert("RGBA"))
    out = np.zeros((len(ROWS) * SLOT, max(len(row) for row in ROWS) * SLOT, 4), dtype=np.uint8)
    for row, names in enumerate(ROWS):
        for column, name in enumerate(names):
            y, x = row * SLOT + 2, column * SLOT + 2
            out[y:y + CELL_SIZE, x:x + CELL_SIZE] = cell(sheet, name)
    Image.fromarray(out, "RGBA").save(target)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit("usage: prepare_protoman.py <source sheet> <prepared sheet>")
    main(sys.argv[1], sys.argv[2])
