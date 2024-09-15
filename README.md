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

### RUNNING AN "OFFICIAL" VERSION OF THE GAME

Download the latest zipped build from one of the following links depending on your OS:
- Windows: https://drive.google.com/file/d/18KVTMErB2zWhANkvfaD5S5z3RyA3ZvuY/view?usp=sharing
- Linux: https://drive.google.com/file/d/1pHo7JE-d9CUIBNADLJdiatiP95DJP7mb/view?usp=sharing

Once the download has completed, extract the zip to a suitable location on your local desktop.

For Windows, there should be a `desktop.exe` file in the root of the unzipped directory to launch the game.

For Linux, there should be an AppImage in the `bin` directory of the unzipped directory.

**Support for Macbooks is in the works!**

Please note that while the game is in development, new "official" versions will not be generated often. Therefore, as
long as there is no "release" version of the game, it is recommended to build the game from source for the latest
changes and fixes.

### BUILDING THE GAME FROM SOURCE

Since the game runs using gradle and JRE, you can build a JAR of the game yourself. Keep in mind that in order to run
the generated JAR, you will need Java 17+ installed on your local machine.

#### Pre-requisites:

- git
- Java 17+

1. Use `git` to clone the project to a suitable location on your desktop.
2. From the root directory, run one of the following:

- Build and run a "distribution" JAR:
    - Windows:
        - `.\gradlew desktop:dist`
        - `java -jar .\desktop\build\libs\desktop-1.0.jar` (replace `desktop-1.0` in the JAR filename if these values
          have been changed in the root `build.gradle` file)
    - Linux/Mac:
        - `build-run-desktop.sh` (runs the same commands as those for the Windows "distribution" JAR)
- For debugging purposes, the following can be run instead:
    - Windows:
        - `.\gradlew desktop:run --args="[args here]"`where `[args here]` is replaced with the optional program args
    - Linux/Mac:
        - `./gradlew desktop:run --args="[args here]"`where `[args here]` is replaced with the optional program args
    - See [DesktopLauncher](./desktop/src/com/megaman/maverick/game/DesktopLauncher.java) for more info regarding app args

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

### STARTING THE GAME

This section currently pertains to starting an alpha build of the game, and it will be updated when beta and release
versions are eventually released.

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

Megaman Weapon Get Sprite - [PixelBoy127](https://www.spriters-resource.com/custom_edited/megamancustoms/sheet/166239/)

#### Bosses

- Timber Woman - [oldlavygenes](https://www.deviantart.com/oldlavygenes474/art/TimberWoman-Megaman-Maverick-1087794591)
- Rodent Man - [oldlavygenes](https://www.deviantart.com/oldlavygenes474/art/Rodent-Man-Megaman-Maverick-1087797101)
- Reactor Man - [Balloon Cart](https://balloon-cart.itch.io/reactor-man-asset-pack)
- Moon Man - [boberatu](https://www.deviantart.com/boberatu/art/MPN-006-Moon-Man-625679636)
- Glacier Man - [Balloon Cart](https://balloon-cart.itch.io/glacier-man-asset-pack)
- Inferno Man - [boberatu](https://www.deviantart.com/boberatu/art/MPN-000-Volcano-Man-313694441)
- Polarity man - [pharos04](https://www.deviantart.com/pharos04/art/Polarity-Man-ORN-010-570273324)
- Desert Man - [rcrdcat](https://www.deviantart.com/rcrdcat/art/Desert-Man-Spritesheet-Mugshot-332165249)
- Bospider - [Bean and Shawn](https://www.spriters-resource.com/game_boy_gbc/mmxtreme/sheet/480/?source=genre)
- Guts Tank - [Mister Mike](https://www.spriters-resource.com/nes/mm2/sheet/2317/)
- Mecha Dragon - [Mister Mike](https://www.spriters-resource.com/nes/mm2/sheet/2317/)
- Moon Head - [oldlavygenes](https://www.deviantart.com/oldlavygenes474/art/MoonHead-Miniboss-Megaman-Maverick-1090151707)
- Penpen Maker - [Mister Mike](https://www.spriters-resource.com/nes/mm3/sheet/77911/)
- Nuclear Monkey - [Balloon Cart](https://balloon-cart.itch.io/reactor-man-asset-pack)
- Sphinx - [Mister Mike](https://www.spriters-resource.com/custom_edited/megamancustoms/sheet/108177/)

#### Tilesets

- [Balloon Cart (Reactor Man)](https://balloon-cart.itch.io/reactor-man-asset-pack)
- [Balloon Cart (Glacier Man)](https://balloon-cart.itch.io/glacier-man-asset-pack)
- [MegaBot](https://ansimuz.itch.io/mega-bot)
- [Mega 8-Bit](https://assetstore.unity.com/packages/2d/environments/mega-8-bit-pixel-pack-60158?srsltid=AfmBOordeWICo0KR-N3MKcw6iqd2TehrlFgQn6Hijzmk09-2eoq2Gid0)
- All other tilesets ripped from Capcom's official Megaman games

#### Music

- MM Omega Title Theme - [8-Bit Alchemy](https://www.youtube.com/watch?v=ygMXSgow2FY) - used in title screen
- MMX6 Blaze Heatnix - [Famitard](https://youtu.be/QpbMwCnJDSo) - used in Inferno Man stage
- MMX5 Dark Necrobat - [Famitard](https://youtu.be/RosxPCxVOyk) - used in Moon Man stage
- Xenoblade Gaur Plains - [Bulby](https://www.youtube.com/watch?v=xkrf4xfDsZs&t=60s&ab_channel=Bulby) - used in Timber Woman stage
- MMX7 Vanish. Gung. - [Famitard](https://youtu.be/MFfZ-LEwcMo) - used in Desert Man stage
- MM8 Frost Man Alt - [Famitard](https://youtu.be/pQvwU4BavFI) - used in Glacier Man stage
- MMX5 Shining Firefly - [Famitard](https://youtu.be/EQMQvUFC3g0) - used in Polarity Man stage
- MM Omega Sludge Man - [8-bit Alchemy](https://youtu.be/1W6vRVMyLvE) - used in Reactor Man stage
- FF7 Let the Battles Begin - [Bulby](https://www.youtube.com/watch?v=04_jviOqc3Y&t=16s&ab_channel=Bulby) - used as boss theme 1
- MMX Sigma Fortress 1 - [Wyatt](https://youtu.be/mpHnNDPZNKk) - used in Wily stage 1
- Duel of the Fates - [Chips 'N Cellos](https://youtu.be/acIkuMy5684) - used in Wily stage 1 boss fight
- MMX Sigma Fortress 2 - [Bulby](https://youtu.be/WQK9AGaDJLw) - used in Wily stage 2 boss fight
- World Revolution - [TrojanHorse711](https://youtu.be/UbA4ahXMGos) - used in Wily stage 2 boss fight
- MMZ Enemy Hall - [DjKlzonez](https://www.youtube.com/watch?v=Van2IRP7OJM&ab_channel=DjKlzonez) - used in Wily stage 3
- MMX6 Sigma Final Boss - [Famitard](https://youtu.be/FVdYxfEo4lI) - used in Wily stage 3 boss fight

####

Sounds belong to Capcom (Megaman-ripped sound effects) and Nintendo (Mario-ripped sound effects)


