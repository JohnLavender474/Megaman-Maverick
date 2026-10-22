# Notes: 035 Aspect-ratio policy for phones

Item numbers in the review text are the numbers of the review list from 2026-10-22. The new ID is the review number plus 1, except review item 45, which is part of `026`. Line numbers refer to `master` at `9f3917b6a`. Check them again before the work starts.

## Review: Rendering, backgrounds, assets and platforms (2026-10-22)

Copied as written from the review section "ITEM 34: Aspect-ratio policy for phones".

````text
1. Current state

- The game view is a fixed 16×14 tiles (`Constants.kt:14-15`, about 8:7), drawn with `FitViewport` or `PixelPerfectFitViewport` (`MegamanMaverickGame.kt:287-299`). The bars are black.
- An on-screen controller already exists: `ScreenController` (scene2d, `ExtendViewport`), enabled by `showScreenController` (`MegamanMaverickGame.kt:129, 210`).
- Rooms: 143 of 259 `game_rooms` in the 18 live maps are 16 tiles wide or less. 18 of them are vertical shafts.
- `RotatableCamera.coerceIntoBounds` (`RotatableCamera.kt:77-98`) clamps to the max side, then to the min side. If the view is wider than the room, the camera pins to the left edge and shows space outside the room on the right. In rotated gravity it swaps width and height, so a wider view becomes a taller view.
- Spawning and checkpoint activation depend on camera overlap (items 18 and 35). A wider view changes when enemies spawn, so it changes gameplay, which breaks the contract.
- A 19.5:9 phone at 14 tiles high shows about 30 tiles across, close to twice the design width.

2. Options

(a) Keep the 16×14 view and pillarbox. Put the touch D-pad and buttons in the side bars. With a gamepad, show border art or the HUD in the bars.
   Pros: no level changes. Gameplay is the same on all platforms. The thumbs do not cover the game. Emulators and many retro re-releases on phones use this layout.
   Cons: the game image is smaller (on a 2400×1080 phone the game uses about 1234×1080, and an integer 2× scale gives 1024×896). The bars are "empty" with a gamepad.
(b) Wider view, clamped to room bounds. The camera stays inside the room, and the part outside the room is masked (black) when the room is narrower than the view.
   Pros: wide rooms show more on phones.
   Cons: no gain in more than half of the rooms (16 wide). Spawn and culling timing changes because they are tied to the camera. Room transitions and boss rooms are designed for 16 wide. It needs a new spawn/cull rule that does not use the camera (item 18). Every level needs a play test again.
(c) Separate mobile level variants.
   Pros: the best fit for each device.
   Cons: two sets of 18 levels to maintain. Every fix is done twice. Validator, tests and assets are doubled. Too much cost for a fan project, and the mobile set will drift behind.

3. Recommendation

Use (a). Do not plan a separate mobile release.
- Decide now: "The gameplay view is 16×14 on all platforms" is part of the contract. Item 30 (input intents) must treat touch as one more input source that maps to the same intents. Keep the view size only in `ConstVals`, and keep spawn and checkpoint rules off the raw camera rectangle (items 18 and 35), so that option (b) stays possible later.
- Decide later: border art and HUD placement in the bars, and whether to add (b) as an option for wide rooms only, after a real Android build exists.
- Android with a gamepad: hide the touch controls when a controller connects (a `Controllers` listener) and show them again when it disconnects.

4. Open questions for the owner

- Is integer scaling (sharp pixels, smaller image) more important than filling the height?
- Do you want the HUD (health bars, weapon energy) moved into the side bars on phones, or kept in the game view as today?
- Is a tablet (4:3, close to 8:7) a target? Then (a) gives almost no bars there.

5. Proposed backlog wording

Title: Aspect-ratio policy: fixed 16×14 view with side-bar controls
- The gameplay view stays 16×14 tiles on all platforms. Pillarbox on wide screens.
- Touch controls (the existing `ScreenController`) go in the side bars. Hide them when a gamepad connects.
- Optional border art and HUD in the bars.
- Keep the view size in one place, and keep spawn and checkpoint rules independent of the camera size (so a wider view stays possible later).

6. Dependencies

30 (touch as an input source), 33 (persisted settings for controls and scaling), 31 (Android target), 18 and 35 (camera-independent spawn and activation, if (b) ever comes).
````
