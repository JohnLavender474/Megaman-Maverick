package com.megaman.maverick.game.screens.menus.bosses;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.engine.animations.Animation;
import com.engine.common.enums.Position;
import com.engine.common.interfaces.Updatable;
import com.engine.drawables.IDrawable;
import com.megaman.maverick.game.ConstVals;
import com.megaman.maverick.game.MegamanMaverickGame;
import com.megaman.maverick.game.assets.TextureAsset;
import com.megaman.maverick.game.entities.bosses.BossType;

import java.util.function.Supplier;

public class BossPane implements Updatable, IDrawable<Batch> {

    public static final float PANE_BOUNDS_WIDTH = 5.33f;
    public static final float PANE_BOUNDS_HEIGHT = 4f;
    public static final float BOTTOM_OFFSET = 1.35f;
    public static final float SPRITE_HEIGHT = 2f;
    public static final float SPRITE_WIDTH = 2.5f;
    public static final float PANE_HEIGHT = 3f;
    public static final float PANE_WIDTH = 4f;

    private final String bossName;
    private final Supplier<TextureRegion> bossRegSupplier;
    private final Sprite bossSprite = new Sprite();
    private final Sprite paneSprite = new Sprite();
    private final Animation paneBlinkingAnim;
    private final Animation paneHighlightedAnim;
    private final Animation paneUnhighlightedAnim;

    private BossPaneStat bossPaneStat = BossPaneStat.UNHIGHLIGHTED;

    public BossPane(MegamanMaverickGame game, BossType boss) {
        this(game, game.getAssMan().get(TextureAsset.FACES_1.getSource(), TextureAtlas.class).findRegion(boss.name),
                boss.name, boss.position);
    }

    public BossPane(MegamanMaverickGame game, TextureRegion bossRegion, String bossName, Position position) {
        this(game, bossRegion, bossName, position.getX(), position.getY());
    }

    public BossPane(MegamanMaverickGame game, TextureRegion bossRegion, String bossName, int x, int y) {
        this(game, () -> bossRegion, bossName, x, y);
    }

    public BossPane(MegamanMaverickGame game2d, Supplier<TextureRegion> bossRegSupplier,
                    String bossName, Position position) {
        this(game2d, bossRegSupplier, bossName, position.getX(), position.getY());
    }

    public BossPane(MegamanMaverickGame game, Supplier<TextureRegion> bossRegSupplier,
                    String bossName, int x, int y) {
        this.bossName = bossName;
        this.bossRegSupplier = bossRegSupplier;
        float centerX =
                (x * PANE_BOUNDS_WIDTH * ConstVals.PPM) + (PANE_BOUNDS_WIDTH * ConstVals.PPM / 2f);
        float centerY = (BOTTOM_OFFSET * ConstVals.PPM + y * PANE_BOUNDS_HEIGHT * ConstVals.PPM) +
                (PANE_BOUNDS_HEIGHT * ConstVals.PPM / 2f);
        bossSprite.setSize(SPRITE_WIDTH * ConstVals.PPM, SPRITE_HEIGHT * ConstVals.PPM);
        bossSprite.setCenter(centerX, centerY);
        paneSprite.setSize(PANE_WIDTH * ConstVals.PPM, PANE_HEIGHT * ConstVals.PPM);
        paneSprite.setCenter(centerX, centerY);
        TextureAtlas decorationAtlas = game.getAssMan().get(TextureAsset.UI_1.getSource(), TextureAtlas.class);
        TextureRegion paneUnhighlighted = decorationAtlas.findRegion("Pane");
        this.paneUnhighlightedAnim = new Animation(paneUnhighlighted);
        TextureRegion paneBlinking = decorationAtlas.findRegion("PaneBlinking");
        this.paneBlinkingAnim = new Animation(paneBlinking, 1, 2, .125f, true);
        TextureRegion paneHighlighted = decorationAtlas.findRegion("PaneHighlighted");
        this.paneHighlightedAnim = new Animation(paneHighlighted);
    }

    @Override
    public void update(float delta) {
        Animation timedAnimation;
        switch (bossPaneStat) {
            case BLINKING -> timedAnimation = paneBlinkingAnim;
            case HIGHLIGHTED -> timedAnimation = paneHighlightedAnim;
            case UNHIGHLIGHTED -> timedAnimation = paneUnhighlightedAnim;
            default -> throw new IllegalStateException();
        }
        timedAnimation.update(delta);
        paneSprite.setRegion(timedAnimation.getCurrentRegion());
    }

    @Override
    public void draw(Batch batch) {
        paneSprite.draw(batch);
        bossSprite.setRegion(bossRegSupplier.get());
        bossSprite.draw(batch);
    }

    public String getBossName() {
        return bossName;
    }

    public Supplier<TextureRegion> getBossRegSupplier() {
        return bossRegSupplier;
    }

    public Sprite getBossSprite() {
        return bossSprite;
    }

    public Sprite getPaneSprite() {
        return paneSprite;
    }

    public Animation getPaneBlinkingAnim() {
        return paneBlinkingAnim;
    }

    public Animation getPaneHighlightedAnim() {
        return paneHighlightedAnim;
    }

    public Animation getPaneUnhighlightedAnim() {
        return paneUnhighlightedAnim;
    }

    public BossPaneStat getBossPaneStat() {
        return bossPaneStat;
    }

    public void setBossPaneStat(BossPaneStat bossPaneStat) {
        this.bossPaneStat = bossPaneStat;
    }

}
