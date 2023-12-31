package com.megaman.maverick.game.drawables.sprites;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.engine.common.interfaces.Updatable;
import com.engine.drawables.IDrawable;
import com.megaman.maverick.game.ConstKeys;

public class Background implements Updatable, IDrawable<Batch> {

    protected final Sprite[][] backgroundSprites;
    protected final Sprite backgroundModel;
    protected final float startX;
    protected final float startY;
    protected final float height;
    protected final float width;
    protected final int rows;
    protected final int cols;

    public Background(TextureRegion textureRegion, RectangleMapObject backgroundObj) {
        this(textureRegion, backgroundObj.getRectangle().x, backgroundObj.getRectangle().y,
                backgroundObj.getRectangle().width, backgroundObj.getRectangle().height,
                backgroundObj.getProperties().get(ConstKeys.ROWS, Integer.class),
                backgroundObj.getProperties().get(ConstKeys.COLUMNS, Integer.class));
    }

    public Background(TextureRegion textureRegion, float startX, float startY,
                      float width, float height, int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
        this.backgroundModel = new Sprite(textureRegion);
        this.backgroundModel.setBounds(startX, startY, width, height);
        this.backgroundSprites = new Sprite[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                backgroundSprites[i][j] = new Sprite(backgroundModel);
            }
        }
        resetPositions();
    }

    public void resetPositions() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                float x = startX + j * width;
                float y = startY + i * height;
                backgroundSprites[i][j].setPosition(x, y);
            }
        }
    }

    public void translate(float x, float y) {
        for (Sprite[] row : backgroundSprites) {
            for (Sprite sprite : row) {
                sprite.translate(x, y);
            }
        }
    }

    @Override
    public void update(float delta) {
    }

    @Override
    public void draw(Batch batch) {
        for (Sprite[] row : backgroundSprites) {
            for (Sprite sprite : row) {
                sprite.draw(batch);
            }
        }
    }

}

