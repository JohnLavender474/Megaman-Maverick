This README is not finished; therefore sections may contain inaccurate or outdated info.

# MEGAMAN MAVERICK

<img src="img/MainScreen.png" width="600px"/>
<hr/>
<img src="img/BossSelectScreen.png" width="600px"/>
<hr/>

**Megaman Maverick** is a fangame based on the [classic Megaman series by Capcom](https://megaman.fandom.com/wiki/Mega_Man_(original_series)).

The game is currently under active development and is in a **pre-alpha** stage.

The game is built using the [Mega 2D Game Engine](https://github.com/JohnLavender474/Mega-2D-Game-Engine), an engine
built on top of the popular open-source [LibGDX game library](https://libgdx.com/). This game is programmed primarily in
Kotlin, with few parts written in Java.

View my Youtube channel for demos and more! https://youtube.com/playlist?list=PL4ZszXL-HC0r0E6Eb5NCFGhsi93AXoDZj&si=IITydzhhTSKmxc5-

## RUNNING THE GAME

Currently, there is no "official" release of this game since it is still under active development. However, see the 
section below on how to run the game using the source code.

### BUILDING THE GAME FROM SOURCE

Since the game runs using gradle and JRE, you can build a JAR of the game yourself. Keep in mind that in order to run
the generated JAR, you will need Java 17+ installed on your local machine.

#### Pre-requisites:

- git
- Java 17+

1. Use `git` to clone the project to a suitable location on your desktop.
2. From the root directory, run one of the following:

- Run using gradle:
    - Run the following command to start the desktop application: `./gradlew lwjgl:run`. 
    - Optionally, you can run the `build-run-desktop-alpha.sh` or `build-run-desktop-debug.sh` script instead. The former is 
      a shortcut for the above command. Meanwhile, the latter script includes configurable args for debugging purposes.
    - See the [GDX Liftoff](#GDX-Liftoff) section for more gradle commands.

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

These can be remapped through the Keyboard Settings screen. Use the keys for "UP" and "DOWN" (by default the "W" and "S"
keys respectively) to navigate to the game button you wish to remap and press the key for "START" (by default the "ENTER"
key). You will be prompted to press any key on the keyboard to assign that key's code to the in-game button action.

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

### STARTING THE GAME (ALPHA)

This section pertains to starting an alpha build of the game (which can be started via the `./build-run-desktop-alpha.sh`
script), and it will be updated when beta and release versions are eventually released. To run an 

When launching an alpha build, the first screen you see should be something like the following.

<img src="img/StartAlphaGame.png" width="600px"/>

Press the key that corresponds to the command you wish to run. To quit the app, press the ESCAPE key.

If you press ENTER, you will be taken to the "Select Level" screen. Here, you can select any level you want to play.
Levels are separated into three categories:
- Beta: Levels that are over halfway complete but may require some level of redesigning
- Alpha: Levels that are less than halfway complete and require major redesigning
- Test: Levels that are made purely for testing game functionality, and that will not be included in release versions of the game

Navigate to the level you wish to play and press the key for START. This will launch the level. **NOTE: Due to the fact
that this game is an alpha, don't be surprised if the game crashes or if you encounter any bugs!**

To exit out of a level early, press the ESCAPE key. From the "Select Level" screen, you can return back to the start
screen by pressing the ESCAPE key.

## CREDITS

The artwork, sounds, and music used in this game are sourced from Capcom and fan works from the community. Below is a
comprehensive listing of the credits for the artwork used in this game.

### Pixel Art

#### Title Screen

- Megaman Title - [Mister Mike](https://www.spriters-resource.com/fullview/258/)
- Megaman Weapon Get Sprite - [PixelBoy127](https://www.spriters-resource.com/custom_edited/megamancustoms/sheet/166239/)

#### Bosses

- Timber Woman spritesheet & mugshot- [oldlavygenes](https://www.deviantart.com/oldlavygenes474/art/TimberWoman-Megaman-Maverick-1087794591)
- Rodent Man spritesheet & mugshot - [oldlavygenes](https://www.deviantart.com/oldlavygenes474/art/Rodent-Man-Megaman-Maverick-1087797101)
- Precious Man spritesheet & mugshot - [oldlavygenes](https://www.deviantart.com/oldlavygenes474/art/Precious-Man-Megaman-Maverick-1087800596)
- Reactor Man spritesheet & mugshot - [Balloon Cart](https://balloon-cart.itch.io/reactor-man-asset-pack)
- Moon Man spritesheet - [boberatu](https://www.deviantart.com/boberatu/art/MPN-006-Moon-Man-625679636)
- Moon Man mugshot - [oldlavygenes](https://www.deviantart.com/oldlavygenes474/art/Moon-Man-Mugshot-Based-on-boberatu-s-sprite-1088201420)
- Glacier Man spritesheet & mugshot - [Balloon Cart](https://balloon-cart.itch.io/glacier-man-asset-pack)
- Inferno Man spritesheet & mugshot - [boberatu](https://www.deviantart.com/boberatu/art/MPN-000-Volcano-Man-313694441)
- Desert Man spritesheet & mugshot - [rcrdcat](https://www.deviantart.com/rcrdcat/art/Desert-Man-Spritesheet-Mugshot-332165249)
- Bospider spritesheet - [Bean and Shawn](https://www.spriters-resource.com/game_boy_gbc/mmxtreme/sheet/480/?source=genre)
- Guts Tank spritesheet - [Mister Mike](https://www.spriters-resource.com/nes/mm2/sheet/2317/)
- Mecha Dragon spritesheet - [Mister Mike](https://www.spriters-resource.com/nes/mm2/sheet/2317/)
- Moon Head spritesheet - [oldlavygenes](https://www.deviantart.com/oldlavygenes474/art/MoonHead-Miniboss-Megaman-Maverick-1090151707)
- Penpen Maker spritesheet - [Mister Mike](https://www.spriters-resource.com/nes/mm3/sheet/77911/)
- Nuclear Monkey spritesheet - [Balloon Cart](https://balloon-cart.itch.io/reactor-man-asset-pack)
- Sphinx spritesheet - [Mister Mike](https://www.spriters-resource.com/custom_edited/megamancustoms/sheet/108177/)

#### Enemies
- Sprites by [MegaRed225](https://www.deviantart.com/megared225/gallery):
  - [Random Enemies Set 5](https://www.deviantart.com/megared225/art/Random-Enemies-Set-5-517418497):
    - Big Fish Neo 

#### Tilesets

- [Balloon Cart (Reactor Man)](https://balloon-cart.itch.io/reactor-man-asset-pack)
- [Balloon Cart (Glacier Man)](https://balloon-cart.itch.io/glacier-man-asset-pack)
- [MegaBot](https://ansimuz.itch.io/mega-bot)
- [Mega 8-Bit](https://assetstore.unity.com/packages/2d/environments/mega-8-bit-pixel-pack-60158?srsltid=AfmBOordeWICo0KR-N3MKcw6iqd2TehrlFgQn6Hijzmk09-2eoq2Gid0)
- TODO: All other tilesets ripped from Capcom's official Megaman games via the Spriter's Resource website (TODO: will provide links and credits for each tileset used from here)

### Music

- MMX6 Blaze Heatnix - [Famitard](https://youtu.be/QpbMwCnJDSo) - used in Inferno Man stage
- MMX5 Dark Necrobat - [Famitard](https://youtu.be/RosxPCxVOyk) - used in Moon Man stage
- Xenoblade Gaur Plains - [Bulby](https://www.youtube.com/watch?v=xkrf4xfDsZs&t=60s&ab_channel=Bulby) - used in Timber Woman stage
- MMX7 Vanish. Gung. - [Famitard](https://youtu.be/MFfZ-LEwcMo) - used in Desert Man stage
- MM8 Frost Man Alt - [Famitard](https://youtu.be/pQvwU4BavFI) - used in Glacier Man stage
- MMX8 Burn Rooster - [Famitard](https://youtu.be/DHh-QSWvb-o) - used in Reactor Man stage
- MMX6 Boss - [Famitard](https://youtu.be/IeySHEF5U_8) - used in Robot Master boss fights
- MMX Sigma Fortress 1 - [Wyatt](https://youtu.be/mpHnNDPZNKk) - used in Wily stage 1
- MMX Sigma Fortress 2 - [Bulby](https://youtu.be/WQK9AGaDJLw) - used in Wily stage 2
- MMX6 Gate's Laboratory- [Famitard](https://youtu.be/Zdtp6f57E_c) - used in Wily stage 3
- MMX6 Sigma Final Boss - [Famitard](https://youtu.be/FVdYxfEo4lI) - used in Wily stage 3 boss fight
- MMX5 Volt Kraken - [Famitard](https://www.youtube.com/watch?v=xi7Odov_rek&ab_channel=FamiTard)
- TODO: Other music tracks are saved in the `assets` dir but not yet used in the game; these will be added here as they're added to the game

### Sounds

Sounds belong to Capcom (Megaman-ripped sound effects) and Nintendo (Mario-ripped sound effects)
TODO: add credits and links here for where the sounds were downloaded from

---

**PROOF OF PERMISSIONS FOR COMMUNITY ASSETS**

For some of the artwork used in this game, the artist did not state in the work's description or elsewhere that the work was
free to use. For these works, I reached out to the artist personally to ask if I could use their work. Below are screenshots
as proof of permission. No proof is required for works where the description states that the asset is free to use.

Works by Wyatt:

<img src="./img/permissions/Wyatt.png" width="300"/>

Works by boberatu:

<img src="./img/permissions/boberatu.png" width="300">

Works by rcrdcat:

<img src="./img/permissions/rcrdcat.png" width="300">

Works by Famitard:

<img src="./img/permissions/famitard.png" width="300">

Works by Bulby:

<img src="./img/permissions/Bulby.png" width="300">

---

# GDX Liftoff

This project was generated with a template including simple application launchers and an `ApplicationAdapter` extension that draws libGDX logo.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.

## Gradle

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
