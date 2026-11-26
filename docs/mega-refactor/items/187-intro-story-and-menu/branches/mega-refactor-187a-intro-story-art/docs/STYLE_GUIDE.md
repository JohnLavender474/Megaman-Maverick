# NES Mega Man intro/cutscene style guide for the story-still redraw

Scope: analysis of three reference images (two ripped sprite sheets of NES-era Mega Man cutscene art, one screenshot of a capsule scene), compared against the fan game's own art, ending in a redraw plan for 7 intro slides. All numbers below were measured from the files at 1:1 (both sheets are at native 1x; one NES screen = 256 px wide). Nothing here should be traced; it is a description of construction rules.

Confidence notes are marked "(measured)", "(estimated)" or "(inferred)".

---

## 0. The five findings that matter most

1. **Colour count is tiny and it is per-region, not per-image.** The tall city skyline is drawn with **6 colours in total** (measured: sky light, sky dark, black, green, cyan, white). The night version uses **3**. The fortress uses 8 in full colour and **3** in each monochrome version. A character bust uses 5 to 7 including black.
2. **Black is a full structural colour, not just an outline.** In the day city, black covers about 12% of the picture; in the night city about 38%. Black is used for outlines, for the whole shadow side of buildings, for window grids, and for the ground mass.
3. **There are no gradients and almost no dithering.** Sky = one flat colour plus a second flat colour, joined by 3 to 4 thin horizontal bars. The only dither-like texture in all three references is foliage/ground texture (irregular speckle on bushes, grass, treetops), never on sky, light or skin.
4. **One drawing, many palettes.** The seven skyline variants are pixel-identical index maps (identical per-index pixel counts: 55534 / 21836 / 12118 / 3869 / ...). Only the colour table changes. Same for the fortress x3 and the second city x11.
5. **The first-pass stills already had a low colour count (8 to 15 per still, measured). Colour count was not the problem.** The problems are dither gradients, absent black outlines/shadow masses, realistic lighting, unlit silhouettes, and no characters. See section F.

---

## A. Overall NES look

### A1. Palette limits
- Hardware logic to imitate: background is built from 8x8 tiles grouped in 16x16 attribute cells; each cell picks one of 4 sub-palettes; each sub-palette is 3 colours plus one shared backdrop colour. Sprites: 3 colours plus transparent each.
- Practical result in the references (measured colour counts, whole image):
  - Tall skyline, day: 6. Dusk: 5. Night: 3.
  - Second (low, wide) city: 9 to 10 incl. black; night version about 7.
  - Fortress: 8 full colour; 3 in each mono variant.
  - Teal lab interior: 8. Dark navy lab interior: 7.
  - Hero bust: blue, dark blue, cyan, skin, white, black, plus 1 to 2 mouth colours = 7 to 8.
  - Crowd of silhouette robots behind the villain: 3 (mid purple, light lavender highlight, black) and the villain in front gets his own 6 to 7.
  - Text screens: 2 (black, white).
- Working rule for new art: **any 16x16 cell should contain at most 4 colours (3 plus the scene's shared backdrop/black)**. A whole still should land at **6 to 12 colours**; 16 is a hard ceiling, used only when a multi-colour character stands in front of a background.

### A2. The master colour set
Every colour seen in the references is one of roughly 35 NES values. Use these exact hexes (as sampled; the two sheets differ by about 4 per channel because of ripper palettes, e.g. `#0070E8` vs `#0070EC`. Pick the second column and stay consistent, it matches the fan game's sprites):

| Role | Hex |
|---|---|
| Black | `#000000` |
| White | `#FCFCFC` |
| Light grey / mid grey | `#BCBCBC` / `#747474` |
| Hero blue (mid) | `#0070EC` |
| Sky blue | `#3CBCFC` |
| Pale sky | `#A8E4FC` |
| Indigo / night sky | `#24188C` |
| Deep blue (shadow of hero blue) | `#0000A8` |
| Royal blue | `#2038EC` |
| Cornflower | `#5C94FC` |
| Navy slate (night, machinery) | `#183C5C` |
| Cyan (hero light) | `#00E8D8` |
| Teal | `#008088` |
| Mint | `#58F898` |
| Pale mint | `#B0FCCC` |
| Greens | `#4CDC48`, `#00A800`, `#009038`, `#008000` |
| Pale yellow-green (lit windows at night) | `#E0F8A0` |
| Violet / deep purple | `#400098` |
| Lavender | `#A088FC`, pale `#D4C8FC` |
| Magenta-purple | `#800080`, `#8C0074` |
| Crimson / hot pink | `#E40058` |
| Pink | `#FC74B4`, `#F478FC` |
| Red | `#D82800` |
| Dark red / maroon | `#A40000`, `#7C0800` |
| Salmon | `#FC7460` |
| Rust orange | `#C84C0C` |
| Orange | `#FC9838` |
| Gold | `#F0BC3C` |
| Cream | `#FCE4A0` |
| Skin | `#FCD8A8` |
| Olive | `#887000` |

Never invent in-between tones. If a colour is not in this table (or the full NES 54-colour table), do not use it.

### A3. How ramps are built
- A "ramp" is **2 steps, sometimes 3**: base plus one darker hue-shifted shade, plus optionally a white or pale highlight. Examples (measured):
  - Hero armour: `#0070EC` base, `#0000A8` (or indigo `#24188C`) shadow, black outline. Light parts are a different hue (`#00E8D8`), not a lighter blue.
  - Bone/skull: white base, sky blue `#3CBCFC` shadow, grey `#747474` deep shadow.
  - Silhouette crowd: `#400098` base, `#A088FC` rim highlight, black eyes/creases.
  - Red robot: `#D82800` base, `#A40000` shadow, white specular.
- Shadows shift hue toward blue/purple, highlights toward white or cyan. There is no "same hue, 5 lightness steps" ramp anywhere.

### A4. Shading
- Flat cel shading. Each form gets one shadow shape with a hard edge, drawn as a solid cluster, usually on the lower-right or underside.
- Shadow shapes are large: on a 54 px wide helmet, the shadow crescent is about 8 to 10 px wide. On the fortress, shadow sides of cylinders are 3 to 6 px wide stripes.
- Metal is shown with **horizontal or vertical highlight bars**: a 1 to 2 px white stripe next to a light-blue stripe on a grey or blue body. Glass is shown with one diagonal or curved white streak.
- No ambient occlusion, no cast-light cones, no glow halos. A light source is just a white or cream shape with at most a 1 px ring of the next colour.

### A5. Dithering
- **Sky, skin, armour, buildings, panels: none.**
- Where texture appears (measured by eye at 4x):
  - Bushes and treetops in the low city: two greens (`#00A800`, `#4CDC48`) broken with an irregular speckle of single pixels and 2 px clumps along the top edge and scattered inside; bottom band is black with green speckle.
  - Fortress ground: rust `#C84C0C` and maroon `#7C0800` in short diagonal hatch strokes, 3 to 6 px long, suggesting rock.
  - Night bushes: `#183C5C` speckle on black.
  - TV static in a monitor: a full-area 2-colour noise pattern (deliberate "static" effect).
  - Sea in the train scene: flat blue with scattered short horizontal dashes (2 to 6 px) of the lighter blue. That is the pattern for water or rain-wet surfaces.
- Dither is thus a **material texture for organic/rough surfaces**, never a way to fake a gradient. No ordered (Bayer/checker) fields.

### A6. Outlines
- **Colour: pure black, almost always.** Characters, foreground buildings, props, the capsule, the fortress pieces all have a continuous black outline.
- **Thickness: 1 px** on characters and most objects; it thickens to 2 to 4 px where the outline merges with a shadow side (building edges, undersides of ledges).
- Coloured (non-black) outlines appear in exactly three cases:
  1. Far-background silhouettes have **no outline at all**: they are a flat darker sky colour with holes cut for windows.
  2. Whole-image mono variants (fortress in blue), where black is remapped to another index.
  3. Interior detail lines on a form, where a darker hue is used instead of black to keep it soft (dark blue creases in blue fists, dark orange lines in skin: `#FC9838` or `#C84C0C` on `#FCD8A8`).
- Outline gaps: on white coats and pale shapes the outline may be drawn in grey or a mid tone on the lit side. Do this rarely.

### A7. Highlights
- White, hard edged, small: a 2x2 to 3x6 px blob on a helmet, a 1 px bar on a pipe, a 2x3 px glint in each eye.
- At most one highlight per form. Specular on glass domes is a single arc of white 1 px thick, 20 to 40 px long.

### A8. Cluster size and empty space
- Smallest meaningful cluster is about 2x2 px; single pixels are reserved for windows, rivets, eye glints, stars and foliage speckle.
- Windows on buildings are 1x2, 1x3, 2x2 or 2x3 px lights on a 3 to 4 px pitch grid. Line detail on buildings is 1 px.
- **Flat colour dominates.** Measured share of pure sky colour by row in the tall skyline: about 95% in the top third, 87% at mid height, 60% at two-thirds down, under 20% in the bottom 60 rows. Roughly **55% of the whole 256x400 picture is one flat sky colour** and another 22% is the second sky colour.
- Character panels: 55 to 70% of the panel is a single flat background colour.
- Text/capsule screens: 85%+ pure black.

---

## B. City backgrounds

Two different city drawings appear.

### B1. Tall skyline (256 x 400, built for a vertical pan)
Layout top to bottom (measured rows):
- Rows 0 to 47: flat dark sky colour.
- Rows 48 to 55: transition by horizontal bars, no dither. Sequence is 1 light, 2 dark, 1 light, 1 dark, 2 light, 1 dark, then light forever. Eight rows total.
- Rows 56 to about 114: flat light sky with **three clouds**. Each cloud is about 58 px wide and 20 to 24 px tall.
- Row 115 downward: first building tips enter. Antennas are 1 px wide vertical lines 15 to 30 px tall with a 1 px brighter tip.
- Bottom 100 rows: dense, nearly no sky.

Cloud construction: a lumpy top made of 4 to 6 semicircular bumps of 8 to 14 px diameter, flat-ish bottom; body pure white; a 2 to 3 px shadow band along the underside in the dark sky colour (index-shared with the upper sky so it recolours automatically); no outline; no dither.

Depth layers, back to front, using only the 6 colours:
1. **Far skyline**: solid dark-sky colour silhouettes with no outline. Classic stepped skyscraper shapes, 20 to 40 px wide, needle spires, one ball-on-a-stalk tower (a ring of windows around a sphere on a pole). Windows are light-sky-coloured 1x2 or 2x2 holes on a regular grid, so the far layer costs zero extra colours.
2. **Mid buildings**: same dark-sky colour bodies but now with a black shadow side (2 to 4 px) and white windows in ordered columns. Box-with-setbacks shapes; pilaster stripes 1 px wide.
3. **Near buildings**: saturated accent colours (green and cyan in the day version), full black outlines, black window frames with white or light panes, horizontal louver bars. These touch the left, right and bottom frame edges and are cropped by them.
4. **Feature elements**:
   - A transparent **tube/highway** arcing across the upper right, about 12 px thick, drawn with a dark underside line, a body in dark sky colour, and a white highlight line along the top. It ends in a dark circular mouth and sits on a tall thin support pylon.
   - A **floating/hanging garden dish** under a glass dome: a shallow bowl about 55 px wide, green top, black underside, tiny black tree silhouettes on it, and a 1 px white arc for the dome.
   - A **satellite dish** on a rooftop left (circle with a cross, about 14 px).
   - The tall green **spire building** left of centre: a stack of tapering blocks, 8 to 20 px wide, topped by a 1 px mast.
   - A left foreground tower drawn in strong oblique perspective, cut off by the frame.

Perspective: there is none in the real sense. Buildings are frontal elevations, some with a single oblique side plane (a parallelogram of darker colour or black). Rooflines are flat or stepped. Overlap alone creates depth.

How depth works with 2 to 4 colours: far = sky-dark flat; mid = sky-dark plus black side plus white windows; near = accent colour plus black plus white. Value contrast rises toward the viewer and black coverage rises toward the bottom.

### B2. Low wide city (256 x 160 picture area above a 64 px text band)
Measured structure:
- Rows 0 to 23: darker sky. Rows 24 to 31: the same 8-row bar transition. Rows 32 to about 80: pale sky.
- Buildings occupy the left 55% only, 40 to 75 px tall: three white cylinder towers with domed tops and rows of dotted windows, a tapered stepped tower with a mast (tallest element, about 85 px), a slab building with horizontal blinds and a cross-hatched side face, a small domed building.
- A cyan flat silhouette layer behind them (far skyline).
- Right 45%: a **solid black mountain** with a 2 to 3 px light green rim along its top edge.
- Bottom 35 rows: two-green speckled bushes, then an 8 px black band with green speckle.
- Rows 160 to 223: nothing (text sits here on black).

### B3. Time-of-day recolours
Tall skyline, 7 variants, same index map. Indices: A = lower sky and far windows, B = upper sky, far buildings, cloud shadow, K = black, G = accent 1, C = accent 2, W = white (clouds, windows, highlights).

| Variant | A sky | B upper/far | G | C | W | K | Logic |
|---|---|---|---|---|---|---|---|
| Day | `#38B8F8` | `#0070E8` | `#48D848` | `#00E8D8` | `#F8F8F8` | black | full colour, cool |
| Hazy morning | `#D0C8F8` | `#A088F8` | green | cyan | white | black | only the two sky indices change; buildings keep day accents |
| Pink | `#F870B0` | `#E00058` | green | cyan | white | black | sky goes pink/crimson, accents still day |
| Orange sunset | `#F89838` | `#D82800` | `#F87060` | `#F87060` | white | black | G and C **collapse to one salmon**: buildings lose local colour (5 colours) |
| Pink dusk | `#F870B0` | `#E00058` | `#F89838` | `#F89838` | white | black | accents collapse to orange (5 colours) |
| Twilight | `#400098` | `#201888` | `#008088` | `#008088` | `#98F8F0` | black | sky purple/indigo, accents collapse to teal, white dims to pale cyan |
| Night | `#183858` | **black** | **black** | **black** | `#E0F8A0` | black | everything except sky and lights is black: a pure silhouette with yellow-green windows (3 colours) |

Low wide city, 11 steps: pale blue day, lavender, magenta, pink, salmon, orange, salmon, pink, magenta, violet, indigo night. Building whites tint toward the sky colour at each step (white, pale pink, pale orange, back), bushes go green, olive, orange-brown, dark teal. At night the towers become `#0070EC` on `#24188C` sky with orange `#FC9838` window dots.

Fades are also done by palette: a fade-in is 3 to 4 steps where every colour is swapped for a darker neighbour (all black, then indigo/blue only, then full). No alpha blending.

Explosion over the night city: a pure white disc with a 1 px cream (`#FCE4A0`) and 1 px orange/pink rim, growing from a 10 px blob to a 60 px half-disc behind the skyline, then breaking into a thin arc (ring) of 2 to 3 px thickness. Buildings stay in front, unchanged. No glow, no lighting change on the buildings.

### B4. What "one drawing, many palettes" means for the new stills
- Draw **one indexed city image** with a fixed 6 to 8 entry index map and export palette variants. Slide 1 (day) and slide 5 (burning) must be the same pixels with different colour tables, plus sprite-like overlays (fires, fortress, smoke) for slide 5.
- Design indices so collapses work: put near-building accents on their own indices so that at "burning" they can be merged into one colour, and put far buildings on the upper-sky index so they recolour with the sky.
- Lit windows should be on the white/highlight index so they turn to fire-orange or yellow-green automatically.

---

## C. The fortress (composition study, generic shapes)

Canvas 256 x 448 (tall, for a pan). Measured layout:
- Rows 0 to 218: **completely flat sky colour**, nothing else (49% of the picture).
- Rows 219 to 234: sky ends in a **scalloped cloud edge**: 4 stacked rows of shallow downward arcs (each arc about 16 to 24 px wide, 2 to 4 px tall), getting thinner and sparser, then pure black. Below it, a few isolated 1 px arcs float in the black like distant clouds.
- Rows 234 to 438: black void with the structure. Structure bounding box spans the full width and rows 251 to 447, so about 195 px tall.
- Rows 439 to 447: a strip of rocky ground, about 30 px tall where it mounds up in the centre, drawn in rust and maroon diagonal hatch.

Silhouette: a broad low mound, wider than tall (about 250 x 195), roughly triangular, peaking left of centre at a tilted antenna.

Parts, by shape:
- **Front centre-left: a giant skull**, about 70 px wide and 62 px tall, three-quarter view. Rounded cranium, two large rounded-rectangle eye sockets filled with grey and dark horizontal slats (vents), a triangular nose hole, a row of square teeth that sits directly on the ground. A small emblem badge on the forehead (about 14 x 10 px, red and gold).
- **Crossbones**: long bones as fat capsules with double-knob ends, about 12 px thick, lying horizontally behind and beside the skull on both sides, one standing diagonally at the right. They double as horizontal structural beams.
- **Central block behind the skull**: stacked horizontal metal cylinders/drums with 1 to 2 px highlight bands, forming a stepped core.
- **Towers**: two pointed turret spires (tall narrow cones on short cylinders with a lit window), left and right of centre, about 15 px wide and 60 to 70 px tall; two thin pylons topped by a ball (gold ball on a red/gold striped mast).
- **Dish**: a large tilted elliptical dish, about 45 px wide, upper centre, with a long trident/fork-shaped feed antenna pointing up-left, about 40 px long. This is the highest point.
- **Right side**: boxy bunker shapes with slanted roofs, a railing, thin bent pipes/antennas (1 px lines).
- **Base**: slanted buttress walls running down to the ground left and right.

Palette, full-colour variant (8): sky `#E40058`, black, white, sky blue `#3CBCFC` (shadow on white and metal highlight), grey `#747474` (dark metal, eye sockets), gold `#F0BC3C`, rust `#C84C0C`, maroon `#7C0800`. Note the accent red on the towers reuses the sky crimson. Note also that there is no black outline needed against the black void: forms are bounded by the void itself and separated internally by grey.

Three variants, same pixels:
1. Full colour on crimson sky: the "reveal".
2. Blue monochrome: sky `#24188C`, void black, **everything in the structure collapsed to one colour** `#2038EC`. Only the silhouette and the black interior lines remain. Reads as night or as a lightning-off frame.
3. White-on-blue: identical to 2 but the black index becomes white, so the void turns white and the structure is blue on white. Used as a 1 to 2 frame **lightning flash**. Alternating 2 and 3 gives the flash; then 1 is the reveal.

Lesson: a dramatic beat is built from a **silhouette plus palette flashing**, not from rendered light.

---

## D. Character cutscene panels

### D1. Framing
- Panels are **256 px wide and 96 to 160 px tall**, i.e. wide letterbox strips, not full screens. Common sizes: 256x96, 256x112, 256x120, 256x128, 256x160. Small inset panels (a monitor view, a news screen) are about 112x72.
- Characters are **large**: a bust fills 80 to 100% of panel height. A full figure fills 85 to 95%. Heads are routinely cropped by the panel bottom or sides. Two or three characters share a panel, overlapping slightly, each facing the action.
- Background is one flat colour, sometimes with one or two extra elements: a flat cloud mass, a horizon stripe, a setting-sun disc, a far skyline in two greys.
- A note on the purple: in the low-city sheet `#800080` is clearly the ripper's transparency key (it fills the text band and sprite cells). On the panel sheet the same purple fills panel backgrounds, so it may be a key colour standing in for black or backdrop there too (inferred, not certain; one panel on the same sheet has a true black background). Either way the rule is the same: **one flat saturated or black field behind characters, no texture**. For the fan game use black, deep purple `#400098`, or a flat sky colour.

### D2. Proportions
- Super-deformed. Standing full figures are about **2.5 to 3 heads tall**. In the capsule scene the figure is about 96 px tall with a 32 to 36 px head including hair.
- In close-up busts the head is about 54 to 58 px wide, 60 to 65 px tall inside a 120 px panel.
- **Eyes**: tall vertical ovals, about 7 to 8 px wide and 12 to 14 px tall in a 56 px head (roughly one fifth of face height... in practice a quarter of the head). Construction: white oval, a black or dark-blue pupil filling the inner two thirds, a 2x3 px white glint top, 1 px black upper lid line. Eyes sit low on the face and wide apart, separated by about one eye width.
- **Mouth**: small. Open mouth is a trapezoid in maroon with a red or salmon tongue blob and a white top-teeth bar. Closed mouth is a 3 to 5 px line. Nose is a 1 to 2 px tick or absent.
- **Hands**: oversized rounded mitts. A fist is about 26 to 28 px across beside a 56 px head, i.e. half a head. Fingers are indicated by 2 to 3 short dark-hue crease lines, not separately outlined. Open hands (the gesturing scientist) have 4 fat sausage fingers splayed.
- **Hair**: solid masses with a zigzag sawtooth edge of 4 to 7 big spikes, one base colour and one shadow colour, 2 to 4 short interior strand lines. The mad-scientist type has two big side tufts and a bald dome; the kindly-scientist type has a rounded beard and moustache mass drawn as one white shape with light-blue shadow.
- **Elder characters**: big head, thick eyebrows drawn as solid shapes, moustache as a solid shape covering the mouth, white coat as a flat white shape with grey or light-blue fold lines, tie as a small coloured accent.

### D3. Expression conventions
- Emotion is carried by **eyebrow angle, mouth shape, and manga symbols**: sweat drops (white teardrops with black outline floating beside the head), surprise ticks (2 to 3 short radiating strokes), sparkles (4-point stars and crosses, white with a pink/orange ring), a small heart, lightning-bolt zigzags (white on colour, about 16x24), impact stars.
- Determined: brows as straight diagonal bars slanting down toward the nose, mouth open in a shout or set in a short flat line, fists raised to chest height.
- Shock: eyes wide with small pupils, mouth an O, hands to the cheeks, sweat drops.
- Villain gloat: one brow up, one down, wide toothy grin (a white bar with vertical black tooth lines), arms spread with open palms.

### D4. Mood by panel type
- **Comedic/slice-of-life**: bright flat backgrounds (cyan, sky blue, pink), fluffy clouds, props (bags, flowers, camera), smiling faces, symbols. High saturation, lots of white.
- **Dramatic**: black or deep purple background, characters lit normally (they never go dark), tension shown by pose and by symbols. A lone-figure sunset panel uses salmon sky `#FC7460`, crimson top band with bar transition, an orange sun disc sliced by 3 to 4 horizontal sky-coloured bars, flat pale-pink cloud streaks, and the figure standing on a small brown ground wedge at lower right, back partly turned, scarf blowing.
- **Villain reveal**: the villain is drawn in full colour, small-ish and centred, standing on a rubble pile; around and behind him a **crowd of huge silhouette figures in a 3-colour monochrome** (deep violet, lavender rim highlights, black eye slits with white pupils). The crowd is animated over 8 frames by tiny changes only. The contrast "one coloured figure against a mono crowd" is the menace device.
- **Monochrome flashback**: a whole panel in black, white and two greys (4 colours) for something remembered or reported.
- **Screen-in-screen**: a monitor frame (orange with cream highlight, or cyan) containing a mini scene, flanked by characters watching in profile.

### D5. The hero, in construction terms (draw from scratch, do not copy)
Colours (indices): 1 mid blue `#0070EC`; 2 deep blue `#0000A8` (or indigo `#24188C`) for shadow and creases; 3 cyan `#00E8D8`; 4 skin `#FCD8A8`; 5 white `#FCFCFC`; 6 black outline and pupils; optional 7 skin shadow `#FC9838`; optional 8 mouth `#A40000`/`#D82800`. So 6 core colours, 8 at most.

Bust (front, as in a 256x120 panel, head about 56 px wide):
- **Helmet**: a near-circular dome, slightly wider than tall, mid blue. A raised centre ridge runs front to back: on a front view it is a vertical cyan strip about 8 px wide from the crown down to the brow, split into two stacked rounded rectangles (upper about 8x12, lower about 10x8) by a 2 px blue gap. A blue brow band frames the face opening in a rounded "M" or wide arch shape, dipping to a point between the eyes.
- **Ear pieces**: a round disc on each side at eye level, about 10 px across on a front view (seen edge-on as a 4 px cyan bar), in profile a 12 px disc with a cyan ring and a dark or red centre.
- **Face**: skin fills the opening, roughly a rounded square 32 px wide. Two tall oval eyes as described; brows are 1 px black bars; tiny nose tick; mouth per expression. Skin shadow is a thin strip under the helmet brim only.
- **Neck/torso**: a short black-outlined neck, cyan torso (the body suit is the light colour), mid blue shoulders.
- **Arms and fists**: forearms are fat blue cylinders flaring toward the wrist, about 1.5x the upper arm width, ending in a mitt fist about half the head width. Shadow in deep blue on the lower third and as 2 to 3 knuckle creases.
- **Arm cannon**: replaces one forearm: a blue barrel the same thickness as the flared forearm, slightly longer, with a dark circular muzzle (black disc with a deep-blue ring) and 1 to 2 cyan or white indicator lights/bars on top. No extra colours.
- **Legs/boots** (full figure): cyan thighs, blue trunks, oversized blue boots flaring at the calf with rounded toes; feet as long as the head is wide.
- **Profile**: head is a circle with the face opening cut as a notch at front; one eye visible as a tall oval near the front edge; small nose bump; ear disc centred on the side of the helmet.
- **Shading**: one deep-blue crescent on the helmet's shadow side (8 to 10 px), a 2x4 white glint optional. Nothing else.

In-game scale for comparison: the fan game's standing sprite is about 21 to 26 px wide and 24 px tall in a 48x48 frame, 5 colours (black, `#0070EC`, `#00E8D8`, `#FCD8A8`, `#FCFCFC`), 1 px black outline. Its 32x32 face portrait uses 7 colours (adds `#1620A4`-like deep blue and salmon `#FB7665` skin shadow). Cutscene busts should look like that 32x32 portrait enlarged in resolution, not like a different rendering style.

---

## E. Layout and UI

### E1. Screen composition
- Native 256x224 visible (the fan game shows it at 512x448, 2x).
- Intro story screens: **picture in the top 160 rows, text in the bottom 64 rows on pure black** (measured on the low-city screens: art ends at row 159). The picture itself often does not fill 256x160: the capsule scene is a 64x128 object centred on black; character panels are 256x96 to 256x128 strips with black above and below.
- So a typical frame is: black bar (0 to 24 px), art strip, black, text. Black bars are normal and desirable.

### E2. Text box
- There is no box: no border, no background panel. **White text directly on black.**
- Font: 8x8 monospaced caps, uniform 1 px stroke with slightly widened/square letterforms, no lowercase. Punctuation used: period, comma, apostrophe, ellipsis as separate dots, double exclamation as one glyph. The repo already ships a matching font at `assets/Megaman10Font.ttf`.
- Layout (measured): left margin 32 px (column 4), up to **24 characters per line** (192 px, leaving 32 px right margin), **3 lines maximum**, line pitch 16 px (one blank tile row between lines). First line starts about 8 px below the art area, so baselines sit near rows 168, 184, 200.
- Text is left-aligned, breaks mid-sentence across screens, and is typed out character by character.
- Standalone caption screens ("year" card): a single short line of grey-to-white text centred on a fully black screen, faded in through 3 brightness steps (`#747474`, `#BCBCBC`, `#FCFCFC`).

### E3. Fades
- All fades are **palette steps, 3 to 4 frames**: black, then dark version (indigo sky, everything else black or dark blue), then mid, then full. Fade-out reverses, sometimes passing through a red-brown step.
- White flash: every index to white for 1 to 2 frames (the capsule sequence goes normal, pale, all white with only black outlines, then back).
- Time passing: cycle the same picture through the time-of-day tables.

### E4. Title screen
- Black background, 256x224.
- Logo: centred horizontally in the upper half (rows about 40 to 100), about 180 px wide, in forced perspective (letters wider at the bottom, like receding upward), filled with **horizontal stripe banding**: white, pale blue, sky blue, mid blue in 1 to 3 px bars, with a crimson/orange band at the very top of the first word and a solid blue extruded shadow below/right. A big red numeral below it, red fill with white inner outline and dark red shadow.
- Menu: two lines of white 8x8 caps, left-aligned at about x = 48, rows about 152 and 168, each preceded by an 8x8 cursor cell; the selected one shows a small arrow/icon that blinks, the other is blank.
- Lower right (about x 176 to 240, y 150 to 205): the hero's helmet as an icon, about 44 px wide, three-quarter view, 4 blues plus cyan, sitting on a gold ellipse arc (a 2 to 3 px thick orbit ring).
- Copyright line: white 8x8 caps centred near row 200 (in one regional version) or on a separate preceding screen.
- Fade-in in 3 palette steps; the dim step turns white to grey and blues to indigo.

### E5. Capsule/creation scene
- Fully black screen. A single object, centred horizontally, top at about row 16: a **capsule about 64 px wide and 128 px tall**.
- Capsule shape: a tall rectangle with a semicircular (arched) top, outlined by a 2 px blue line (`#0070EC`) with a 1 px darker inner line; interior is black; a base slab about 72 px wide and 10 px tall in orange/cream/white horizontal stripes with a small dark slot in the middle.
- Inside, behind the figure: tiny UI decorations in blue, white and red: 3 to 4 short horizontal bars (a meter) lower left, a small square icon upper right, dotted lines, a few red dots. These are 1 px details and make it read as "machine".
- Figure: front view, standing rigid, arms slightly out, about 96 px tall, 3 heads tall, skin in 2 tones (`#FCD8A8` plus `#FC9838`), big spiky blue hair, blue boots on the base.
- The scene animates by palette (dark red silhouette, orange, full colour, white flash) and by swapping the head sprite.
- Text below as in E2.

---

## F. What the first-pass stills got wrong, and the fix

The first pass (`/home/claude/story_stills`, 7 stills at 256x224, slide 1 at 256x448) measured 8 to 15 colours per still, so the palette size was fine. The style misses are structural:

| First pass | Why it does not read as NES Mega Man | Do instead |
|---|---|---|
| Sky drawn as a vertical gradient with ordered (checker/Bayer) dither bands; sun with a dithered glow halo | The references have zero dithered gradients. Sky is 2 flat colours joined by 8 rows of horizontal bars. Dither fields read as 16-bit/PC-98 or modern "pixel painting". | Flat sky colour; darker flat band in the top 48 rows; 3 to 4 thin bars as the only transition. Sun = flat disc cut by 3 horizontal bars, no halo. |
| Buildings as pale, desaturated, outline-free silhouettes in 4 to 5 shades of one blue (atmospheric perspective) | References use hue contrast, not value fog: green and cyan towers with black outlines against blue. Only the farthest layer is an unoutlined silhouette, and it uses the sky's own second colour. | 3 layers: far flat (sky-dark), mid (sky-dark plus black side plus white windows), near (2 saturated accents plus black outline and black shadow sides). |
| Thin or absent outlines; no large black masses in day scenes | Black is a main colour: 12% of the day city and the entire shadow side of every near building. | 1 px black outline on everything in the near and mid layer, thickened to 2 to 4 px on shadow sides. |
| Real perspective (converging desk, angled floor, light cones, cast window light) | References are frontal elevations with at most one oblique side plane. Depth is overlap only. | Draw rooms as a flat wall elevation plus a floor strip. No vanishing points. |
| Painterly lighting: lamp glow pools, a red alarm beam with dithered falloff, a blue energy bloom, light shafts | NES cutscenes never render light. Light is a flat shape; drama is done by palette swaps and flashes. | Alarm = whole-palette swap to red/black for alternate frames. Glow = white core with one flat ring colour. |
| Characters hidden: back-view black silhouette at the desk, tiny dark figure on the ledge, no faces anywhere | The source style is character-first: big heads, huge eyes, readable expressions, filling 80 to 100% of panel height. A story told in empty moody rooms reads as a different genre. | Put a large, fully coloured character in at least slides 2, 6 and 7, and a small one in 1. Characters keep their normal bright colours even at night. |
| Moody, low-contrast, mostly dark navy scenes (lab, ledge, alarm) with many near-black blues | NES night = pure black plus 1 to 2 saturated colours plus a bright accent. Near-black blues that differ by a few percent are invisible and non-NES. | Use `#000000` for dark, `#183C5C` or `#24188C` as the only "night" tone, and keep accents at full saturation. |
| Rain as dense diagonal streak noise over a detailed dark scene | Too many single-pixel elements; the eye loses the shapes. | Rain = sparse 1x3 or 1x4 px white/pale-blue dashes on a 16x16 repeating tile, about 1 dash per 16x16 cell, over a flat sky. |
| Blueprint still as a fine grid with many thin technical lines and dimension marks | Acceptable idea, but the 1 px grid over the whole image is busy and has no focal character. | Large simple shapes, grid at 16 px pitch in a barely lighter blue or omitted, 3 big drawings, see slide 3. |
| Fire and smoke with dithered orange-to-purple sky and soft smoke columns | Same gradient problem; smoke had soft edges. | Recolour the day city index map; smoke as flat scalloped cloud shapes in black/maroon with hard edges; flames as 3-colour teardrop shapes (white core, gold, orange/red). |
| Every still fills the full 256x224 | References leave black bars and reserve the bottom 64 rows for text. | Compose art in the top 160 rows (or a 256x112 to 128 strip), bottom 64 rows black. |

Short version: first pass was "modern indie pixel painting with a limited palette". Target is "flat black-outlined cartoon cels on flat colour fields, recoloured by palette".

---

## G. Redraw plan for the 7 intro slides

Global conventions for all slides:
- Draw at **1x native**, export 1x and 2x nearest-neighbour. Single screens are **256x224** with art confined to rows 0 to 159 and rows 160 to 223 pure black for text. Slide 1 is tall.
- Work in indexed colour. Keep a written index map per slide. Verify: max 4 colours per 16x16 cell for backgrounds (3 plus black); characters may add their own 3 to 4.
- Hero indices everywhere: H1 `#0070EC`, H2 `#0000A8`, H3 `#00E8D8`, H4 `#FCD8A8`, H5 `#FCFCFC`, K `#000000` (optional H6 `#FC9838` skin shadow in busts over 40 px).
- Scientist indices: coat/hair/beard `#FCFCFC`, coat shadow `#BCBCBC` (or `#A8E4FC` in cool scenes), skin `#FCD8A8`, skin line `#FC9838`, tie or accent `#D82800`, K black.

### Slide 1: Day city, the hero has saved the world (tall, upward pan)
- **Size**: 256 x 448 (two screens; the first pass used this size, the reference skyline is 256x400). Pan starts at the bottom screen and moves up, or the reverse; compose so both end frames work alone.
- **Palette (7 plus hero overlay)**:
  - 0 sky `#3CBCFC`
  - 1 upper sky / far buildings / cloud shadow `#0070EC`
  - 2 black `#000000`
  - 3 accent A `#4CDC48`
  - 4 accent B `#00E8D8`
  - 5 white `#FCFCFC` (clouds, windows, highlights)
  - 6 optional third accent `#F0BC3C` (use sparingly, on its own 16x16 cells: a beacon, a sign)
  - Hero sprite overlay: H1 to H5.
- **Layers**:
  1. Sky: rows 0 to 55 index 1; rows 56 to 63 bar transition (light 1, dark 2, light 1, dark 1, light 2, dark 1); below, index 0.
  2. Clouds: 4 to 5 clouds between rows 70 and 200, each 48 to 64 px wide, bump-top, 2 to 3 px index-1 underside, at least one cropped by the frame edge.
  3. Far skyline (index 1 flat, index-0 window holes): starts around row 230, tips between rows 230 and 300; 8 to 10 buildings, varied: needle spire, stepped tower, sphere-on-stalk, slab.
  4. Mid buildings (index 1 body, black side, white windows): rows 280 to 448.
  5. Near buildings (accents A and B, black outline): cropped by left and right edges, rising to about row 150 on one side for a strong vertical that the pan travels along. Give this city its own landmarks rather than the reference's: e.g. a twin-finned tower with a ring, an elevated rail line on pylons crossing the frame diagonally in flat elevation, a rooftop dish array. Keep one tube/skyway and one domed element because they say "future city".
  6. Ground line at the very bottom: a 10 px black band with a row of round trees (green circle on a 1 px black trunk, 12 px wide) is fine.
- **Character**: the hero small, standing on the top of the tallest near building around rows 140 to 170, about 26 to 32 px tall (slightly larger than the in-game sprite), three-quarter back or side view, one fist on hip or arm raised. Optional: 2 to 3 tiny celebratory elements (a flag, confetti squares in white/gold) to state "saved" without text.
- **Top screen alone** must be nearly empty: sky, bars, 2 clouds, the hero on a spire tip, and antennas. That emptiness is on-style.

### Slide 2: The scientist's workshop at night
- **Size**: 256x224; art is a 256x144 strip at rows 8 to 151.
- **Palette (10)**:
  - 0 black `#000000`
  - 1 wall `#183C5C`
  - 2 machinery mid `#008088`
  - 3 machinery light `#00E8D8`
  - 4 screen glow `#58F898`
  - 5 window night sky `#24188C`
  - 6 white `#FCFCFC`
  - 7 skin `#FCD8A8`
  - 8 warm accent `#FC9838` (lamp, skin line)
  - 9 grey `#BCBCBC` (coat shadow)
- **Composition**: flat elevation of a lab wall, no perspective. Left third: a console block with a monitor (mint frame, black screen with 3 to 4 mint waveform bars), a keyboard shelf as a parallelogram. Centre: workbench as a horizontal slab with a 2 px white top highlight and black underside. Right third: a wall window (32x40) showing indigo sky, a white crescent moon (10 px) and 4 single-pixel stars, plus two vertical ribbed pipes. Along the top: a horizontal girder with rivets every 16 px and two louvered vents (horizontal 1 px bars). Floor: a pale tiled strip 12 px tall with vertical joints every 32 px.
- **Character**: the scientist **asleep at the bench, facing the viewer in three-quarter view, large**: head about 44 px wide, resting on folded arms on the bench top. Bald dome, big white beard and moustache as one shape, eyes closed as two downward arcs (5 px wide), white coat sleeves as rounded masses. Above his head: white "Z" letters in 3 sizes (5, 7, 9 px) drifting up right, or a round dream bubble (white outline circle, 40 px, with 2 small lead-in circles) containing a tiny blue robot silhouette. A desk lamp as a simple cone with a cream `#FC9838` flat triangle of light is optional; no glow.
- He is fully lit and coloured; the night is conveyed by the navy wall and black, not by darkening him.

### Slide 3: Blueprints / new designs montage
- **Size**: 256x224; art rows 8 to 151.
- **Palette (6)**:
  - 0 blueprint field `#0000A8` (or `#24188C`)
  - 1 grid line `#0070EC`
  - 2 drawing line `#FCFCFC`
  - 3 highlight line `#00E8D8`
  - 4 callout `#F0BC3C`
  - 5 black `#000000` (frame and outside)
- **Composition**: three sheets, each a rectangle with a 2 px white border, slightly overlapping and each offset by a few px vertically (flat, not rotated; rotation aliases badly at this size). Grid at 16 px pitch only, 1 px, index 1.
  - Left sheet (72x112): an arm cannon in side elevation, 56 px long: flared barrel, round muzzle, 3 indicator bars. 2 px white line work, cyan for the energy chamber.
  - Centre sheet (96x128): the hero front view as a line drawing, 96 px tall, 3 heads tall, built from the helmet circle, torso trapezoid, flared forearms and boots. White 1 px lines with the helmet ridge and ear discs in cyan. A dashed centreline.
  - Right sheet (72x112): a boot/thruster in side view and a helmet in profile, stacked.
  - Callouts: 3 to 4 gold leader lines ending in a small gold square, plus short rows of 2x1 px white dashes standing in for annotation text. No readable words.
- **Animation hook**: can be delivered as layers (bg, each sheet) so sheets pop in one by one, as the first pass did; the first pass's layer split under `story_stills/layers` is a reasonable structure to keep.
- **Optional character**: the scientist's hand (big mitt, skin plus white cuff) holding a pencil entering from the lower right corner, 40 px wide. This adds the missing "character-first" feel.

### Slide 4: "But then..." alarm flash beat
- **Size**: 256x224. Two frames (A/B) designed to alternate every 4 to 8 game frames, plus the still.
- **Palette (5)**:
  - 0 black `#000000`
  - 1 alarm red `#D82800`
  - 2 dark red `#7C0800`
  - 3 hot `#FC9838`
  - 4 white `#FCFCFC`
- **Composition**: mostly black, as in the caption screens. Centred in the art area: a wall-mounted **rotating beacon**, about 40 px wide: a black-outlined dome (red, with a white 3x6 glint) on a grey-free dark-red base plate with two rivets. From it, **four flat triangular light wedges** (two left, two right), each a solid red triangle with a 2 px orange inner edge, about 90 px long, hard edged, no dither. Frame B: wedges absent, dome dark red, entire screen otherwise black. Optional frame C: full-screen flat `#D82800` for 1 frame.
- Lower-left of the art area: the **lab from slide 2 recoloured** as a 3-colour silhouette (black, dark red, red) so it is clear where we are; or simply the scientist's startled face in a small 64x64 inset at lower right: eyes wide open (white with small pupils), brows up, 3 surprise ticks, drawn with the red palette as a monochrome (white, red, dark red, black).
- This slide is the palette-flash lesson from the fortress: get the drama from alternating two flat palettes.

### Slide 5: The same city burning under a fortress shadow
- **Size**: 256x224 (use the lower/middle 256x160 window of slide 1's art, e.g. rows 250 to 409, so recognisable landmarks are in frame; if a pan is wanted, reuse the full 256x448).
- **Base**: slide 1's index map, pixel-identical, new colour table:
  - 0 sky `#FC9838` (or `#FC7460` for a redder look)
  - 1 upper sky / far buildings / cloud shadow `#D82800`
  - 2 black `#000000`
  - 3 accent A becomes `#7C0800`
  - 4 accent B becomes `#7C0800` (collapse: buildings lose local colour) or `#000000` for a harsher silhouette
  - 5 white becomes `#FCE4A0` (windows read as fire-lit)
  - 6 gold stays `#F0BC3C`
- **Overlays (new pixels, sprite-like)**:
  1. **Fortress silhouette** in the upper sky, filling the top 70 to 90 rows, centred or upper right: a flat **black** shape with a 1 px `#7C0800` inner rim on its underside, plus two or three small `#E40058` or `#F0BC3C` lit windows. Design a silhouette for this game's villain rather than reusing the skull: a broad low mound or floating mass about 180 px wide with 2 pointed turrets, one tilted dish with a forked antenna, and horizontal beam shapes. It must read from silhouette alone.
  2. Its **shadow**: recolour index 0 to index 1 in a hard-edged band across the sky below the fortress, or simply make the top 48 rows black with a scalloped lower edge (4 rows of arcs, as in section C).
  3. **Smoke**: 3 to 4 columns rising from the mid skyline, each a stack of overlapping flat circles (10 to 22 px diameter) in black with a 1 to 2 px `#7C0800` highlight on one side. Hard edges.
  4. **Fires**: 6 to 8 flames on rooftops, 8 to 16 px tall, teardrop with 2 to 3 tongues: `#FCE4A0` core, `#F0BC3C` body, `#D82800` outer; no outline (they are light sources).
  5. Clouds: delete or recolour to `#FC7460` streaks.
- **Total colours**: 8 to 9.
- **Character**: none needed; optionally 2 to 3 tiny black flying robot silhouettes (6 to 8 px) near the fortress.

### Slide 6: The hero turns to face the villain (determination close-up)
- **Size**: 256x224; art is a 256x120 strip at rows 20 to 139 (black bars above and below).
- **Palette (9)**:
  - 0 background `#400098` flat (or black; pick purple for a "dramatic panel" feel)
  - 1 H1 `#0070EC`
  - 2 H2 `#0000A8`
  - 3 H3 `#00E8D8`
  - 4 H4 `#FCD8A8`
  - 5 H5 `#FCFCFC`
  - 6 K `#000000`
  - 7 `#FC9838` skin shadow
  - 8 `#D82800` (mouth interior, reflected fire glint)
- **Composition**: hero bust, three-quarter view facing right, placed left of centre, head about 60 px wide and 64 px tall so it nearly fills the strip height; helmet top cropped by 2 to 4 px at the strip's upper edge. Right fist raised at chest height in the lower centre (about 28 px wide). The arm cannon forearm crosses the lower right, muzzle pointing right, with a small white charge spark (4-point star, 9 px, plus 3 single white pixels).
- Expression: brows as diagonal bars angled down to the nose, eyes wide ovals with large pupils looking right, mouth a short flat line or slightly open with gritted white teeth bar.
- Right third: **the villain's fortress silhouette again, tiny and far**, flat `#24188C` on the purple (2 tone) or a black silhouette, 60 px wide, so the eyeline has a target. Alternative: 3 white speed lines (1 px, 40 to 80 px long) behind the head to sell the turn.
- A two-frame version (head in profile looking left, then three-quarter right) would sell "turns"; if only one still, use the three-quarter view.

### Slide 7: Hero alone on a rooftop ledge in rain; the world sees him as a threat
- **Size**: 256x224; art rows 0 to 159.
- **Palette (9)**:
  - 0 sky `#183C5C`
  - 1 black `#000000`
  - 2 rain / window lights `#A8E4FC`
  - 3 H1 `#0070EC`
  - 4 H2 `#0000A8`
  - 5 H3 `#00E8D8`
  - 6 H4 `#FCD8A8`
  - 7 H5 `#FCFCFC`
  - 8 warning red `#D82800`
- **Composition** (night-city logic: everything except sky and lights is black):
  - Sky: flat index 0, top 24 rows black with the 8-row bar transition.
  - City: the slide 1 skyline's far/mid silhouette reused in **black**, low in the frame (rows 100 to 159), with pale `#A8E4FC` windows; this ties back to slides 1 and 5 via the same landmark shapes.
  - Ledge: a black rooftop slab entering from the left, top edge at row 112, extending to x = 150, with a 1 px `#A8E4FC` highlight line along its top edge (wet rim) and a few 2 to 4 px dashes below it (reflections, the "water dash" texture from section A5).
  - **Hero**: sitting or standing at the ledge end, side or three-quarter back view, about **56 to 64 px tall** (a mid shot, not a speck), fully coloured in his normal palette, head bowed: helmet tilted 15 to 20 degrees forward, eye shown as a half-closed oval (lower half only) or hidden under the brim, mouth a small flat line. Arms hanging, cannon lowered.
  - **"Threat" device**: two or three big **screens/billboards** on the black buildings at right, each 40x32, red frame with black interior, showing a simple white-and-red icon of the hero's helmet silhouette inside a triangle or under a diagonal bar (a "wanted/danger" sign), with a row of dashes as fake text. Alternatively a searchlight: a flat pale `#A8E4FC` hard-edged wedge from the lower right crossing the sky to the hero. Use one device, not both.
  - Rain: 1x3 px dashes in index 2, slanted by placing them on a diagonal lattice, about one dash per 16x16 cell, in the sky area only; let 4 to 6 dashes cross the hero and ledge at most. Optional 3 px wide "splash" ticks on the ledge top.
- Keep the mood through composition (small figure, large black masses, bowed head), not through darkening or blur.

---

## H. Checklist for the artist-agent (most important first)

1. **DO** use only the NES hexes in section A2, and keep each still to 6 to 12 colours (16 absolute max) with at most 4 per 16x16 background cell.
2. **DON'T** dither gradients, anywhere. Skies are 2 flat colours joined by 3 to 4 thin horizontal bars over 8 rows.
3. **DO** outline every near and mid object and every character in 1 px pure black, and use solid black for shadow sides and ground masses (10 to 40% of the image).
4. **DO** build the city once as an indexed image and produce day, burning and night-silhouette versions by palette swap only; add fire, smoke and fortress as separate overlays.
5. **DO** put large, fully coloured, expressive characters in the frame (bust at 80 to 100% of strip height; heads 2.5 to 3 per body; tall oval eyes about a quarter of head height; mitt fists half a head wide).
6. **DON'T** render light: no glows, halos, light cones with falloff, bloom or cast shadows. Light is a flat hard-edged shape; drama comes from palette flashes and silhouettes.
7. **DON'T** use real perspective. Frontal elevations, at most one oblique side plane, depth by overlap and by three flat layers (far flat, mid plus black, near accent plus black).
8. **DO** shade with exactly one hard-edged shadow shape per form in a hue-shifted darker colour, plus at most one small white highlight; ramps are 2 steps, 3 at most.
9. **DO** leave it empty: more than half of any background should be one flat colour; compose art in the top 160 rows or a 256x96 to 128 strip, with black bars and the bottom 64 rows black for text.
10. **DO** keep the hero to 6 core colours (`#0070EC`, `#0000A8`, `#00E8D8`, `#FCD8A8`, `#FCFCFC`, black) in every slide, including night scenes; never dim or tint a character to fit the lighting.
11. **DON'T** scatter single pixels: minimum cluster 2x2 except windows, rivets, stars, eye glints, rain dashes and foliage speckle. Speckle texture is for bushes, rock and water only.
12. **DON'T** copy or trace any reference sprite; build every shape from the construction notes (circle helmet, ridge strip, ear discs, flared forearms, stepped towers, scallop clouds) and give the city and fortress their own landmarks.

---

## Appendix: measurements used

- Reference sheets are at 1x; one screen is 256 px wide.
- Tall skyline: 256x400; colour pixel counts (day): 55534 sky, 21836 upper/far, 12118 black, 5217 green, 3869 white, 3826 cyan. All seven variants share these counts exactly (palette swaps of one index map).
- Sky bar transition: rows 48 to 55 in the tall skyline, rows 24 to 31 in the low city; identical 1-2-1-1-2-1 pattern.
- Clouds: about 58 px wide, first cloud spans x 18 to 76.
- Low city: art rows 0 to 159, text band rows 160 to 223; mountain is solid black from about row 102.
- Fortress: 256x448; flat sky rows 0 to 218; scallop edge rows 219 to 234; structure bbox rows 251 to 447; ground strip from row 439 at the edges.
- Capsule: about 64x128 on black; figure about 96 px tall.
- Text: 8x8 caps, left margin 32 px, 24 characters max per line, 3 lines, 16 px pitch.
- Fan game: native 256x224 at 16 px tiles (Tiled maps use 32 px tiles at the 2x presentation scale; tileset art such as `InfernoManTileset.png` is authored on a 16 px grid at 288x160). Player frames are 48x48 with a figure about 21 to 26 px wide and 24 px tall (29 px in a jump), 5 colours: black, `#0070EC`, `#00E8D8`, `#FCD8A8`, `#FCFCFC`. Face portraits are 32x32 with 6 to 7 colours and heavy black (about 35% of pixels). Tilesets use 7 saturated colours with black outlines and horizontal highlight bars. The game's art already follows the NES conventions above, so the stills should match it.
- First-pass stills: 256x224 (slide 1 256x448); 15, 14, 11, 8, 13, 10, 11 colours respectively.
