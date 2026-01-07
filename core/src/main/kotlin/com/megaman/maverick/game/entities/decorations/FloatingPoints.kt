package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.add
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

class FloatingPoints(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IDirectional {

    companion object {
        const val TAG = "FloatingPoints"

        private const val INIT_FLOAT_SPEED = 2f
        private const val DEGRADATION = 2f

        private const val PRE_FADE_DUR = 0.5f
        private const val FADE_DUR = 0.5f

        fun spawnFloatingPoints(
            floatingPointsType: FloatingPointsType,
            position: Vector2,
            direction: Direction
        ) {
            val floatingPoints = MegaEntityFactory.fetch(FloatingPoints::class)!!
            floatingPoints.spawn(
                props(
                    ConstKeys.POSITION pairTo position,
                    ConstKeys.DIRECTION pairTo direction,
                    ConstKeys.TYPE pairTo floatingPointsType,
                )
            )
        }

        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class FloatingPointsType(val points: Int? = null) {
        POINTS100(100),
        POINTS200(200),
        POINTS400(400),
        POINTS800(800),
        POINTS1000(1000),
        POINTS2000(2000),
        POINTS4000(4000),
        POINTS8000(8000),
        ONE_UP(null)
    }

    override lateinit var direction: Direction

    private lateinit var type: FloatingPointsType

    private val position = Vector2()

    private val preFadeTimer = Timer(PRE_FADE_DUR)
    private val fadeTimer = Timer(FADE_DUR)

    private var speed = 0f

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SMB3_POINTS.source)
            FloatingPointsType.entries.forEach {
                val key = it.name.lowercase()
                regions.put(key, atlas.findRegion(key))
            }
        }
        super.init()
        addComponent(defineSpritesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        type = spawnProps.get(ConstKeys.TYPE, FloatingPointsType::class)!!

        direction = spawnProps.get(ConstKeys.DIRECTION, Direction::class)!!

        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        this.position.set(position)

        preFadeTimer.reset()
        fadeTimer.reset()

        speed = INIT_FLOAT_SPEED
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (!preFadeTimer.isFinished()) preFadeTimer.update(delta)
        else {
            fadeTimer.update(delta)
            if (fadeTimer.isFinished()) destroy()
        }

        speed = (speed - DEGRADATION * delta).coerceAtLeast(0f)
        position.add(speed, direction)
    })

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
                .also { it.setSize(ConstVals.PPM.toFloat()) })
        .preProcess { _, sprite ->
            val region = regions[type.name.lowercase()]!!
            sprite.setRegion(region)
            sprite.setCenter(position)
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
        }
        .build()

    override fun getType() = EntityType.DECORATION
}
