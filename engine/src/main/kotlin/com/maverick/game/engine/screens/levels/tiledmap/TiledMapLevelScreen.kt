package com.maverick.game.engine.screens.levels.tiledmap

import com.badlogic.gdx.graphics.g2d.Batch
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.screens.BaseScreen
import com.maverick.game.engine.screens.levels.tiledmap.builders.ITiledMapLayerBuilders

abstract class TiledMapLevelScreen(val batch: Batch, var mapLoader: ITiledMapLoader, props: Properties = props()) :
    BaseScreen(props) {

    protected var tiledMapLoadResult: TiledMapLoadResult? = null
        private set
    protected var tiledMapLevelRenderer: TiledMapLevelRenderer? = null
        private set

    protected abstract fun getLayerBuilders(): ITiledMapLayerBuilders

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
