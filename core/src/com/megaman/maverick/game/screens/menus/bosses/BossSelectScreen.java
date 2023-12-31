package com.megaman.maverick.game.screens.menus.bosses;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.engine.animations.Animation;
import com.engine.common.enums.Direction;
import com.engine.common.enums.Position;
import com.engine.common.time.TimeMarkedRunnable;
import com.engine.common.time.Timer;
import com.engine.drawables.fonts.BitmapFontHandle;
import com.engine.screens.menus.IMenuButton;
import com.megaman.maverick.game.ConstVals;
import com.megaman.maverick.game.MegamanMaverickGame;
import com.megaman.maverick.game.assets.MusicAsset;
import com.megaman.maverick.game.assets.SoundAsset;
import com.megaman.maverick.game.assets.TextureAsset;
import com.megaman.maverick.game.entities.bosses.BossType;
import com.megaman.maverick.game.screens.ScreenEnum;
import com.megaman.maverick.game.screens.menus.AbstractMenuScreen;
import com.megaman.maverick.game.screens.utils.BlinkingArrow;
import com.megaman.maverick.game.screens.utils.ScreenSlide;
import com.megaman.maverick.game.utils.ExtensionsKt;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public class BossSelectScreen extends AbstractMenuScreen {

    private static final Vector3 INTRO_BLOCKS_TRANS = new Vector3(15f * ConstVals.PPM, 0f, 0f);
    private static final String MEGA_MAN = "MEGA MAN";
    private static final String BACK = "BACK";

    private static ObjectSet<String> bNameSet;

    private final BitmapFontHandle bName;
    private final ScreenSlide slide;
    private final Sprite bar1;
    private final Sprite bar2;
    private final Sprite white;
    private final Timer outTimer;
    private final Array<BitmapFontHandle> t;
    private final Array<BossPane> bp;
    private final Array<Sprite> bkgd;
    private final ObjectMap<Sprite, Animation> bars;
    private final ObjectMap<String, BlinkingArrow> bArrs;

    private boolean outro;
    private boolean blink;
    private BossType bSelect;

    private ObjectMap<String, IMenuButton> _menuButtons;

    public BossSelectScreen(MegamanMaverickGame game) {
        super(game, MEGA_MAN);
        if (bNameSet == null) {
            bNameSet = new ObjectSet<>();
            for (BossType b : BossType.values()) {
                bNameSet.add(b.name);
            }
        }
        bar1 = new Sprite();
        bar2 = new Sprite();
        white = new Sprite();
        t = new Array<>();
        bp = new Array<>();
        bkgd = new Array<>();
        bars = new ObjectMap<>();
        bArrs = new ObjectMap<>();
        outTimer = new Timer(1.05f, new Array<>() {{
            for (int i = 1; i <= 10; i++) {
                add(new TimeMarkedRunnable(.1f * i, () -> {
                    blink = !blink;
                    return null;
                }));
            }
        }});
        slide = new ScreenSlide(getCastGame().getUiCamera(), INTRO_BLOCKS_TRANS,
                ExtensionsKt.getDefaultCameraPosition().sub(INTRO_BLOCKS_TRANS),
                ExtensionsKt.getDefaultCameraPosition(), .5f, false);
        TextureAtlas megamanFacesAtlas = getCastGame().assMan.get(TextureAsset.FACES_1.getSource(), TextureAtlas.class);
        Map<Position, TextureRegion> megamanFaces = new EnumMap<>(Position.class);
        for (Position position : Position.values()) {

            // TODO: Maverick instead of Megaman faces?
            TextureRegion faceRegion = megamanFacesAtlas.findRegion("Maverick/" + position.name());
            megamanFaces.put(position, faceRegion);

        }
        Supplier<TextureRegion> megamanFaceSupplier = () -> {
            BossType boss = BossType.findByName(getCurrentButtonKey());
            if (boss == null) {
                return megamanFaces.get(Position.CENTER);
            }
            return megamanFaces.get(boss.position);
        };
        BossPane megamanPane = new BossPane(getCastGame(), megamanFaceSupplier, MEGA_MAN, Position.CENTER);
        bp.add(megamanPane);
        for (BossType boss : BossType.values()) {
            bp.add(new BossPane(getCastGame(), boss));
        }
        t.add(new BitmapFontHandle(() -> "PRESS START", ExtensionsKt.getDefaultFontSize(),
                new Vector2(5.35f * ConstVals.PPM, 13.85f * ConstVals.PPM), true, true, "Megaman10Font.ttf"));
        t.add(new BitmapFontHandle(() -> BACK, ExtensionsKt.getDefaultFontSize(), new Vector2(12.35f * ConstVals.PPM,
                ConstVals.PPM), true, true, "Megaman10Font.ttf"));
        bArrs.put(BACK, new BlinkingArrow(getCastGame().assMan, new Vector2(12f * ConstVals.PPM,
                .75f * ConstVals.PPM)));
        TextureAtlas stageSelectAtlas = getCastGame().assMan.get(TextureAsset.UI_1.getSource(), TextureAtlas.class);
        TextureRegion bar = stageSelectAtlas.findRegion("Bar");
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 3; j++) {
                Sprite sprite = new Sprite(bar);
                sprite.setBounds(i * 3f * ConstVals.PPM, (j * 4f * ConstVals.PPM) + 1.35f * ConstVals.PPM,
                        5.33f * ConstVals.PPM, 4f * ConstVals.PPM);
                Animation timedAnimation = new Animation(bar, 1, 4, Array.with(.3f, .15f, .15f, .15f), true);
                bars.put(sprite, timedAnimation);
            }
        }
        TextureAtlas colorsAtlas = getCastGame().assMan.get(TextureAsset.COLORS.getSource(), TextureAtlas.class);
        TextureRegion whiteReg = colorsAtlas.findRegion("White");
        white.setRegion(whiteReg);
        white.setBounds(0f, 0f, ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM);
        TextureRegion black = colorsAtlas.findRegion("Black");
        bar1.setRegion(black);
        bar1.setBounds(-ConstVals.PPM, -ConstVals.PPM, (2f + ConstVals.VIEW_WIDTH) * ConstVals.PPM, 2f * ConstVals.PPM);
        bar2.setRegion(black);
        bar2.setBounds(0f, 0f, .25f * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM);
        TextureAtlas tilesAtlas = getCastGame().assMan.get(TextureAsset.PLATFORMS_1.getSource(), TextureAtlas.class);
        TextureRegion blueBlockRegion = tilesAtlas.findRegion("8bitBlueBlockTransBorder");
        final float halfPPM = ConstVals.PPM / 2f;
        for (int i = 0; i < ConstVals.VIEW_WIDTH; i++) {
            for (int j = 0; j < ConstVals.VIEW_HEIGHT - 1; j++) {
                for (int x = 0; x < 2; x++) {
                    for (int y = 0; y < 2; y++) {
                        Sprite blueBlock = new Sprite(blueBlockRegion);
                        blueBlock.setBounds(i * ConstVals.PPM + (x * halfPPM), j * ConstVals.PPM + (y * halfPPM),
                                halfPPM, halfPPM);
                        bkgd.add(blueBlock);
                    }
                }
            }
        }
        bName = new BitmapFontHandle(() -> "", ExtensionsKt.getDefaultFontSize(), new Vector2(ConstVals.PPM,
                ConstVals.PPM), true, true, "Megaman10Font.ttf");
    }

    @Override
    public void show() {
        super.show();
        slide.init();
        outro = false;
        outTimer.reset();
        getCastGame().getAudioMan().playMusic(MusicAsset.STAGE_SELECT_MM3_MUSIC, true);
    }

    @Override
    protected void onAnyMovement() {
        getCastGame().audioMan.playSound(SoundAsset.CURSOR_MOVE_BLOOP_SOUND, false);
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        Batch batch = getCastGame().batch;
        if (!getCastGame().getPaused()) {
            slide.update(delta);
            if (outro) {
                outTimer.update(delta);
            }
            if (outTimer.isFinished()) {
                getCastGame().startLevelScreen(bSelect.level);
                return;
            }
            for (ObjectMap.Entry<Sprite, Animation> e : bars) {
                e.value.update(delta);
                e.key.setRegion(e.value.getCurrentRegion());
            }
            for (BossPane b : bp) {
                if (b.getBossName().equals(getCurrentButtonKey())) {
                    b.setBossPaneStat(getSelectionMade() ? BossPaneStat.HIGHLIGHTED : BossPaneStat.BLINKING);
                } else {
                    b.setBossPaneStat(BossPaneStat.UNHIGHLIGHTED);
                }
                b.update(delta);
            }
            if (bArrs.containsKey(getCurrentButtonKey())) {
                bArrs.get(getCurrentButtonKey()).update(delta);
            }
        }
        batch.setProjectionMatrix(getCastGame().getUiCamera().combined);
        batch.begin();
        if (outro && blink) {
            white.draw(batch);
        }
        for (Sprite b : bkgd) {
            b.draw(batch);
        }
        for (ObjectMap.Entry<Sprite, Animation> e : bars) {
            e.key.draw(batch);
        }
        for (BossPane b : bp) {
            b.draw(batch);
        }
        bar1.draw(batch);
        bar2.draw(batch);
        if (bArrs.containsKey(getCurrentButtonKey())) {
            bArrs.get(getCurrentButtonKey()).draw(batch);
        }
        for (BitmapFontHandle text : t) {
            text.draw(batch);
        }
        if (MEGA_MAN.equals(getCurrentButtonKey()) || bNameSet.contains(getCurrentButtonKey())) {
            bName.setTextSupplier(() -> getCurrentButtonKey().toUpperCase());
            bName.draw(batch);
        }
        batch.end();
    }

    @Override
    protected @NotNull ObjectMap<String, IMenuButton> getMenuButtons() {
        if (_menuButtons != null) {
            return _menuButtons;
        }

        _menuButtons = new ObjectMap<>();
        _menuButtons.put(MEGA_MAN, new IMenuButton() {
            @Override
            public boolean onSelect(float delta) {
                return false;
            }

            @Override
            public String onNavigate(@NotNull Direction direction, float delta) {
                return switch (direction) {
                    case UP -> BossType.findByPos(1, 2).name;
                    case DOWN -> BossType.findByPos(1, 0).name;
                    case LEFT -> BossType.findByPos(0, 1).name;
                    case RIGHT -> BossType.findByPos(2, 1).name;
                };
            }
        });
        _menuButtons.put(BACK, new IMenuButton() {
            @Override
            public boolean onSelect(float delta) {
                getCastGame().setCurrentScreen(ScreenEnum.MAIN.name());
                return true;
            }

            @Override
            public String onNavigate(@NotNull Direction direction, float delta) {
                return switch (direction) {
                    case UP, LEFT, RIGHT -> BossType.findByPos(2, 0).name;
                    case DOWN -> BossType.findByPos(2, 2).name;
                };
            }
        });
        for (BossType boss : BossType.values()) {
            _menuButtons.put(boss.name, new IMenuButton() {
                @Override
                public boolean onSelect(float delta) {
                    getCastGame().audioMan.playSound(SoundAsset.BEAM_OUT_SOUND, false);
                    getCastGame().audioMan.stopMusic(null);
                    bSelect = boss;
                    outro = true;
                    return true;
                }

                @Override
                public String onNavigate(@NotNull Direction direction, float delta) {
                    int x = boss.position.getX();
                    int y = boss.position.getY();
                    switch (direction) {
                        case UP -> y += 1;
                        case DOWN -> y -= 1;
                        case LEFT -> x -= 1;
                        case RIGHT -> x += 1;
                    }
                    if (y < 0 || y > 2) {
                        return BACK;
                    }
                    if (x < 0) {
                        x = 2;
                    }
                    if (x > 2) {
                        x = 0;
                    }
                    Position position = Position.Companion.get(x, y);
                    if (position == null) {
                        throw new IllegalStateException();
                    } else if (position.equals(Position.CENTER)) {
                        return MEGA_MAN;
                    } else {
                        return BossType.findByPos(x, y).name;
                    }
                }
            });
        }
        return _menuButtons;
    }

}

