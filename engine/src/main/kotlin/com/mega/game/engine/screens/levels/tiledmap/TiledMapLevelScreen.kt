package com.mega.game.engine.screens.levels.tiledmap

import com.badlogic.gdx.graphics.g2d.Batch
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.screens.BaseScreen
import com.mega.game.engine.screens.levels.tiledmap.builders.TiledMapLayerBuilders

abstract class TiledMapLevelScreen(
    val batch: Batch,
    var tiledMapLoader: ITiledMapLoader,
    var tmxMapSource: String? = null,
    properties: Properties = props()
) : BaseScreen(properties) {

    protected var tiledMapLoadResult: TiledMapLoadResult? = null
        private set
    protected var tiledMapLevelRenderer: TiledMapLevelRenderer? = null
        private set

    protected abstract fun getLayerBuilders(): TiledMapLayerBuilders

    protected abstract fun buildLevel(result: Properties)

    override fun show() {
        super.show()

        tmxMapSource?.let {
            tiledMapLoadResult = tiledMapLoader.load(it)

            val returnProps = Properties()
            val layerBuilders = getLayerBuilders()
            layerBuilders.build(tiledMapLoadResult!!.map.layers, returnProps)
            buildLevel(returnProps)

            tiledMapLevelRenderer = TiledMapLevelRenderer(tiledMapLoadResult!!.map, batch)
        } ?: throw IllegalStateException("Tmx map source must be set before calling show()")
    }
}
