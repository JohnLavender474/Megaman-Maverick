package com.megaman.maverick.game.drawables.sprites;


import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megaman.maverick.game.ConstVals;
import com.megaman.maverick.game.MegamanMaverickGame;
import com.megaman.maverick.game.assets.TextureAsset;

public class Stars extends Background {

    private static final int ROWS = 1;
    private static final int COLS = 6;
    private static final float DUR = 10f;
    private static final float WIDTH = ConstVals.VIEW_WIDTH / 3f;
    private static final float HEIGHT = ConstVals.VIEW_HEIGHT / 4f;

    private float dist;

    public Stars(MegamanMaverickGame game, float x, float y) {
        super(game.assMan.get(TextureAsset.BACKGROUNDS_1.getSource(), TextureAtlas.class).findRegion("StarFieldBG"),
                x, y, WIDTH * ConstVals.PPM, HEIGHT * ConstVals.PPM, ROWS, COLS);
    }

    @Override
    public void update(float delta) {
        float trans = WIDTH * ConstVals.PPM * delta / DUR;
        translate(-trans, 0f);
        dist += trans;
        if (dist >= WIDTH * ConstVals.PPM) {
            resetPositions();
            dist = 0f;
        }
    }

}
