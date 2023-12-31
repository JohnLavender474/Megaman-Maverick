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
    TIMBER_WOMAN("Timber Woman", Level.TEST1, Position.TOP_LEFT, TextureAsset.TIMBER_WOMAN) {
        @Override
        public Vector2 getSpriteSize() {
            return new Vector2(4.25f, 3.5f);
        }

        @Override
        public Map<String, Animation> getAnims(TextureAtlas textureAtlas) {
            return new HashMap<>() {{
                put("Stand", new Animation(textureAtlas.findRegion("Stand"), 1, 2, Array.with(1.5f, .15f), true));
                put("JustLand", new Animation(textureAtlas.findRegion("JustLand"), 1, 5, .1f, false));
                put("Swing", new Animation(textureAtlas.findRegion("Swing"), 1, 6, .1f, false));
                put("Jump", new Animation(textureAtlas.findRegion("Jump"), 1, 4, .1f, true));
            }};
        }

        @Override
        public Queue<Pair<Animation, Timer>> getIntroAnimsQ(TextureAtlas textureAtlas) {
            Map<String, Animation> anims = getAnims(textureAtlas);
            return new LinkedList<>() {{
                add(new Pair<>(anims.get("Jump"), new Timer(ConstVals.BOSS_DROP_DOWN_DURATION)));
                add(new Pair<>(anims.get("JustLand"), new Timer(.5f)));
                add(new Pair<>(anims.get("Stand"), new Timer(1.75f)));
                add(new Pair<>(anims.get("Swing"), new Timer(4f)));
            }};
        }
    },
    DISTRIBUTOR_MAN("Distributor Man", Level.TEST1, Position.TOP_CENTER, TextureAsset.DISTRIBUTOR_MAN) {
        @Override
        public Vector2 getSpriteSize() {
            return new Vector2(1.85f, 1.5f);
        }

        @Override
        public Map<String, Animation> getAnims(TextureAtlas textureAtlas) {
            return new HashMap<>() {{
                put("Stand", new Animation(textureAtlas.findRegion("Stand"), 1, 2, Array.with(1.5f, 0.15f), true));
                put("Jump", new Animation(textureAtlas.findRegion("Jump")));
                put("JumpShock", new Animation(textureAtlas.findRegion("JumpShock"), 1, 2, .15f, true));
                put("JustLand", new Animation(textureAtlas.findRegion("JustLand"), 1, 2, .15f, false));
                put("Shock", new Animation(textureAtlas.findRegion("Shock"), 1, 2, .15f, true));
                put("Damaged", new Animation(textureAtlas.findRegion("Damaged"), 1, 2, .15f, true));
            }};
        }

        @Override
        public Queue<Pair<Animation, Timer>> getIntroAnimsQ(TextureAtlas textureAtlas) {
            Map<String, Animation> anims = getAnims(textureAtlas);
            return new LinkedList<>() {{
                add(new Pair<>(anims.get("Jump"), new Timer(ConstVals.BOSS_DROP_DOWN_DURATION)));
                add(new Pair<>(anims.get("JustLand"), new Timer(.3f)));
                add(new Pair<>(anims.get("Stand"), new Timer(2.15f)));
                add(new Pair<>(anims.get("Shock"), new Timer(1f)));
                add(new Pair<>(anims.get("Stand"), new Timer(1.7f)));
            }};
        }
    },
    ROASTER_MAN("Roaster Man", Level.TEST1, Position.TOP_RIGHT, TextureAsset.ROASTER_MAN) {
        @Override
        public Vector2 getSpriteSize() {
            return new Vector2(3f, 2.5f);
        }

        @Override
        public Map<String, Animation> getAnims(TextureAtlas textureAtlas) {
            return new HashMap<>() {{
                put("Aim", new Animation(textureAtlas.findRegion("Aim")));
                put("CoolPose", new Animation(textureAtlas.findRegion("CoolPose"), 1, 2, .3f, false));
                put("FallingWithStyle", new Animation(textureAtlas.findRegion("FallingWithStyle"), 1, 2, 0.05f, true));
                put("FlyFlap", new Animation(textureAtlas.findRegion("FlyFlap"), 1, 2, .2f, true));
                put("RetractWings", new Animation(textureAtlas.findRegion("RetractWings")));
                put("Stand", new Animation(textureAtlas.findRegion("Stand"), 1, 2, Array.with(1.5f, .15f), true));
                put("StandFlap", new Animation(textureAtlas.findRegion("StandFlap"), 1, 2, .2f, true));
                put("SuaveCombSweep", new Animation(textureAtlas.findRegion("SuaveCombSweep"), 1, 2, .2f, true));
            }};
        }

        @Override
        public Queue<Pair<Animation, Timer>> getIntroAnimsQ(TextureAtlas textureAtlas) {
            Map<String, Animation> anims = getAnims(textureAtlas);
            return new LinkedList<>() {{
                add(new Pair<>(anims.get("FlyFlap"), new Timer(ConstVals.BOSS_DROP_DOWN_DURATION)));
                add(new Pair<>(anims.get("StandFlap"), new Timer(1.5f)));
                add(new Pair<>(anims.get("RetractWings"), new Timer(.2f)));
                add(new Pair<>(anims.get("SuaveCombSweep"), new Timer(.8f)));
                add(new Pair<>(anims.get("CoolPose"), new Timer(4.25f)));
            }};
        }
    },
    MISTER_MAN("Mister Man", Level.TEST1, Position.CENTER_LEFT, TextureAsset.MISTER_MAN) {
        @Override
        public Vector2 getSpriteSize() {
            return new Vector2(3.25f, 2.85f);
        }

        @Override
        public Map<String, Animation> getAnims(TextureAtlas textureAtlas) {
            return new HashMap<>() {{
                put("Stand", new Animation(textureAtlas.findRegion("Stand"), 1, 2, Array.with(1.5f, .15f), true));
                put("Jump", new Animation(textureAtlas.findRegion("Jump")));
                put("Flex", new Animation(textureAtlas.findRegion("Flex"), 1, 2, .2f, true));
                put("Electrocuted", new Animation(textureAtlas.findRegion("Electrocuted"), 1, 2, .1f, true));
                put("Squirt", new Animation(textureAtlas.findRegion("Squirt"), 1, 2, .1f, true));
            }};
        }

        @Override
        public Queue<Pair<Animation, Timer>> getIntroAnimsQ(TextureAtlas textureAtlas) {
            Map<String, Animation> anims = getAnims(textureAtlas);
            return new LinkedList<>() {{
                add(new Pair<>(anims.get("Jump"), new Timer(ConstVals.BOSS_DROP_DOWN_DURATION)));
                add(new Pair<>(anims.get("Stand"), new Timer(1f)));
                add(new Pair<>(anims.get("Flex"), new Timer(1.5f)));
                add(new Pair<>(anims.get("Squirt"), new Timer(1f)));
                add(new Pair<>(anims.get("Stand"), new Timer(3.25f)));
            }};
        }
    },
    BLUNT_MAN("Blunt Man", Level.TEST1, Position.CENTER_RIGHT, TextureAsset.BLUNT_MAN) {
        @Override
        public Vector2 getSpriteSize() {
            return new Vector2(1.65f, 1.5f);
        }

        @Override
        public Map<String, Animation> getAnims(TextureAtlas textureAtlas) {
            return new HashMap<>() {{
                put("Damaged", new Animation(textureAtlas.findRegion("Damaged"), 1, 2, .1f, true));
                put("Flaming", new Animation(textureAtlas.findRegion("Flaming"), 1, 2, .15f, true));
                put("Flex", new Animation(textureAtlas.findRegion("Flex"), 1, 2, .2f, true));
                put("Jump", new Animation(textureAtlas.findRegion("Jump")));
                put("Slide", new Animation(textureAtlas.findRegion("Slide")));
                put("Stand", new Animation(textureAtlas.findRegion("Stand"), 1, 2, Array.with(1.5f, .15f), true));
            }};
        }

        @Override
        public Queue<Pair<Animation, Timer>> getIntroAnimsQ(TextureAtlas textureAtlas) {
            Map<String, Animation> anims = getAnims(textureAtlas);
            return new LinkedList<>() {{
                add(new Pair<>(anims.get("Jump"), new Timer(ConstVals.BOSS_DROP_DOWN_DURATION)));
                add(new Pair<>(anims.get("Stand"), new Timer(1f)));
                add(new Pair<>(anims.get("Flex"), new Timer(1.5f)));
                add(new Pair<>(anims.get("Slide"), new Timer(.75f)));
                add(new Pair<>(anims.get("Stand"), new Timer(3.5f)));
            }};
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
            return new HashMap<>() {{
                put("Jump", new Animation(atlas.findRegion("Jump"), 1, 2, .15f, false));
                put("JumpFreeze", new Animation(atlas.findRegion("JumpFreeze"), 1, 4, .15f, false));
                put("Run", new Animation(atlas.findRegion("Jump"), 1, 4, .15f, true));
                put("Stand", new Animation(atlas.findRegion("Stand"), 1, 2, Array.with(1.25f, .15f), true));
                put("StandFreeze", new Animation(atlas.findRegion("StandFreeze"), 1, 3, .15f, true));
                put("StandShoot", new Animation(atlas.findRegion("StandShoot")));
            }};
        }

        @Override
        public Queue<Pair<Animation, Timer>> getIntroAnimsQ(TextureAtlas textureAtlas) {
            Map<String, Animation> anims = getAnims(textureAtlas);
            return new LinkedList<>() {{
                add(new Pair<>(anims.get("Jump"), new Timer(ConstVals.BOSS_DROP_DOWN_DURATION)));
                add(new Pair<>(anims.get("StandShoot"), new Timer(.15f)));
                add(new Pair<>(anims.get("Stand"), new Timer(1.6f)));
                add(new Pair<>(anims.get("StandFreeze"), new Timer(2.7f)));
                add(new Pair<>(anims.get("Stand"), new Timer(2.5f)));
            }};
        }
    },
    RODENT_MAN("Rodent Man", Level.RODENT_MAN, Position.BOTTOM_CENTER, TextureAsset.RODENT_MAN) {
        @Override
        public Vector2 getSpriteSize() {
            return new Vector2(2.5f, 2f);
        }

        @Override
        public Map<String, Animation> getAnims(TextureAtlas textureAtlas) {
            return new HashMap<>() {{
                put("Jump", new Animation(textureAtlas.findRegion("Jump"), 1, 4, .15f, true));
                put("Run", new Animation(textureAtlas.findRegion("Run"), 1, 4, .15f, true));
                put("Shoot", new Animation(textureAtlas.findRegion("Shoot"), 1, 3, .15f, false));
                put("Slash", new Animation(textureAtlas.findRegion("Slash"), 1, 2, .15f, false));
                put("Stand", new Animation(textureAtlas.findRegion("Stand"), 1, 6, .15f, true));
                put("WallSlide", new Animation(textureAtlas.findRegion("WallSlide"), 1, 2, .15f, true));
            }};
        }

        @Override
        public Queue<Pair<Animation, Timer>> getIntroAnimsQ(TextureAtlas textureAtlas) {
            Map<String, Animation> anims = getAnims(textureAtlas);
            return new LinkedList<>() {{
                add(new Pair<>(anims.get("Jump"), new Timer(ConstVals.BOSS_DROP_DOWN_DURATION)));
                add(new Pair<>(anims.get("Stand"), new Timer(2f)));
                add(new Pair<>(anims.get("Run"), new Timer(.6f)));
                add(new Pair<>(anims.get("Slash"), new Timer(.45f)));
                add(new Pair<>(anims.get("Slash"), new Timer(.45f)));
                add(new Pair<>(anims.get("Stand"), new Timer(3f)));
            }};
        }
    },
    MICROWAVE_MAN("Microwave Man", Level.TEST1, Position.BOTTOM_RIGHT, TextureAsset.MICROWAVE_MAN) {
        @Override
        public Vector2 getSpriteSize() {
            return new Vector2(2.85f, 2.5f);
        }

        @Override
        public Map<String, Animation> getAnims(TextureAtlas textureAtlas) {
            return new HashMap<>() {{
                put("HeadlessJump", new Animation(textureAtlas.findRegion("HeadlessJump")));
                put("HeadlessOpenDoor", new Animation(textureAtlas.findRegion("HeadlessOpenDoor")));
                put("HeadlessShoot", new Animation(textureAtlas.findRegion("HeadlessShoot")));
                put("HeadlessStand", new Animation(textureAtlas.findRegion("HeadlessStand")));
                put("Jump", new Animation(textureAtlas.findRegion("Jump")));
                put("OpenDoor", new Animation(textureAtlas.findRegion("OpenDoor")));
                put("Shoot", new Animation(textureAtlas.findRegion("Shoot")));
                put("Stand", new Animation(textureAtlas.findRegion("Stand"), 1, 2, Array.with(1.5f, .15f), true));
            }};
        }

        @Override
        public Queue<Pair<Animation, Timer>> getIntroAnimsQ(TextureAtlas textureAtlas) {
            Map<String, Animation> anims = getAnims(textureAtlas);
            return new LinkedList<>() {{
                add(new Pair<>(anims.get("Jump"), new Timer(ConstVals.BOSS_DROP_DOWN_DURATION)));
                add(new Pair<>(anims.get("Stand"), new Timer(1.5f)));
                add(new Pair<>(anims.get("Shoot"), new Timer(1.5f)));
                add(new Pair<>(anims.get("OpenDoor"), new Timer(2.5f)));
                add(new Pair<>(anims.get("Stand"), new Timer(1.25f)));
            }};
        }
    };

    public final String name;
    public final Level level;
    public final Position position;
    public final TextureAsset ass;

    public abstract Vector2 getSpriteSize();

    public abstract Map<String, Animation> getAnims(TextureAtlas textureAtlas);

    public abstract Queue<Pair<Animation, Timer>> getIntroAnimsQ(TextureAtlas textureAtlas);

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

}
