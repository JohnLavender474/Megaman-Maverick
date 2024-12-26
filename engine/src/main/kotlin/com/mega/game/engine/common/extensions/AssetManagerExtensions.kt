package com.mega.game.engine.common.extensions

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Queue

fun <T> AssetManager.loadAssetsInDirectory(
    directory: String,
    type: Class<T>,
) {
    val queue = Queue<String>()
    queue.addLast(directory)

    while (!queue.isEmpty) {
        val path = queue.removeFirst()
        val file = Gdx.files.internal(path)

        if (file.isDirectory) {
            file.list().forEach { queue.addLast(it.path()) }
        } else {
            load(file.path(), type)
        }
    }
}

fun AssetManager.getTextureAtlas(atlas: String) = get(atlas, TextureAtlas::class.java)!!

fun AssetManager.getTextureRegion(atlas: String, region: String) = getTextureAtlas(atlas).findRegion(region)!!

fun AssetManager.getSound(sound: String): Sound {
    return get(sound, Sound::class.java)
}

fun AssetManager.getMusic(music: String): Music {
    return get(music, Music::class.java)
}
