package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class CannonHopper(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IFaceable {

    companion object {
        const val TAG = "CannonHopper"

        private const val STAND_DUR = 1f
        private const val SHOOT_TIME = 0.5f

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f

        private const val BULLET_SPEED = 8f

        private const val JUMP_X = 4f
        private const val JUMP_Y = 8f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var facing: Facing

    private val loop = Loop("stand", "hop")
    private val currentState: String
        get() = loop.getCurrent()
    private val standTimer = Timer(STAND_DUR).addRunnables(TimeMarkedRunnable(SHOOT_TIME) { shoot() })

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            gdxArrayOf("stand", "hop").forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        loop.reset()
        standTimer.reset()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun shoot() {
        val spawn = body.getCenter().add(0.5f * ConstVals.PPM * facing.value, 0.25f * ConstVals.PPM)

        val trajectory = GameObjectPools.fetch(Vector2::class).set(BULLET_SPEED * ConstVals.PPM * facing.value, 0f)

        val bullet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.Companion.BULLET)!!
        bullet.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory,
                ConstKeys.OWNER pairTo this
            )
        )

        requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
    }

    private fun jump() =
        body.physics.velocity.set(JUMP_X * ConstVals.PPM * facing.value, JUMP_Y * ConstVals.PPM)

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (currentState) {
                "stand" -> {
                    facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

                    standTimer.update(delta)
                    if (standTimer.isFinished()) {
                        jump()
                        loop.next()
                        standTimer.reset()
                    }
                }

                "hop" -> if (body.isSensing(BodySense.FEET_ON_GROUND) && body.physics.velocity.y <= 0f) loop.next()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.5f * ConstVals.PPM, ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(1.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(1.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.ORANGE
        debugShapes.add { headFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM

            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK) && body.physics.velocity.y > 0f)
                body.physics.velocity.y = 0f

            if (body.physics.velocity.y <= 0f && body.isSensing(BodySense.FEET_ON_GROUND))
                body.physics.velocity.x = 0f
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.Companion.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            val region = regions[currentState]
            sprite.setRegion(region)
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.hidden = damageBlink
        }
        .build()
}
