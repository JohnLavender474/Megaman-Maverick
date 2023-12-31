package com.megaman.maverick.game.screens.menus.bosses;


import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.engine.animations.Animation;
import com.engine.common.time.Timer;
import com.engine.drawables.fonts.BitmapFontHandle;
import com.engine.screens.BaseScreen;
import com.megaman.maverick.game.ConstVals;
import com.megaman.maverick.game.MegamanMaverickGame;
import com.megaman.maverick.game.assets.MusicAsset;
import com.megaman.maverick.game.assets.SoundAsset;
import com.megaman.maverick.game.assets.TextureAsset;
import com.megaman.maverick.game.drawables.sprites.Stars;
import com.megaman.maverick.game.entities.bosses.BossType;
import com.megaman.maverick.game.utils.ExtensionsKt;
import kotlin.Pair;

import java.util.LinkedList;
import java.util.Queue;

import static com.engine.common.objects.PropertiesKt.props;

@SuppressWarnings("ALL")
public class BossIntroScreen extends BaseScreen {

    private static final float DUR = 7f;
    private static final float B_DROP = .25f;
    private static final float B_LET_DELAY = 1f;
    private static final float B_LET_DUR = .2f;
    private static final int STARS_N_BARS = 4;

    private final MegamanMaverickGame game;
    private final Camera uiCam;
    private final Timer bLettersDelay;
    private final Timer bLettersTimer;
    private final Timer durTimer;
    private final Timer bDropTimer;
    private final Animation barAnim;
    private final Array<Sprite> bars;
    private final Array<Stars> stars;
    private final BitmapFontHandle bText;

    private BossType b;
    private Queue<Runnable> bLettersAnimQ;
    private Pair<Sprite, Queue<Pair<Animation, Timer>>> currBAnim;

    public BossIntroScreen(MegamanMaverickGame game) {
        super(game, props());
        this.game = game;
        this.uiCam = game.getUiCamera();
        bLettersDelay = new Timer(B_LET_DELAY);
        bLettersTimer = new Timer(B_LET_DUR);
        TextureRegion barReg = game.getAssMan().get(TextureAsset.UI_1.getSource(), TextureAtlas.class).findRegion(
                "Bar");
        barAnim = new Animation(barReg, 1, 4, Array.with(.3f, .15f, .15f, .15f), true);
        bars = new Array<>() {{
            for (int i = 0; i < STARS_N_BARS; i++) {
                Sprite bar = new Sprite();
                bar.setBounds(
                        (i * ConstVals.VIEW_WIDTH * ConstVals.PPM / 3f) - 5f,
                        ConstVals.VIEW_HEIGHT * ConstVals.PPM / 3f,
                        (ConstVals.VIEW_WIDTH * ConstVals.PPM / 3f) + 5f,
                        ConstVals.VIEW_HEIGHT * ConstVals.PPM / 3f);
                add(bar);
            }
        }};
        stars = new Array<>() {{
            for (int i = 0; i < STARS_N_BARS; i++) {
                add(new Stars(game, 0f, i * ConstVals.PPM * ConstVals.VIEW_HEIGHT / 4f));
            }
        }};
        durTimer = new Timer(DUR);
        bDropTimer = new Timer(B_DROP);
        bText = new BitmapFontHandle(() -> "", ExtensionsKt.getDefaultFontSize(), new Vector2(
                (ConstVals.VIEW_WIDTH * ConstVals.PPM / 3f) - ConstVals.PPM,
                ConstVals.VIEW_HEIGHT * ConstVals.PPM / 3f), false, false, "Megaman10Font.ttf");
    }

    public void set(BossType b) {
        this.b = b;
        Sprite s = new Sprite();
        Vector2 size = b.getSpriteSize();
        s.setSize(size.x * ConstVals.PPM, size.y * ConstVals.PPM);
        currBAnim = new Pair<>(s, b.getIntroAnimsQ(game.assMan.get(b.ass.getSource(), TextureAtlas.class)));
        bLettersAnimQ = new LinkedList<>();
        for (int i = 0; i < b.name.length(); i++) {
            final int finalI = i;
            bLettersAnimQ.add(() -> {
                bText.setTextSupplier(() -> b.name.substring(0, finalI + 1));
                if (Character.isWhitespace(b.name.charAt(finalI))) {
                    return;
                }
                game.getAudioMan().playSound(SoundAsset.THUMP_SOUND, false);
            });
        }
    }

    @Override
    public void show() {
        bText.setTextSupplier(() -> "");
        durTimer.reset();
        bDropTimer.reset();
        bLettersTimer.reset();
        bLettersDelay.reset();
        for (int i = 0; i < stars.size; i++) {
            stars.set(i, new Stars(game, 0f, i * ConstVals.PPM * ConstVals.VIEW_HEIGHT / 4f));
        }
        currBAnim.component1().setPosition(((ConstVals.VIEW_WIDTH / 2f) - 1.5f) * ConstVals.PPM,
                ConstVals.VIEW_HEIGHT * ConstVals.PPM);
        for (Pair<Animation, Timer> e : currBAnim.component2()) {
            e.component1().reset();
            e.component2().reset();
        }
        uiCam.position.set(ExtensionsKt.getDefaultCameraPosition());
        game.getAudioMan().playMusic(MusicAsset.MM2_BOSS_INTRO_MUSIC, false);
    }

    @Override
    public void render(float delta) {
        if (durTimer.isFinished()) {
            game.startLevelScreen(b.level);
            return;
        }
        Sprite bSprite = currBAnim.component1();
        if (!game.getPaused()) {
            durTimer.update(delta);
            for (Stars s : stars) {
                s.update(delta);
            }
            barAnim.update(delta);
            for (Sprite b : bars) {
                b.setRegion(barAnim.getCurrentRegion());
            }
            bDropTimer.update(delta);
            if (!bDropTimer.isFinished()) {
                bSprite.setY((ConstVals.VIEW_HEIGHT * ConstVals.PPM) -
                        (((ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f) + .85f * ConstVals.PPM) * bDropTimer.getRatio()));
            }
            if (bDropTimer.isJustFinished()) {
                bSprite.setY((ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f) - .85f * ConstVals.PPM);
            }
            bLettersDelay.update(delta);
            if (bLettersDelay.isFinished() && bDropTimer.isFinished() && !bLettersAnimQ.isEmpty()) {
                bLettersTimer.update(delta);
                if (bLettersTimer.isFinished()) {
                    bLettersAnimQ.poll().run();
                    bLettersTimer.reset();
                }
            }
            Queue<Pair<Animation, Timer>> bAnimQ = currBAnim.component2();
            assert bAnimQ.peek() != null;
            Timer t = bAnimQ.peek().component2();
            if (bAnimQ.size() > 1 && t.isFinished()) {
                bAnimQ.peek().component1().reset();
                bAnimQ.poll();
            }
            t.update(delta);
            Animation bAnim = bAnimQ.peek().component1();
            bAnim.update(delta);
            bSprite.setRegion(bAnim.getCurrentRegion());
        }
        SpriteBatch batch = game.getBatch();
        batch.setProjectionMatrix(uiCam.combined);
        batch.begin();
        for (Stars s : stars) {
            s.draw(batch);
        }
        for (Sprite b : bars) {
            b.draw(batch);
        }
        bSprite.draw(batch);
        bText.draw(batch);
        batch.end();
    }


    @Override
    public void pause() {
        game.getAudioMan().pauseAllSound();
        game.getAudioMan().pauseMusic(null);
    }

    @Override
    public void resume() {
        game.getAudioMan().resumeAllSound();
        game.getAudioMan().playMusic(null, true);
    }

    @Override
    public void dispose() {
        game.getAudioMan().stopMusic(null);
    }

}

