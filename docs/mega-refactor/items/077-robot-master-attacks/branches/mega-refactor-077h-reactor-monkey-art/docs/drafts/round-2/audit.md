# Reactor Monkey round 1: audit against the original frames

Compared at 4x: `audit-side-by-side.png` (examples) and `before-after.png` (every frame, round 1 above round 2).

## What the original has (`stand`, `throw`)

- **Head:** a small dome (highlight side upper left, 1 dark pixel as a rivet, dark rim on the right); a black visor band
  that runs from the eyes back to the near shoulder pad; 2 white goggle rings with dark pupils; a thin lip strip with
  2 white tusk pixels at its corners; a brown mouth line; a large round jaw under it, with a brown left border and a
  dark right rim. Head and jaw are the same pixels in `stand`, `throw` 1 and `throw` 3.
- **Shoulder pads:** 2 discs of about 31 px, as large as the head and jaw together. Each has a light face on the upper
  left and 2 brown rings with a dark band between them on the lower right. They sit at head height: the pad top is 1 to
  4 px under the dome top, and the head sits between them. `stand`: the near pad overlaps the head on the right, the far
  pad shows as a crescent on the left. `throw` 1 (upright): pad centers 13 px left and 28 px right of the head center,
  both partly behind the raised arms. The upper arm comes out from under the pad.
- **Torso:** upright it is a wide plate that narrows to the waist, light in the middle, dark bands at the sides, a
  brown and black pec line near the bottom. On all fours it is a ribbed back (brown cross lines) behind the near pad.
- **Waist:** a brown belt with studs, a brown loincloth block between 2 round hip balls.
- **Arms and legs:** short upper arm, long forearm that narrows to the wrist (1 dark px left, 2 to 3 right), a dark
  knuckle block with brown toes and a sole line; thin shins, large dome feet with a brown sole.
- **Tail:** an S curve with a 1 px black outline.

## Round 1, per strip

| Strip | Missing or wrong |
|---|---|
| `intro_drop` | New drawing from balls and capsules. Pads are small plain balls with no rings, set wide of the torso. No jaw border, no tusks, no pec line, plain limbs, tiny fists. |
| `intro_land` 1, 2 | Same ball style: pads have no rings, the ribbed back and the belt are gone, the forearms are plain capsules. Cell 3 is fine (real `stand` pixels). |
| `intro_roar` 1 to 5 | Pads are plain balls that float beside the neck and are smaller than in `stand`; the torso is one ellipse with no plate shading; the arms hang from the pad edge, not from under the pad. Cell 6 is fine. |
| `dying` | Real `stand` pixels, details complete. Only the far fist jump in cell 1 looked cut off. |
| `collapse` 2 to 6, `dead` | Ball style again: no pad rings, no ribbed back, no belt, no loincloth, visor band shortened, tusks gone. |
| `chest_beat` 2 to 8 | Body from `throw` 1, so torso and waist are right. The red head is an oval that covers the jaw border, the tusks and the mouth line. The far pad is a bare brown disc with the arm ball in front of it and the other pad is missing, so the shoulders read as 2 small balls: too small, and the left one sits on the chest. |
| `mad_stand`, `mad_throw` | Body complete. The red head is repainted as one oval: jaw border, tusks, dome rim and rivet are gone, and the head is wider than the original. |
| `steam`, `steam_jet`, `ball_mad` | No defect found; copied unchanged. |
