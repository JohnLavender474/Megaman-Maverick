package com.mega.game.engine.screens.levels.tiledmap

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile

class TiledMapLevelRenderer(map: TiledMap, batch: Batch) : OrthogonalTiledMapRenderer(map, batch) {

    fun render(camera: OrthographicCamera) {
        setView(camera)
        super.render()
    }

    override fun beginRender() = AnimatedTiledMapTile.updateAnimationBaseTime()

    override fun endRender() {}

    override fun dispose() {}
}
