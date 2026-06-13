# Editing Tile CSV Data Programmatically

Bulk edits (filling a floor, adding walls for a new room) must be done with a script —
hand-editing hundreds of comma-separated values is error-prone.

## Recommended approach

```python
import xml.etree.ElementTree as ET

ET.register_namespace('', '')  # preserve namespaces if needed
tree = ET.parse('assets/tiled_maps/tmx/LEVEL.tmx')
root = tree.getroot()

data = root.find('.//layer[@name="tiles1"]/data')
rows = [row.split(',') for row in data.text.strip().split('\n')]

# Example: fill floor at rows 48–49, cols 66–81 with tile 190
for r in range(48, 50):
    for c in range(66, 82):
        rows[r][c] = '190'

data.text = '\n' + '\n'.join(','.join(row) for row in rows) + '\n'
tree.write('assets/tiled_maps/tmx/LEVEL.tmx', xml_declaration=True, encoding='UTF-8')
```

## Coordinate mapping
- Row index `r` → pixel y = `r * 32`
- Col index `c` → pixel x = `c * 32`
- Room at pixel `(px, py)` → start row = `py // 32`, start col = `px // 32`
