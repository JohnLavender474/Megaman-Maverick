package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.getCenter

class FallingLeaf(game: MegamanMaverickGame) : MegaGameEntity(game), ICullableEntity, ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "FallingLeaf"
        private var region: TextureRegion? = null
        private const val DEFAULT_MIN_TRAJECTORY_X = -0.75f
        private const val DEFAULT_MAX_TRAJECTORY_X = -3f
        private const val DEFAULT_MIN_TRAJECTORY_Y = -0.25f
        private const val DEFAULT_MAX_TRAJECTORY_Y = -3f
        private const val DEFAULT_MIN_ELAPSE_DURATION = 0.5f
        private const val DEFAULT_MAX_ELAPSE_DURATION = 2f
        private const val FADE_OUT_TIME = 1f
        private const val MIN_Y_OFFSET = 20f
        private const val MAX_SPAWNED_ALLOWED = 20
    }

    private val spawnPosition = Vector2()
    private val currentPosition = Vector2()

    private val currentTrajectory = Vector2()
    private val minTrajectory = Vector2()
    private val maxTrajectory = Vector2()

    private val elapseTimer = Timer()

    private val fadeTimer = Timer(FADE_OUT_TIME)
    private var fading = false

    private var minElapseDuration = DEFAULT_MIN_ELAPSE_DURATION
    private var maxElapseDuration = DEFAULT_MAX_ELAPSE_DURATION

    private var minY = 0f

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.ENVIRONS_1.source, "Wood/$TAG")
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineDrawableShapesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val x = spawnProps.get(ConstKeys.X, Float::class)!!
        val y = spawnProps.get(ConstKeys.Y, Float::class)!!
        currentPosition.set(spawnPosition.set(x, y))

        val minYOffset =
            spawnProps.getOrDefault("${ConstKeys.MIN}_${ConstKeys.Y}_${ConstKeys.OFFSET}", MIN_Y_OFFSET, Float::class)
        minY = spawnPosition.y - minYOffset * ConstVals.PPM

        val minTrajX = spawnProps.getOrDefault(
            "${ConstKeys.MIN}_${ConstKeys.TRAJECTORY}_${ConstKeys.X}",
            DEFAULT_MIN_TRAJECTORY_X, Float::class
        )
        val minTrajY = spawnProps.getOrDefault(
            "${ConstKeys.MIN}_${ConstKeys.TRAJECTORY}_${ConstKeys.Y}",
            DEFAULT_MIN_TRAJECTORY_Y, Float::class
        )
        minTrajectory.set(minTrajX, minTrajY)

        val maxX = spawnProps.getOrDefault(
            "${ConstKeys.MAX}_${ConstKeys.TRAJECTORY}_${ConstKeys.X}",
            DEFAULT_MAX_TRAJECTORY_X, Float::class
        )
        val maxY = spawnProps.getOrDefault(
            "${ConstKeys.MAX}_${ConstKeys.TRAJECTORY}_${ConstKeys.Y}",
            DEFAULT_MAX_TRAJECTORY_Y, Float::class
        )
        maxTrajectory.set(maxX, maxY)

        currentTrajectory.set(getRandom(minTrajectory.x, maxTrajectory.x), getRandom(minTrajectory.y, maxTrajectory.y))

        minElapseDuration =
            spawnProps.getOrDefault("${ConstKeys.MIN}_${ConstKeys.ELAPSE}", DEFAULT_MIN_ELAPSE_DURATION, Float::class)
        maxElapseDuration =
            spawnProps.getOrDefault("${ConstKeys.MAX}_${ConstKeys.ELAPSE}", DEFAULT_MAX_ELAPSE_DURATION, Float::class)
        elapseTimer.resetDuration(getRandom(minElapseDuration, maxElapseDuration))

        fadeTimer.reset()
        fading = false

        val setOfAllLeaves = MegaGameEntities.getOfTag(getTag())
        GameLogger.debug(
            TAG,
            "onSpawn(): " +
                "setOfAllLeaves.size=${setOfAllLeaves.size}, " +
                "megaman.body.center=${megaman.body.getCenter()}"
        )

        if (setOfAllLeaves.size > MAX_SPAWNED_ALLOWED) {
            val iter = setOfAllLeaves.iterator()
            while (iter.hasNext) {
                val leaf = iter.next()
                if (leaf is FallingLeaf && !leaf.isFadingOut()) {
                    leaf.fadeOutToDestroy()
                    break
                }
            }
        }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    fun fadeOutToDestroy() {
        fading = true
        fadeTimer.reset()
    }

    fun isFadingOut() = fading

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this, objectSetOf(EventType.BEGIN_ROOM_TRANS)
            )
        )
    )

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        elapseTimer.update(delta)
        if (elapseTimer.isFinished()) {
            val duration = getRandom(minElapseDuration, maxElapseDuration)
            elapseTimer.resetDuration(duration)

            val trajX = getRandom(minTrajectory.x, maxTrajectory.x)
            val trajY = getRandom(minTrajectory.y, maxTrajectory.y)
            currentTrajectory.set(trajX, trajY)
        }

        currentPosition.add(currentTrajectory.cpy().scl(delta * ConstVals.PPM))

        if (fading) {
            fadeTimer.update(delta)
            if (fadeTimer.isFinished()) destroy()
        }

        if (currentPosition.y < minY) destroy()
    })

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 1))
        sprite.setSize(4f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setCenter(currentPosition)
            val alpha = when {
                fading -> 1f - fadeTimer.getRatio()
                else -> 1f
            }
            sprite.setAlpha(alpha)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 10, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    private fun defineDrawableShapesComponent(): DrawableShapesComponent {
        val shapes = Array<() -> IDrawableShape?>()
        shapes.add { defaultSprite.boundingRectangle.toGameRectangle() }
        return DrawableShapesComponent(debugShapeSuppliers = shapes, debug = true)
    }

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}
