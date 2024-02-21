package com.megaman.maverick.game.entities.bosses;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.engine.animations.Animation;
import com.engine.common.enums.Position;
import com.engine.common.time.Timer;
import com.megaman.maverick.game.ConstVals;
import com.megaman.maverick.game.assets.TextureAsset;
import com.megaman.maverick.game.screens.levels.Level;
import kotlin.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public enum BossType {
    TIMBER_WOMAN("Timber Woman", Level.TIMBER_WOMAN, Position.TOP_LEFT, TextureAsset.TIMBER_WOMAN) {
        @Override
        public Vector2 getSpriteSize() {
            return new Vector2(4.25f, 3.5f);
        }

        @Override
        public Map<String, Animation> getAnims(TextureAtlas textureAtlas) {
            Map<String, Animation> map = new HashMap<>();
            map.put("Stand", new Animation(textureAtlas.findRegion("Stand"), 1, 2, Array.with(1.5f, .15f), true));
            map.put("JustLand", new Animation(textureAtlas.findRegion("JustLand"), 1, 5, .1f, false));
            map.put("Swing", new Animation(textureAtlas.findRegion("Swing"), 1, 6, .1f, false));
            map.put("Jump", new Animation(textureAtlas.findRegion("Jump"), 1, 4, .1f, true));
            return map;
        }

        @Override
        public Queue<Pair<Animation, Timer>> getIntroAnimsQ(TextureAtlas textureAtlas) {
            Map<String, Animation> anims = getAnims(textureAtlas);
            Queue<Pair<Animation, Timer>> timerPairs = new LinkedList<>();
            timerPairs.add(new Pair<>(anims.get("Jump"), new Timer(ConstVals.BOSS_DROP_DOWN_DURATION)));
            timerPairs.add(new Pair<>(anims.get("JustLand"), new Timer(.5f)));
            timerPairs.add(new Pair<>(anims.get("Stand"), new Timer(1.75f)));
            timerPairs.add(new Pair<>(anims.get("Swing"), new Timer(4f)));
            return timerPairs;
        }
    },
    DISTRIBUTOR_MAN("Distributor Man", Level.TEST1, Position.TOP_CENTER, TextureAsset.DISTRIBUTOR_MAN) {
        @Override
        public Vector2 getSpriteSize() {
            return new Vector2(1.85f, 1.5f);
        }

        @Override
        public Map<String, Animation> getAnims(TextureAtlas textureAtlas) {
            Map<String, Animation> map = new HashMap<>();
            map.put("Stand", new Animation(textureAtlas.findRegion("Stand"), 1, 2, Array.with(1.5f, 0.15f), true));
            map.put("Jump", new Animation(textureAtlas.findRegion("Jump")));
            map.put("JumpShock", new Animation(textureAtlas.findRegion("JumpShock"), 1, 2, .15f, true));
            map.put("JustLand", new Animation(textureAtlas.findRegion("JustLand"), 1, 2, .15f, false));
            map.put("Shock", new Animation(textureAtlas.findRegion("Shock"), 1, 2, .15f, true));
            map.put("Damaged", new Animation(textureAtlas.findRegion("Damaged"), 1, 2, .15f, true));
            return map;
        }

        @Override
        public Queue<Pair<Animation, Timer>> getIntroAnimsQ(TextureAtlas textureAtlas) {
            Map<String, Animation> anims = getAnims(textureAtlas);
            Queue<Pair<Animation, Timer>> timerPairs = new LinkedList<>();
            timerPairs.add(new Pair<>(anims.get("Jump"), new Timer(ConstVals.BOSS_DROP_DOWN_DURATION)));
            timerPairs.add(new Pair<>(anims.get("JustLand"), new Timer(.3f)));
            timerPairs.add(new Pair<>(anims.get("Stand"), new Timer(2.15f)));
            timerPairs.add(new Pair<>(anims.get("Shock"), new Timer(1f)));
            timerPairs.add(new Pair<>(anims.get("Stand"), new Timer(1.7f)));
            return timerPairs;
        }
    },
    ROASTER_MAN("Roaster Man", Level.TEST1, Position.TOP_RIGHT, TextureAsset.ROASTER_MAN) {
        @Override
        public Vector2 getSpriteSize() {
            return new Vector2(3f, 2.5f);
        }

        @Override
        public Map<String, Animation> getAnims(TextureAtlas textureAtlas) {
            Map<String, Animation> map = new HashMap<>();
            map.put("Aim", new Animation(textureAtlas.findRegion("Aim")));
            map.put("CoolPose", new Animation(textureAtlas.findRegion("CoolPose"), 1, 2, .3f, false));
            map.put("FallingWithStyle", new Animation(textureAtlas.findRegion("FallingWithStyle"), 1, 2, 0.05f, true));
            map.put("FlyFlap", new Animation(textureAtlas.findRegion("FlyFlap"), 1, 2, .2f, true));
            map.put("RetractWings", new Animation(textureAtlas.findRegion("RetractWings")));
            map.put("Stand", new Animation(textureAtlas.findRegion("Stand"), 1, 2, Array.with(1.5f, .15f), true));
            map.put("StandFlap", new Animation(textureAtlas.findRegion("StandFlap"), 1, 2, .2f, true));
            map.put("SuaveCombSweep", new Animation(textureAtlas.findRegion("SuaveCombSweep"), 1, 2, .2f, true));
            return map;
        }

        @Override
        public Queue<Pair<Animation, Timer>> getIntroAnimsQ(TextureAtlas textureAtlas) {
            Map<String, Animation> anims = getAnims(textureAtlas);
            Queue<Pair<Animation, Timer>> timerPairs = new LinkedList<>();
            timerPairs.add(new Pair<>(anims.get("FlyFlap"), new Timer(ConstVals.BOSS_DROP_DOWN_DURATION)));
            timerPairs.add(new Pair<>(anims.get("StandFlap"), new Timer(1.5f)));
            timerPairs.add(new Pair<>(anims.get("RetractWings"), new Timer(.2f)));
            timerPairs.add(new Pair<>(anims.get("SuaveCombSweep"), new Timer(.8f)));
            timerPairs.add(new Pair<>(anims.get("CoolPose"), new Timer(4.25f)));
            return timerPairs;
        }
    },
    MISTER_MAN("Mister Man", Level.TEST1, Position.CENTER_LEFT, TextureAsset.MISTER_MAN) {
        @Override
        public Vector2 getSpriteSize() {
            return new Vector2(3.25f, 2.85f);
        }

        @Override
        public Map<String, Animation> getAnims(TextureAtlas textureAtlas) {
            Map<String, Animation> map = new HashMap<>();
            map.put("Stand", new Animation(textureAtlas.findRegion("Stand"), 1, 2, Array.with(1.5f, .15f), true));
            map.put("Jump", new Animation(textureAtlas.findRegion("Jump")));
            map.put("Flex", new Animation(textureAtlas.findRegion("Flex"), 1, 2, .2f, true));
            map.put("Electrocuted", new Animation(textureAtlas.findRegion("Electrocuted"), 1, 2, .1f, true));
            map.put("Squirt", new Animation(textureAtlas.findRegion("Squirt"), 1, 2, .1f, true));
            return map;
        }

        @Override
        public Queue<Pair<Animation, Timer>> getIntroAnimsQ(TextureAtlas textureAtlas) {
            Map<String, Animation> anims = getAnims(textureAtlas);
            Queue<Pair<Animation, Timer>> timerPairs = new LinkedList<>();
            timerPairs.add(new Pair<>(anims.get("Jump"), new Timer(ConstVals.BOSS_DROP_DOWN_DURATION)));
            timerPairs.add(new Pair<>(anims.get("Stand"), new Timer(1f)));
            timerPairs.add(new Pair<>(anims.get("Flex"), new Timer(1.5f)));
            timerPairs.add(new Pair<>(anims.get("Squirt"), new Timer(1f)));
            timerPairs.add(new Pair<>(anims.get("Stand"), new Timer(3.25f)));
            return timerPairs;
        }
    },
    BLUNT_MAN("Blunt Man", Level.TEST1, Position.CENTER_RIGHT, TextureAsset.BLUNT_MAN) {
        @Override
        public Vector2 getSpriteSize() {
            return new Vector2(1.65f, 1.5f);
        }

        @Override
        public Map<String, Animation> getAnims(TextureAtlas textureAtlas) {
            Map<String, Animation> map = new HashMap<>();
            map.put("Damaged", new Animation(textureAtlas.findRegion("Damaged"), 1, 2, .1f, true));
            map.put("Flaming", new Animation(textureAtlas.findRegion("Flaming"), 1, 2, .15f, true));
            map.put("Flex", new Animation(textureAtlas.findRegion("Flex"), 1, 2, .2f, true));
            map.put("Jump", new Animation(textureAtlas.findRegion("Jump")));
            map.put("Slide", new Animation(textureAtlas.findRegion("Slide")));
            map.put("Stand", new Animation(textureAtlas.findRegion("Stand"), 1, 2, Array.with(1.5f, .15f), true));
            return map;
        }

        @Override
        public Queue<Pair<Animation, Timer>> getIntroAnimsQ(TextureAtlas textureAtlas) {
            Map<String, Animation> anims = getAnims(textureAtlas);
            Queue<Pair<Animation, Timer>> timerPairs = new LinkedList<>();
            timerPairs.add(new Pair<>(anims.get("Jump"), new Timer(ConstVals.BOSS_DROP_DOWN_DURATION)));
            timerPairs.add(new Pair<>(anims.get("Stand"), new Timer(1f)));
            timerPairs.add(new Pair<>(anims.get("Flex"), new Timer(1.5f)));
            timerPairs.add(new Pair<>(anims.get("Slide"), new Timer(.75f)));
            timerPairs.add(new Pair<>(anims.get("Stand"), new Timer(3.5f)));
            return timerPairs;
        }
    },

    // TODO: change to precious man texture asset
    PRECIOUS_MAN("Precious Man", Level.TEST1, Position.BOTTOM_LEFT, TextureAsset.PRECIOUS_MAN) {
        @Override
        public Vector2 getSpriteSize() {
            return new Vector2(2.85f, 2.5f);
        }

        @Override
        public Map<String, Animation> getAnims(TextureAtlas atlas) {
            Map<String, Animation> map = new HashMap<>();
            map.put("Jump", new Animation(atlas.findRegion("Jump"), 1, 2, .15f, false));
            map.put("JumpFreeze", new Animation(atlas.findRegion("JumpFreeze"), 1, 4, .15f, false));
            map.put("Run", new Animation(atlas.findRegion("Jump"), 1, 4, .15f, true));
            map.put("Stand", new Animation(atlas.findRegion("Stand"), 1, 2, Array.with(1.25f, .15f), true));
            map.put("StandFreeze", new Animation(atlas.findRegion("StandFreeze"), 1, 3, .15f, true));
            map.put("StandShoot", new Animation(atlas.findRegion("StandShoot")));
            return map;
        }

        @Override
        public Queue<Pair<Animation, Timer>> getIntroAnimsQ(TextureAtlas textureAtlas) {
            Map<String, Animation> anims = getAnims(textureAtlas);
            Queue<Pair<Animation, Timer>> timerPairs = new LinkedList<>();
            timerPairs.add(new Pair<>(anims.get("Jump"), new Timer(ConstVals.BOSS_DROP_DOWN_DURATION)));
            timerPairs.add(new Pair<>(anims.get("StandShoot"), new Timer(.15f)));
            timerPairs.add(new Pair<>(anims.get("Stand"), new Timer(1.6f)));
            timerPairs.add(new Pair<>(anims.get("StandFreeze"), new Timer(2.7f)));
            timerPairs.add(new Pair<>(anims.get("Stand"), new Timer(2.5f)));
            return timerPairs;
        }
    },
    RODENT_MAN("Rodent Man", Level.RODENT_MAN, Position.BOTTOM_CENTER, TextureAsset.RODENT_MAN) {
        @Override
        public Vector2 getSpriteSize() {
            return new Vector2(2.5f, 2f);
        }

        @Override
        public Map<String, Animation> getAnims(TextureAtlas textureAtlas) {
            Map<String, Animation> map = new HashMap<>();
            map.put("Jump", new Animation(textureAtlas.findRegion("Jump"), 1, 4, .15f, true));
            map.put("Run", new Animation(textureAtlas.findRegion("Run"), 1, 4, .15f, true));
            map.put("Shoot", new Animation(textureAtlas.findRegion("Shoot"), 1, 3, .15f, false));
            map.put("Slash", new Animation(textureAtlas.findRegion("Slash"), 1, 2, .15f, false));
            map.put("Stand", new Animation(textureAtlas.findRegion("Stand"), 1, 6, .15f, true));
            map.put("WallSlide", new Animation(textureAtlas.findRegion("WallSlide"), 1, 2, .15f, true));
            return map;
        }

        @Override
        public Queue<Pair<Animation, Timer>> getIntroAnimsQ(TextureAtlas textureAtlas) {
            Map<String, Animation> anims = getAnims(textureAtlas);
            Queue<Pair<Animation, Timer>> timerPairs = new LinkedList<>();
            timerPairs.add(new Pair<>(anims.get("Jump"), new Timer(ConstVals.BOSS_DROP_DOWN_DURATION)));
            timerPairs.add(new Pair<>(anims.get("Stand"), new Timer(2f)));
            timerPairs.add(new Pair<>(anims.get("Run"), new Timer(.6f)));
            timerPairs.add(new Pair<>(anims.get("Slash"), new Timer(.45f)));
            timerPairs.add(new Pair<>(anims.get("Slash"), new Timer(.45f)));
            timerPairs.add(new Pair<>(anims.get("Stand"), new Timer(3f)));
            return timerPairs;
        }
    },
    MICROWAVE_MAN("Microwave Man", Level.TEST1, Position.BOTTOM_RIGHT, TextureAsset.MICROWAVE_MAN) {
        @Override
        public Vector2 getSpriteSize() {
            return new Vector2(2.85f, 2.5f);
        }

        @Override
        public Map<String, Animation> getAnims(TextureAtlas textureAtlas) {
            Map<String, Animation> map = new HashMap<>();
            map.put("HeadlessJump", new Animation(textureAtlas.findRegion("HeadlessJump")));
            map.put("HeadlessOpenDoor", new Animation(textureAtlas.findRegion("HeadlessOpenDoor")));
            map.put("HeadlessShoot", new Animation(textureAtlas.findRegion("HeadlessShoot")));
            map.put("HeadlessStand", new Animation(textureAtlas.findRegion("HeadlessStand")));
            map.put("Jump", new Animation(textureAtlas.findRegion("Jump")));
            map.put("OpenDoor", new Animation(textureAtlas.findRegion("OpenDoor")));
            map.put("Shoot", new Animation(textureAtlas.findRegion("Shoot")));
            map.put("Stand", new Animation(textureAtlas.findRegion("Stand"), 1, 2, Array.with(1.5f, .15f), true));
            return map;
        }

        @Override
        public Queue<Pair<Animation, Timer>> getIntroAnimsQ(TextureAtlas textureAtlas) {
            Map<String, Animation> anims = getAnims(textureAtlas);
            Queue<Pair<Animation, Timer>> timerPairs = new LinkedList<>();
            timerPairs.add(new Pair<>(anims.get("Jump"), new Timer(ConstVals.BOSS_DROP_DOWN_DURATION)));
            timerPairs.add(new Pair<>(anims.get("Stand"), new Timer(1.5f)));
            timerPairs.add(new Pair<>(anims.get("Shoot"), new Timer(1.5f)));
            timerPairs.add(new Pair<>(anims.get("OpenDoor"), new Timer(2.5f)));
            timerPairs.add(new Pair<>(anims.get("Stand"), new Timer(1.25f)));
            return timerPairs;
        }
    };

    public final String name;
    public final Level level;
    public final Position position;
    public final TextureAsset ass;

    BossType(String name, Level level, Position position, TextureAsset ass) {
        this.name = name;
        this.level = level;
        this.position = position;
        this.ass = ass;
    }

    public static BossType findByName(String name) {
        for (BossType boss : values()) {
            if (name.equals(boss.name)) {
                return boss;
            }
        }
        return null;
    }

    public static BossType findByPos(int x, int y) {
        return BossType.findByPos(Position.Companion.get(x, y));
    }

    public static BossType findByPos(Position position) {
        for (BossType boss : values()) {
            if (boss.position.equals(position)) {
                return boss;
            }
        }
        return null;
    }

    public abstract Vector2 getSpriteSize();

    public abstract Map<String, Animation> getAnims(TextureAtlas textureAtlas);

    public abstract Queue<Pair<Animation, Timer>> getIntroAnimsQ(TextureAtlas textureAtlas);

}
