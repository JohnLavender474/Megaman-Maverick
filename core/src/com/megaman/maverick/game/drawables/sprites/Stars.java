package com.megaman.maverick.game.drawables.sprites;


import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Rectangle;
import com.engine.drawables.sorting.DrawingPriority;
import com.engine.drawables.sorting.DrawingSection;
import com.megaman.maverick.game.ConstKeys;
import com.megaman.maverick.game.ConstVals;
import com.megaman.maverick.game.MegamanMaverickGame;
import com.megaman.maverick.game.assets.TextureAsset;
import kotlin.Pair;

import static com.engine.common.objects.PropertiesKt.props;

@SuppressWarnings("ALL")
public class Stars extends Background {

    private static final int ROWS = 1;
    private static final int COLS = 6;
    private static final float DUR = 10f;
    private static final float WIDTH = ConstVals.VIEW_WIDTH / 3f;
    private static final float HEIGHT = ConstVals.VIEW_HEIGHT / 4f;

    private float dist;

    public Stars(MegamanMaverickGame game, float x, float y) {
        super(game.assMan.get(TextureAsset.BACKGROUNDS_1.getSource(), TextureAtlas.class).findRegion("StarFieldBG"),
                new Rectangle().setPosition(x, y),
                props(new Pair<>(ConstKeys.WIDTH, WIDTH * ConstVals.PPM),
                        new Pair<>(ConstKeys.HEIGHT, HEIGHT * ConstVals.PPM),
                        new Pair<>(ConstKeys.ROWS, ROWS),
                        new Pair<>(ConstKeys.PRIORITY, new DrawingPriority(DrawingSection.FOREGROUND, 1)),
                        new Pair<>(ConstKeys.COLUMNS, COLS)));
    }

    @Override
    public void update(float delta) {
        float trans = WIDTH * ConstVals.PPM * delta / DUR;
        getSpriteMatrix().translate(-trans, 0f);
        dist += trans;
        if (dist >= WIDTH * ConstVals.PPM) {
            getSpriteMatrix().resetPositions();
            dist = 0f;
        }
    }

}

