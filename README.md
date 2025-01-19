# MEGAMAN MAVERICK

---

Megaman Maverick is a non-commercial fan game based on the [classic Megaman series by Capcom](https://megaman.fandom.com/wiki/Mega_Man_(original_series)). 
All rights to "Megaman" are owned by Capcom, and this project aligns with their lenient stance on fan works.

Currently in pre-alpha, the game features assets from official Capcom titles and the community (see [Credits](#Credits)). 
Special thanks to these creators for helping make this dream project a reality!

- Framework: [LibGDX](https://libgdx.com/)
- Language: [Kotlin](https://kotlinlang.org/)

Check out my [YouTube channel](https://youtube.com/playlist?list=PL4ZszXL-HC0r0E6Eb5NCFGhsi93AXoDZj&si=IITydzhhTSKmxc5-) for demos!

Join the [Discord server](https://discord.gg/Cab2XMKs) for updates!

<img src="img/MainScreen.png" width="600px"/>
<img src="img/BossSelectScreen.png" width="600px"/>

---

## RUNNING THE GAME

Currently, there is no "official" release of this game since it is still under active development. However, see the
section below on how to run the game using the source code.

### BUILDING THE GAME FROM SOURCE

Since the game runs using gradle and JRE, you can build a JAR of the game yourself. Keep in mind that in order to run
the generated JAR, you will need Java 17+ installed on your local machine.

#### Pre-requisites:

- Git
- Java 17+

##### Notes:
- Download Git using the following link: https://git-scm.com/downloads
- Download JDK 17+ (skip this step if running IntelliJ): https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
  - Set JDK Home on Windows: https://confluence.atlassian.com/doc/setting-the-java_home-variable-in-windows-8895.html
  - Set JDK Home on Mac: https://stackoverflow.com/questions/22842743/how-to-set-java-home-environment-variable-on-mac-os-x-10-9
  - Set JDK Home on Linux: https://access.redhat.com/solutions/1445833
  - If running Mac/Linux, run the following command to see if Java is properly installed and configured
    ```bash
    which java
    ```

1. Use `git` to clone the project to a suitable location on your desktop using the following command
   ```bash
   git clone https://github.com/JohnLavender474/Megaman-Maverick.git
   ```
2. From the root directory of the game project, run one of the following:
   1. Mac/Linux (edit the shell script to change settings):
      ```bash
      build-run-desktop-debug.sh
      ```
   2. Windows (will have to pass program args if you want to change any settings):
      ```bash
      ./gradlew lwjgl:run
      ```
- See the [GDX Liftoff](#GDX-Liftoff) section for more gradle commands and details.

---

## PLAYING THE GAME

### CONTROLS

By default, the keyboard controls for the game are the following whether the 1st value is the keyboard key and the 2nd
value is the "in-game" action:
- A: LEFT
- D: RIGHT
- S: DOWN
- W: UP
- J: B
- K: A
- ENTER: START

These can be remapped through the keyboard settings screen accessed from the main menu. Use the keys for "UP" and "DOWN"
(by default the "W" and "S" keys respectively) to navigate to the game button you wish to remap and press the key for
"START" (by default the "ENTER" key). You will be prompted to press any key on the keyboard to assign that key's code
to the in-game button action.

<img src="img/KeyboardSettings.png" width="600px"/>
<img src="img/PressToReassignKey.png" width="600px"/>

The game supports controllers and should detect a controller as soon as it is plugged in. The keyboard controls continue
to work even when a controller is connected. The game has been tested with the following controllers:
- PS3 controller
- Xbox 360 controller
- NES-style USB controller

When a controller is connected, default mappings are assigned. Most likely, you will want to configure these mappings.
In the "Controller Settings" screen, you can reassign the mappings for the controller similarly to how the same is done
for the keyboard mappings.

---

## THE STORY

The story is still in progress and nothing has been finalized yet.

---

## CREDITS

The artwork, sounds, and music used in this game are sourced from Capcom and fan works from the community. Below is a
comprehensive listing of the credits for the artwork used in this game.

### Pixel Art

#### Megaman

- Megaman Redesigned - [SamuelX4](https://www.deviantart.com/samuelx4) 

#### Title Screen

- Megaman Title - [Mister Mike](https://www.spriters-resource.com/fullview/258/)
- Megaman Weapon Get Sprite - [PixelBoy127](https://www.spriters-resource.com/custom_edited/megamancustoms/sheet/166239/)

#### Megaman
- Megaman complete spritesheet - [SamuelX4](https://www.deviantart.com/oldlavygenes474/art/1137567721)

#### Robot Masters

- Timber Woman spritesheet- [oldlavygenes](https://www.deviantart.com/oldlavygenes474/art/TimberWoman-Megaman-Maverick-1087794591)
- 
- Rodent Man 
  - Original by [oldlavygenes](https://www.deviantart.com/oldlavygenes474/art/Rodent-Man-Megaman-Maverick-1087797101)
  - Updated mugshot by [SamuelX4](https://www.deviantart.com/oldlavygenes474/art/1137571452)
- Precious Man - [oldlavygenes](https://www.deviantart.com/oldlavygenes474/art/Precious-Man-Megaman-Maverick-1087800596)
- Timber Woman:
  - Original by [oldlavygenes](https://www.deviantart.com/oldlavygenes474/art/TimberWoman-Megaman-Maverick-1087794591)
  - Updated mugshot by [SamuelX4](https://www.deviantart.com/oldlavygenes474/art/Timber-Woman-Mugshot-1137568070)
  - Updated spritesheet by [SamuelX4](https://www.deviantart.com/oldlavygenes474/art/1141743410)
- Moon Man spritesheet:
  - Original by [boberatu](https://www.deviantart.com/boberatu/art/MPN-006-Moon-Man-625679636)
  - Updated by [SamuelX4](https://www.deviantart.com/oldlavygenes474/art/1136876962)
- Inferno Man 
  - Original by [boberatu](https://www.deviantart.com/boberatu/art/MPN-000-Volcano-Man-313694441)
  - Updated mugshot by [SamuelX4](https://www.deviantart.com/oldlavygenes474/art/1137570993)
- Reactor Man spritesheet & mugshot - [Balloon Cart](https://balloon-cart.itch.io/reactor-man-asset-pack)
- Glacier Man spritesheet & mugshot - [Balloon Cart](https://balloon-cart.itch.io/glacier-man-asset-pack)
- Desert Man spritesheet & mugshot - [rcrdcat](https://www.deviantart.com/rcrdcat/art/Desert-Man-Spritesheet-Mugshot-332165249)

#### Other Bosses
- Moon Head spritesheet - [oldlavygenes](https://www.deviantart.com/oldlavygenes474/art/MoonHead-Miniboss-Megaman-Maverick-1090151707)
- Nuclear Monkey spritesheet - [Balloon Cart](https://balloon-cart.itch.io/reactor-man-asset-pack)
- Bospider spritesheet - [Bean and Shawn](https://www.spriters-resource.com/game_boy_gbc/mmxtreme/sheet/480/?source=genre)
- Mecha Dragon spritesheet - [Mister Mike](https://www.spriters-resource.com/nes/mm2/sheet/2317/)
- Guts Tank spritesheet - [Mister Mike](https://www.spriters-resource.com/nes/mm2/sheet/2317/)
- Penpen Maker spritesheet - [Mister Mike](https://www.spriters-resource.com/nes/mm3/sheet/77911/) (currently not used)
- Sphinx spritesheet - [Mister Mike](https://www.spriters-resource.com/custom_edited/megamancustoms/sheet/108177/) (currently not used)

#### Enemies
- Sprites by [MegaRed225](https://www.deviantart.com/megared225/gallery):
  - [Random Enemies Set 5](https://www.deviantart.com/megared225/art/Random-Enemies-Set-5-517418497):
    - Lumber Joe 
    - Big Fish Neo
    - Cannon Hopper
  - [Random Enemies Set 9](https://www.deviantart.com/megared225/art/Random-Enemy-Sprites-Set-9-557171615):
    - Fire Dispensenator
  - [Random Enemies Set 10](https://www.deviantart.com/megared225/art/Random-Enemy-Sprites-Set-10-571128501):
    - Nutt Glider
- All other enemy sprites sourced from https://www.spriters-resource.com/

#### Tilesets

- [Balloon Cart (Reactor Man)](https://balloon-cart.itch.io/reactor-man-asset-pack)
- [Balloon Cart (Glacier Man)](https://balloon-cart.itch.io/glacier-man-asset-pack)
- [Ansimuz (Rodent Man)](https://ansimuz.itch.io/mega-bot)
- [Ansimuz (Inferno Man) (Commission)](https://www.deviantart.com/oldlavygenes474/art/1142906425)
- [Mega 8-Bit](https://assetstore.unity.com/packages/2d/environments/mega-8-bit-pixel-pack-60158?srsltid=AfmBOordeWICo0KR-N3MKcw6iqd2TehrlFgQn6Hijzmk09-2eoq2Gid0)
- Mister Mike:
  - [Megaman 6 - Tomahawk Man Tileset](https://www.spriters-resource.com/nes/mm6/sheet/16402/)
  - [Megaman 3 - Wily Stage 1 Tileset](https://www.spriters-resource.com/nes/mm3/sheet/16388/)
- AxeW1eld3r:
  - [Megaman 9 - Endless Attack Tileset](https://www.spriters-resource.com/wii/mm9/sheet/166545/)
- Rabbid4240:
  - [Megaman 9 - Magma Man Tileset](https://www.spriters-resource.com/wii/mm9/sheet/197901/)
- Bongwater-bandit:
  - [MM6 Plant Man edit](https://www.deviantart.com/bongwater-bandit/art/MM6-Plant-Man-edit-875037908)
- Megaman Maker:
  - [Crystal Cave Background](https://wiki.megamanmaker.com/index.php/File:SprBGCrystal5_0.png)

### Music

- MM6 Capcom Logo - https://downloads.khinsider.com/game-soundtracks/album/megaman-6-original-soundtrack/01%2520Capcom%2520Logo.mp3
- Infero Man stage track - [vinnyz279](https://soundcloud.com/mega-vinnyz279)
- MMX3 Intro Stage - [Famitard](https://youtu.be/jEPimSadiRE)
- MMX6 Blaze Heatnix - [Famitard](https://youtu.be/QpbMwCnJDSo) - used in Inferno Man stage
- MMX5 Dark Necrobat - [Famitard](https://youtu.be/RosxPCxVOyk) - used in Moon Man stage
- MMX7 Vanish. Gung. - [Famitard](https://youtu.be/MFfZ-LEwcMo) - used in Desert Man stage
- MMX1 Chill Penguin - [Famitard](https://youtu.be/QWmsNV18S1g) - used in Glacier Man stage
- MMX8 Burn Rooster - [Famitard](https://youtu.be/DHh-QSWvb-o) - used in Reactor Man stage
- MMX6 Blaze Heatnix - [Famitard](https://youtu.be/QpbMwCnJDSo) - used in Inferno Man stage
- MM7 Slash Man - [Famitard](https://youtu.be/24ECwWnh6Es) - used in Rodent Man stage
- MMX3 Neon Tiger - [Famitard](https://youtu.be/cXiyGW0ulIY) - used in Timber Woman stage
- MMX2 Crystal Snail - [Famitard](https://youtu.be/1qrbYy9qQdE) - used in Precious Man stage
- MMX6 Boss - [Famitard](https://youtu.be/IeySHEF5U_8) - used in Robot Master boss fights
- MMX6 Gate's Laboratory- [Famitard](https://youtu.be/Zdtp6f57E_c) - used in Wily stage 3
- MMX6 Sigma Final Boss - [Famitard](https://youtu.be/FVdYxfEo4lI) - used in Wily stage 3 boss fight
- MMX Vile Fight - [Famitard](https://youtu.be/iIcvnpYVTKc) - used in Wily stages 1 & 2 boss fights
- TODO: Other music tracks are saved in the `assets` dir but not yet used in the game. These will be added here as they're added to the game.

### Sounds

- Megaman 5 Sounds: https://www.sounds-resource.com/nes/megaman5/sound/3618/
- Megaman 6 Sounds: https://www.sounds-resource.com/nes/megaman6/sound/3619/
- Megaman 9 Sounds: https://www.sounds-resource.com/xbox_360/megaman9/sound/33185/

Sounds belong to Capcom (Megaman-ripped sound effects) and Nintendo (Mario-ripped sound effects)

TODO: add credits and links here for where the sounds were downloaded from

### Proof of Permissions for Fan Community Assets

For some of the fan artwork used in this game, the artist did not state in the work's description or elsewhere that the work was
free to use. For these works, I reached out to the artist personally to ask if I could use their work. Below are screenshots
as proof of permission. No proof is required for works where the description states that the asset is free to use.

Works by boberatu:

<img src="./img/permissions/boberatu.png" width="300">

Works by rcrdcat:

<img src="./img/permissions/rcrdcat.png" width="300">

Works by Famitard:

<img src="./img/permissions/famitard.png" width="300">

---

## GDX Liftoff

This project was generated with a template including simple application launchers and an `ApplicationAdapter` extension that draws libGDX logo.

### Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.

### Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should 
be replaced with the ID of a specific project. For example, `core:clean` removes `build` folder only from the `core` 
project.
