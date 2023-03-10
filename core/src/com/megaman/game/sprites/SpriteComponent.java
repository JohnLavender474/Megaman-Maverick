package com.megaman.game.sprites;

import com.badlogic.gdx.utils.Array;
import com.megaman.game.Component;

import java.util.Arrays;

public class SpriteComponent implements Component {

    public Array<SpriteHandle> handles = new Array<>();

    public SpriteComponent(SpriteHandle... handles) {
        this(Arrays.asList(handles));
    }

    public SpriteComponent(Iterable<SpriteHandle> handles) {
        for (SpriteHandle h : handles) {
            this.handles.add(h);
        }
    }

}
