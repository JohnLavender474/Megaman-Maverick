package com.megaman.game.animations;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.megaman.game.utils.interfaces.Resettable;
import com.megaman.game.utils.interfaces.Updatable;
import com.megaman.game.utils.objs.KeyValuePair;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.fill;

@Getter
public class Animation implements Updatable, Resettable {

    private final Array<TextureRegion> frames = new Array<>();
    private final Array<Float> frameTimes = new Array<>();

    public TextureRegion currRegion;
    public boolean loop = true;

    private float animDur;
    private boolean finished;
    private float timeElapsed;

    public Animation(Animation anim) {
        this(anim, false);
    }

    public Animation(Animation anim, boolean reverse) {
        frames.addAll(anim.frames);
        frameTimes.addAll(anim.frameTimes);
        currRegion = anim.currRegion;
        loop = anim.loop;
        animDur = anim.animDur;
        finished = anim.finished;
        timeElapsed = anim.timeElapsed;
        if (reverse) {
            frames.reverse();
            frameTimes.reverse();
        }
    }

    public Animation(TextureRegion textureRegion) {
        this(textureRegion, 1, 1f);
    }

    public Animation(TextureRegion textureRegion, float[] durations) {
        this(textureRegion, durations, true);
    }

    public Animation(TextureRegion textureRegion, float[] durations, boolean loop) {
        this.loop = loop;
        instantiate(textureRegion, durations);
    }

    public Animation(TextureRegion textureRegion, int numFrames, float duration) {
        this(textureRegion, numFrames, duration, true);
    }

    public Animation(TextureRegion textureRegion, int numFrames, float duration, boolean loop) {
        float[] durations = new float[numFrames];
        fill(durations, duration);
        instantiate(textureRegion, durations);
        this.loop = loop;
    }

    public Animation(List<KeyValuePair<Float, TextureRegion>> frameTimeKeyValuePairs) {
        instantiate(frameTimeKeyValuePairs);
    }

    public static Animation of(TextureRegion textureRegion, List<Float> durations) {
        int width = textureRegion.getRegionWidth() / durations.size();
        int height = textureRegion.getRegionHeight();
        List<KeyValuePair<Float, TextureRegion>> keyValuePairs = new ArrayList<>();
        for (int i = 0; i < durations.size(); i++) {
            keyValuePairs.add(new KeyValuePair<>(durations.get(i), new TextureRegion(textureRegion, width * i, 0,
                    width, height)));
        }
        return new Animation(keyValuePairs);
    }

    private void instantiate(TextureRegion textureRegion, float[] durations) {
        int width = textureRegion.getRegionWidth() / durations.length;
        int height = textureRegion.getRegionHeight();
        List<KeyValuePair<Float, TextureRegion>> frameTimeKeyValuePairs = new ArrayList<>();
        for (int i = 0; i < durations.length; i++) {
            frameTimeKeyValuePairs.add(new KeyValuePair<>(durations[i], new TextureRegion(textureRegion, width * i, 0
                    , width, height)));
        }
        instantiate(frameTimeKeyValuePairs);
    }

    private void instantiate(List<KeyValuePair<Float, TextureRegion>> frameTimeKeyValuePairs) {
        frameTimeKeyValuePairs.forEach(frameTimeKeyValuePair -> {
            TextureRegion t = frameTimeKeyValuePair.value();
            Float f = frameTimeKeyValuePair.key();
            animDur += f;
            frameTimes.add(f);
            frames.add(t);
        });
    }

    @Override
    public void update(float delta) {
        if (!finished) {
            timeElapsed += delta;
        }
        if (timeElapsed > animDur && !loop) {
            timeElapsed = animDur - .00001f;
            finished = true;
        }
        float currentLoopDuration = timeElapsed % animDur;
        int index = 0;
        while (currentLoopDuration > frameTimes.get(index) && index < frames.size) {
            currentLoopDuration -= frameTimes.get(index);
            index++;
        }
        currRegion = frames.get(index);
    }

    @Override
    public void reset() {
        timeElapsed = 0f;
        finished = false;
    }

}
