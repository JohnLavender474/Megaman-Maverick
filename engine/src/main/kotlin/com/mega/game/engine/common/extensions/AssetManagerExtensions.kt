package com.mega.game.engine.common.extensions

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g2d.TextureAtlas

fun AssetManager.getTextureAtlas(atlas: String) = get(atlas, TextureAtlas::class.java)!!

fun AssetManager.getTextureRegion(atlas: String, region: String) = getTextureAtlas(atlas).findRegion(region)!!

fun AssetManager.getSound(sound: String): Sound {
    return get(sound, Sound::class.java)
}

fun AssetManager.getMusic(music: String): Music {
    return get(music, Music::class.java)
}
