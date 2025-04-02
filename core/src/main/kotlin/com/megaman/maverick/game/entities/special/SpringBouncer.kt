package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.VelocityAlteration
import com.megaman.maverick.game.utils.VelocityAlterationType
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.world.body.*

class SpringBouncer(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IBodyEntity, IAudioEntity,
    IAnimatedEntity, IDirectional {

    companion object {
        const val TAG = "SpringBouncer"

        private const val BOUNCE_DUR = 0.5f
        private const val BOUNCE_IMPULSE = 25f

        private const val SPRITE_WIDTH = 1f
        private const val SPRITE_HEIGHT = 2f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var direction: Direction

    private val bounces = Array<GamePair<GameRectangle, Timer>>()
    private val outMatrix = Matrix<GameRectangle>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SPECIALS_1.source)
            gdxArrayOf("bounce", "still").forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(SpritesComponent())
        addComponent(AnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        direction = Direction.valueOf(
            spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.UP, String::class).uppercase()
        )

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        defineDrawables(bounds)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        bounces.clear()

        sprites.clear()
        animators.clear()
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.STATIC)

        val bounceFixture = Fixture(body, FixtureType.BOUNCER, GameRectangle())
        bounceFixture.setVelocityAlteration alteration@{ fixture, _, state ->
            if (state == ProcessState.BEGIN && fixture.getEntity() == megaman)
                requestToPlaySound(SoundAsset.DINK_SOUND, false)

            return@alteration bounce(fixture)
        }
        body.addFixture(bounceFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.forEachFixture { fixture ->
                fixture as Fixture
                val bounds = fixture.rawShape as GameRectangle
                bounds.set(body)
            }
        }

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.SHIELD))
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        val bounceIter = bounces.iterator()
        while (bounceIter.hasNext()) {
            val (bounceBounds, bounceTimer) = bounceIter.next()
            bounceTimer.update(delta)
            if (bounceTimer.isFinished()) {
                GameObjectPools.free(bounceBounds)
                bounceIter.remove()
            }
        }
    })

    private fun defineDrawables(bounds: GameRectangle) {
        val cells = bounds.splitByCellSize(ConstVals.PPM.toFloat(), outMatrix)
        for (x in 0 until cells.columns) for (y in 0 until cells.rows) {
            val key = "${x}_$y"

            val sprite = GameSprite()
            sprite.setSize(SPRITE_WIDTH * ConstVals.PPM, SPRITE_HEIGHT * ConstVals.PPM)

            val posX = bounds.getX() + x * ConstVals.PPM
            val posY = bounds.getY() + y * ConstVals.PPM
            sprite.setPosition(posX, posY)
            when (direction) {
                Direction.DOWN -> sprite.translateY(-ConstVals.PPM.toFloat())
                Direction.LEFT -> sprite.translate(0.5f * -ConstVals.PPM, -0.5f * ConstVals.PPM)
                Direction.RIGHT -> sprite.translate(0.5f * ConstVals.PPM, -0.5f * ConstVals.PPM)
                else -> {}
            }

            sprite.setOriginCenter()
            sprite.rotation = when (direction) {
                Direction.UP -> 0f
                Direction.DOWN -> 180f
                Direction.LEFT -> 90f
                Direction.RIGHT -> 270f
            }

            sprites.put(key, sprite)

            val spriteBounds = GameRectangle().setSize(ConstVals.PPM.toFloat()).setPosition(posX, posY)

            val keySupplier: (String?) -> String? = {
                when {
                    bounces.any { (bounceBounds, _) -> spriteBounds.overlaps(bounceBounds) } -> "bounce"
                    else -> "still"
                }
            }
            val animations = objectMapOf<String, IAnimation>(
                "still" pairTo Animation(regions["still"]),
                "bounce" pairTo Animation(regions["bounce"], 1, 5, 0.05f, true)
            )
            val animator = Animator(keySupplier, animations)
            putAnimator(key, sprite, animator)
        }
    }

    private fun bounce(fixture: IFixture): VelocityAlteration {
        val bounceBounds = GameObjectPools.fetch(GameRectangle::class, false)
            .set(fixture.getShape().getBoundingRectangle())
        val bounceTimer = Timer(BOUNCE_DUR)
        bounces.add(bounceBounds pairTo bounceTimer)

        val actionX: VelocityAlterationType
        val actionY: VelocityAlterationType
        if (direction.isVertical()) {
            actionX = VelocityAlterationType.ADD
            actionY = VelocityAlterationType.SET
        } else {
            actionX = VelocityAlterationType.SET
            actionY = VelocityAlterationType.ADD
        }
        val impulse = VelocityAlteration(actionX = actionX, actionY = actionY)
        when (direction) {
            Direction.UP -> impulse.forceY = BOUNCE_IMPULSE * ConstVals.PPM
            Direction.DOWN -> impulse.forceY = -BOUNCE_IMPULSE * ConstVals.PPM
            Direction.LEFT -> impulse.forceX = -BOUNCE_IMPULSE * ConstVals.PPM
            Direction.RIGHT -> impulse.forceX = BOUNCE_IMPULSE * ConstVals.PPM
        }
        if (fixture.getEntity() is Megaman) {
            val controllerPoller = game.controllerPoller
            when {
                (direction == Direction.UP && controllerPoller.isPressed(MegaControllerButton.UP)) ||
                    (direction == Direction.DOWN && controllerPoller.isPressed(MegaControllerButton.DOWN)) ->
                    impulse.forceY *= 2f

                (direction == Direction.LEFT && controllerPoller.isPressed(MegaControllerButton.LEFT)) ||
                    (direction == Direction.RIGHT && controllerPoller.isPressed(MegaControllerButton.RIGHT)) ->
                    impulse.forceX *= 2f
            }
        }
        return impulse
    }

    override fun getType() = EntityType.SPECIAL

    override fun getTag() = TAG
}
