package com.mega.game.engine.screens.levels.tiledmap

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.maps.MapObjects
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.utils.ObjectMap

data class TiledMapLoadResult(
    val map: TiledMap,
    val layers: ObjectMap<String, MapObjects>,
    val worldWidth: Int,
    val worldHeight: Int,
    val tileWidth: Int,
    val tileHeight: Int
)

interface ITiledMapLoader {

    fun load(tmxSrc: String): TiledMapLoadResult
}

class TiledMapLoader(private val assMan: AssetManager) : ITiledMapLoader {

    override fun load(tmxSrc: String): TiledMapLoadResult {
        val map =
            assMan.get(tmxSrc, TiledMap::class.java) ?: throw IllegalStateException("Failed to load map: $tmxSrc")

        val worldWidth = map.properties["width"] as Int
        val worldHeight = map.properties["height"] as Int
        val tileWidth = map.properties["tilewidth"] as Int
        val tileHeight = map.properties["tileheight"] as Int

        val layers = ObjectMap<String, MapObjects>()
        map.layers.forEach { layer -> layers.put(layer.name, layer.objects) }
        return TiledMapLoadResult(map, layers, worldWidth, worldHeight, tileWidth, tileHeight)
    }
}
