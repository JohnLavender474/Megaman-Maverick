package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Position.Companion.get
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.time.Timer
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.screens.levels.Level
import com.megaman.maverick.game.utils.LoopedSuppliers
import java.util.*

enum class BossType(val bossName: String, val level: Level, val position: Position, val ass: TextureAsset) {
    TIMBER_WOMAN("Timber Woman", Level.TIMBER_WOMAN, Position.TOP_LEFT, TextureAsset.TIMBER_WOMAN) {
        override fun getSpriteSize(): Vector2 {
            return Vector2(4.25f, 3.5f)
        }

        override fun getAnims(textureAtlas: TextureAtlas): MutableMap<String, Animation> {
            val map: MutableMap<String, Animation> = HashMap<String, Animation>()
            map.put("Stand", Animation(textureAtlas.findRegion("Stand"), 1, 2, Array.with<Float?>(1.5f, .15f), true))
            map.put("JustLand", Animation(textureAtlas.findRegion("JustLand"), 1, 5, .1f, false))
            map.put("Swing", Animation(textureAtlas.findRegion("Swing"), 1, 6, .1f, false))
            map.put("Jump", Animation(textureAtlas.findRegion("Jump"), 1, 4, .1f, true))
            return map
        }

        override fun getIntroAnimsQ(textureAtlas: TextureAtlas): Queue<GamePair<Animation, Timer>> {
            val anims = getAnims(textureAtlas)
            val timerPairs: Queue<GamePair<Animation, Timer>> = LinkedList<GamePair<Animation, Timer>>()
            timerPairs.add(GamePair<Animation, Timer>(anims["Jump"]!!, Timer(ConstVals.BOSS_DROP_DOWN_DURATION)))
            timerPairs.add(GamePair<Animation, Timer>(anims["JustLand"]!!, Timer(0.5f)))
            timerPairs.add(GamePair<Animation, Timer>(anims["Stand"]!!, Timer(1.75f)))
            timerPairs.add(GamePair<Animation, Timer>(anims["Swing"]!!, Timer(4f)))
            return timerPairs
        }
    },

    DESERT_MAN("Desert Man", Level.DESERT_MAN, Position.CENTER_LEFT, TextureAsset.BOSSES_1) {
        override fun getSpriteSize(): Vector2 {
            return Vector2(1.85f, 1.5f)
        }

        override fun getAnims(textureAtlas: TextureAtlas): MutableMap<String, Animation> {
            val map: MutableMap<String, Animation> = HashMap<String, Animation>()
            // TODO:
            /*
            map.put("Stand", new Animation(textureAtlas.findRegion("Stand"), 1, 2, Array.with(1.5f, 0.15f), true));
            map.put("Jump", new Animation(textureAtlas.findRegion("Jump")));
            map.put("JumpShock", new Animation(textureAtlas.findRegion("JumpShock"), 1, 2, .15f, true));
            map.put("JustLand", new Animation(textureAtlas.findRegion("JustLand"), 1, 2, .15f, false));
            map.put("Shock", new Animation(textureAtlas.findRegion("Shock"), 1, 2, .15f, true));
            map.put("Damaged", new Animation(textureAtlas.findRegion("Damaged"), 1, 2, .15f, true));
             */
            return map
        }

        override fun getIntroAnimsQ(textureAtlas: TextureAtlas): Queue<GamePair<Animation, Timer>> {
            val anims = getAnims(textureAtlas)
            val timerPairs: Queue<GamePair<Animation, Timer>> = LinkedList<GamePair<Animation, Timer>>()
            // TODO:
            /*
            timerPairs.add(new GamePair<>(anims[ "Jump" ]!!, new Timer(ConstVals.BOSS_DROP_DOWN_DURATION)));
            timerPairs.add(new GamePair<>(anims[ "JustLand" ]!!, new Timer(.3f)));
            timerPairs.add(new GamePair<>(anims[ "Stand" ]!!, new Timer(2.15f)));
            timerPairs.add(new GamePair<>(anims[ "Shock" ]!!, new Timer(1f)));
            timerPairs.add(new GamePair<>(anims[ "Stand" ]!!, new Timer(1.7f)));
             */
            return timerPairs
        }
    },

    MOON_MAN("Moon Man", Level.MOON_MAN, Position.BOTTOM_LEFT, TextureAsset.BOSSES_1) {

        override fun getSpriteSize(): Vector2 = LoopedSuppliers.getVector2().set(1.75f, 1.75f)

        override fun getAnims(textureAtlas: TextureAtlas): MutableMap<String, Animation> {
            val anims: MutableMap<String, Animation> = HashMap<String, Animation>()
            anims.put("jump", Animation(textureAtlas.findRegion("MoonMan_v2/jump")))
            anims.put("throw", Animation(textureAtlas.findRegion("MoonMan_v2/throw"), 2, 2, 0.1f, false))
            anims.put(
                "stand",
                Animation(textureAtlas.findRegion("MoonMan_v2/stand"), 2, 1, gdxArrayOf<Float>(1.5f, 0.15f), true)
            )
            return anims
        }

        override fun getIntroAnimsQ(textureAtlas: TextureAtlas): Queue<GamePair<Animation, Timer>> {
            val anims = getAnims(textureAtlas)
            val timerPairs: Queue<GamePair<Animation, Timer>> = LinkedList<GamePair<Animation, Timer>>()
            timerPairs.add(GamePair<Animation, Timer>(anims["jump"]!!, Timer(ConstVals.BOSS_DROP_DOWN_DURATION)))
            timerPairs.add(GamePair<Animation, Timer>(anims["stand"]!!, Timer(0.5f)))
            timerPairs.add(GamePair<Animation, Timer>(anims["throw"]!!, Timer(4f)))
            return timerPairs
        }
    },

    GLACIER_MAN("Glacier Man", Level.GLACIER_MAN, Position.TOP_CENTER, TextureAsset.BOSSES_1) {
        override fun getSpriteSize(): Vector2 {
            return Vector2(1.65f, 1.5f)
        }

        override fun getAnims(textureAtlas: TextureAtlas): MutableMap<String, Animation> {
            val map: MutableMap<String, Animation> = HashMap<String, Animation>()
            // TODO:
            /*
            map.put("Damaged", new Animation(textureAtlas.findRegion("Damaged"), 1, 2, .1f, true));
            map.put("Flaming", new Animation(textureAtlas.findRegion("Flaming"), 1, 2, .15f, true));
            map.put("Flex", new Animation(textureAtlas.findRegion("Flex"), 1, 2, .2f, true));
            map.put("Jump", new Animation(textureAtlas.findRegion("Jump")));
            map.put("Slide", new Animation(textureAtlas.findRegion("Slide")));
            map.put("Stand", new Animation(textureAtlas.findRegion("Stand"), 1, 2, Array.with(1.5f, .15f), true));

             */
            return map
        }

        override fun getIntroAnimsQ(textureAtlas: TextureAtlas): Queue<GamePair<Animation, Timer>> {
            val anims = getAnims(textureAtlas)
            val timerPairs: Queue<GamePair<Animation, Timer>> = LinkedList<GamePair<Animation, Timer>>()
            // TODO:
            /*
            timerPairs.add(new GamePair<>(anims[ "Jump" ]!!, new Timer(ConstVals.BOSS_DROP_DOWN_DURATION)));
            timerPairs.add(new GamePair<>(anims[ "Stand" ]!!, new Timer(1f)));
            timerPairs.add(new GamePair<>(anims[ "Flex" ]!!, new Timer(1.5f)));
            timerPairs.add(new GamePair<>(anims[ "Slide" ]!!, new Timer(.75f)));
            timerPairs.add(new GamePair<>(anims[ "Stand" ]!!, new Timer(3.5f)));
             */
            return timerPairs
        }
    },

    PRECIOUS_MAN("Precious Man", Level.TEST1, Position.BOTTOM_CENTER, TextureAsset.BOSSES_1) {
        override fun getSpriteSize(): Vector2 {
            return Vector2(2.85f, 2.5f)
        }

        override fun getAnims(atlas: TextureAtlas): MutableMap<String, Animation> {
            val map: MutableMap<String, Animation> = HashMap<String, Animation>()
            /*e
            map.put("Jump", new Animation(atlas.findRegion("Jump"), 1, 2, .15f, false));
            map.put("JumpFreeze", new Animation(atlas.findRegion("JumpFreeze"), 1, 4, .15f, false));
            map.put("Run", new Animation(atlas.findRegion("Jump"), 1, 4, .15f, true));
            map.put("Stand", new Animation(atlas.findRegion("Stand"), 1, 2, Array.with(1.25f, .15f), true));
            map.put("StandFreeze", new Animation(atlas.findRegion("StandFreeze"), 1, 3, .15f, true));
            map.put("StandShoot", new Animation(atlas.findRegion("StandShoot")));
             */
            return map
        }

        override fun getIntroAnimsQ(textureAtlas: TextureAtlas): Queue<GamePair<Animation, Timer>> {
            val anims = getAnims(textureAtlas)
            val timerPairs: Queue<GamePair<Animation, Timer>> = LinkedList<GamePair<Animation, Timer>>()
            /*
            timerPairs.add(new GamePair<>(anims[ "Jump" ]!!, new Timer(ConstVals.BOSS_DROP_DOWN_DURATION)));
            timerPairs.add(new GamePair<>(anims[ "StandShoot" ]!!, new Timer(.15f)));
            timerPairs.add(new GamePair<>(anims[ "Stand" ]!!, new Timer(1.6f)));
            timerPairs.add(new GamePair<>(anims[ "StandFreeze" ]!!, new Timer(2.7f)));
            timerPairs.add(new GamePair<>(anims[ "Stand" ]!!, new Timer(2.5f)));
             */
            return timerPairs
        }
    },

    INFERNO_MAN("Inferno Man", Level.TEST1, Position.TOP_RIGHT, TextureAsset.BOSSES_1) {
        override fun getSpriteSize(): Vector2 {
            return Vector2(3f, 2.5f)
        }

        override fun getAnims(textureAtlas: TextureAtlas): MutableMap<String, Animation> {
            val map: MutableMap<String, Animation> = HashMap<String, Animation>()
            // TODO:
            /*
            map.put("Aim", new Animation(textureAtlas.findRegion("Aim")));
            map.put("CoolPose", new Animation(textureAtlas.findRegion("CoolPose"), 1, 2, .3f, false));
            map.put("FallingWithStyle", new Animation(textureAtlas.findRegion("FallingWithStyle"), 1, 2, 0.05f, true));
            map.put("FlyFlap", new Animation(textureAtlas.findRegion("FlyFlap"), 1, 2, .2f, true));
            map.put("RetractWings", new Animation(textureAtlas.findRegion("RetractWings")));
            map.put("Stand", new Animation(textureAtlas.findRegion("Stand"), 1, 2, Array.with(1.5f, .15f), true));
            map.put("StandFlap", new Animation(textureAtlas.findRegion("StandFlap"), 1, 2, .2f, true));
            map.put("SuaveCombSweep", new Animation(textureAtlas.findRegion("SuaveCombSweep"), 1, 2, .2f, true));
             */
            return map
        }

        override fun getIntroAnimsQ(textureAtlas: TextureAtlas): Queue<GamePair<Animation, Timer>> {
            val anims = getAnims(textureAtlas)
            val timerPairs: Queue<GamePair<Animation, Timer>> = LinkedList<GamePair<Animation, Timer>>()
            // TODO:
            /*
            timerPairs.add(new GamePair<>(anims[ "FlyFlap" ]!!, new Timer(ConstVals.BOSS_DROP_DOWN_DURATION)));
            timerPairs.add(new GamePair<>(anims[ "StandFlap" ]!!, new Timer(1.5f)));
            timerPairs.add(new GamePair<>(anims[ "RetractWings" ]!!, new Timer(.2f)));
            timerPairs.add(new GamePair<>(anims[ "SuaveCombSweep" ]!!, new Timer(.8f)));
            timerPairs.add(new GamePair<>(anims[ "CoolPose" ]!!, new Timer(4.25f)));
             */
            return timerPairs
        }
    },

    REACTOR_MAN("Reactor Man", Level.REACTOR_MAN, Position.CENTER_RIGHT, TextureAsset.MICROWAVE_MAN) {
        override fun getSpriteSize(): Vector2 {
            return Vector2(2.85f, 2.5f)
        }

        override fun getAnims(textureAtlas: TextureAtlas): MutableMap<String, Animation> {
            val map: MutableMap<String, Animation> = HashMap<String, Animation>()
            map.put("HeadlessJump", Animation(textureAtlas.findRegion("HeadlessJump")))
            map.put("HeadlessOpenDoor", Animation(textureAtlas.findRegion("HeadlessOpenDoor")))
            map.put("HeadlessShoot", Animation(textureAtlas.findRegion("HeadlessShoot")))
            map.put("HeadlessStand", Animation(textureAtlas.findRegion("HeadlessStand")))
            map.put("Jump", Animation(textureAtlas.findRegion("Jump")))
            map.put("OpenDoor", Animation(textureAtlas.findRegion("OpenDoor")))
            map.put("Shoot", Animation(textureAtlas.findRegion("Shoot")))
            map.put("Stand", Animation(textureAtlas.findRegion("Stand"), 1, 2, Array.with<Float?>(1.5f, .15f), true))
            return map
        }

        override fun getIntroAnimsQ(textureAtlas: TextureAtlas): Queue<GamePair<Animation, Timer>> {
            val anims = getAnims(textureAtlas)
            val timerPairs: Queue<GamePair<Animation, Timer>> = LinkedList<GamePair<Animation, Timer>>()
            timerPairs.add(GamePair<Animation, Timer>(anims["Jump"]!!, Timer(ConstVals.BOSS_DROP_DOWN_DURATION)))
            timerPairs.add(GamePair<Animation, Timer>(anims["Stand"]!!, Timer(1.5f)))
            timerPairs.add(GamePair<Animation, Timer>(anims["Shoot"]!!, Timer(1.5f)))
            timerPairs.add(GamePair<Animation, Timer>(anims["OpenDoor"]!!, Timer(2.5f)))
            timerPairs.add(GamePair<Animation, Timer>(anims["Stand"]!!, Timer(1.25f)))
            return timerPairs
        }
    },

    RODENT_MAN("Rodent Man", Level.RODENT_MAN, Position.BOTTOM_RIGHT, TextureAsset.RODENT_MAN) {
        override fun getSpriteSize(): Vector2 {
            return Vector2(2.25f, 1.85f)
        }

        override fun getAnims(textureAtlas: TextureAtlas): MutableMap<String, Animation> {
            val map: MutableMap<String, Animation> = HashMap<String, Animation>()
            map.put("Jump", Animation(textureAtlas.findRegion("Jump"), 1, 4, 0.15f, true))
            map.put("Run", Animation(textureAtlas.findRegion("Run"), 1, 4, 0.15f, true))
            map.put("Shoot", Animation(textureAtlas.findRegion("Shoot"), 1, 3, 0.15f, false))
            map.put("Slash", Animation(textureAtlas.findRegion("Slash"), 1, 2, 0.15f, false))
            map.put("Stand", Animation(textureAtlas.findRegion("Stand"), 1, 6, 0.15f, true))
            map.put("StandStill", Animation(textureAtlas.findRegion("StandStill")))
            map.put("WallSlide", Animation(textureAtlas.findRegion("WallSlide"), 1, 2, 0.15f, true))
            return map
        }

        override fun getIntroAnimsQ(textureAtlas: TextureAtlas): Queue<GamePair<Animation, Timer>> {
            val anims = getAnims(textureAtlas)
            val timerPairs: Queue<GamePair<Animation, Timer>> = LinkedList<GamePair<Animation, Timer>>()
            timerPairs.add(GamePair<Animation, Timer>(anims["Jump"]!!, Timer(ConstVals.BOSS_DROP_DOWN_DURATION)))
            timerPairs.add(GamePair<Animation, Timer>(anims["Stand"]!!, Timer(2f)))
            timerPairs.add(GamePair<Animation, Timer>(anims["Run"]!!, Timer(0.6f)))
            timerPairs.add(GamePair<Animation, Timer>(anims["Slash"]!!, Timer(0.45f)))
            timerPairs.add(GamePair<Animation, Timer>(anims["Slash"]!!, Timer(0.45f)))
            timerPairs.add(GamePair<Animation, Timer>(anims["StandStill"]!!, Timer(3f)))
            return timerPairs
        }
    };

    abstract fun getSpriteSize(): Vector2

    abstract fun getAnims(textureAtlas: TextureAtlas): MutableMap<String, Animation>

    abstract fun getIntroAnimsQ(textureAtlas: TextureAtlas): Queue<GamePair<Animation, Timer>>

    companion object {
        fun findByName(name: String): BossType? {
            for (boss in BossType.entries) if (name == boss.name) return boss
            return null
        }

        fun findByPos(x: Int, y: Int) = findByPos(get(x, y))

        fun findByPos(position: Position?): BossType? {
            for (boss in BossType.entries) if (boss.position == position) return boss
            return null
        }
    }
}
