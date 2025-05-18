package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.enemies.PreciousCube
import com.megaman.maverick.game.entities.enemies.PreciousCube.PreciousCubeColor
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint

class PreciousCubeChute(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity {

    companion object {
        const val TAG = "PreciousCubeChute"
        private const val MAX_CUBES = 2
        private const val MIN_SPAWN_CUBE_DELAY = 2f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private val chutePosition = Vector2()
    private val spawnCubePosition = Vector2()
    private val spawnCubeDelay = Timer(MIN_SPAWN_CUBE_DELAY)

    private val colorLoop = Loop(PreciousCubeColor.entries.toGdxArray())

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            gdxArrayOf("top", "bottom").forEach { key ->
                val region = atlas.findRegion("$TAG/$key")
                if (region == null) throw IllegalStateException("Region is null: $key")
                regions.put(key, region)
            }
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        spawnCubePosition.set(bounds.getCenter())
        chutePosition.set(bounds.getPositionPoint(Position.TOP_CENTER))
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (canSpawnNewCube()) {
            spawnCubeDelay.update(delta)
            if (spawnCubeDelay.isFinished()) {
                val cube = MegaEntityFactory.fetch(PreciousCube::class)!!
                cube.spawn(props(ConstKeys.POSITION pairTo spawnCubePosition, ConstKeys.COLOR pairTo colorLoop.next()))

                spawnCubeDelay.reset()
            }
        }
    })

    private fun canSpawnNewCube() = MegaGameEntities.getOfTag(PreciousCube.TAG).size < MAX_CUBES

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            "top", GameSprite(regions["top"], DrawingPriority(DrawingSection.PLAYGROUND, 5))
                .also { sprite -> sprite.setSize(3f * ConstVals.PPM, ConstVals.PPM.toFloat()) }
        )
        .updatable { _, sprite -> sprite.setPosition(chutePosition, Position.TOP_CENTER) }
        .sprite(
            "bottom", GameSprite(regions["bottom"], DrawingPriority(DrawingSection.PLAYGROUND, -1))
                .also { sprite -> sprite.setSize(3f * ConstVals.PPM, ConstVals.PPM.toFloat()) }
        )
        .updatable { _, sprite -> sprite.setPosition(chutePosition, Position.TOP_CENTER) }
        .build()

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
