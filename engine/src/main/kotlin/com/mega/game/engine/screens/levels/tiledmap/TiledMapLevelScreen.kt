package com.mega.game.engine.screens.levels.tiledmap

import com.badlogic.gdx.graphics.g2d.Batch
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.screens.BaseScreen
import com.mega.game.engine.screens.levels.tiledmap.builders.TiledMapLayerBuilders

abstract class TiledMapLevelScreen(val batch: Batch, var mapLoader: ITiledMapLoader, props: Properties = props()) :
    BaseScreen(props) {

    protected var tiledMapLoadResult: TiledMapLoadResult? = null
        private set
    protected var tiledMapLevelRenderer: TiledMapLevelRenderer? = null
        private set

    protected abstract fun getLayerBuilders(): TiledMapLayerBuilders

    protected abstract fun buildLevel(result: Properties)

    open fun start(tmxMapSource: String) {
        tiledMapLoadResult = mapLoader.load(tmxMapSource)

        val returnProps = Properties()
        val layerBuilders = getLayerBuilders()
        layerBuilders.build(tiledMapLoadResult!!.map.layers, returnProps)
        buildLevel(returnProps)

        tiledMapLevelRenderer = TiledMapLevelRenderer(tiledMapLoadResult!!.map, batch)
    }
}
