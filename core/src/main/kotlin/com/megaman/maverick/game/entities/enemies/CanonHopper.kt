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
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
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
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.IFreezerEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class CanonHopper(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IFaceable, IFreezableEntity {

    companion object {
        const val TAG = "CanonHopper"

        private const val STAND_DUR = 1f
        private const val SHOOT_TIME = 0.5f

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f

        private const val BULLET_SPEED = 10f

        private const val JUMP_X = 5f
        private const val JUMP_Y = 8f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class CanonHopperState { STAND, HOP }

    override lateinit var facing: Facing

    override var frozen: Boolean
        get() = !frozenTimer.isFinished()
        set(value) {
            if (value) frozenTimer.reset() else frozenTimer.setToEnd()
        }
    private val frozenTimer = Timer(1f)

    private val loop = Loop(CanonHopperState.entries.toGdxArray())
    private val currentState: CanonHopperState
        get() = loop.getCurrent()

    private val standTimer = Timer(STAND_DUR).addRunnables(TimeMarkedRunnable(SHOOT_TIME) { shoot() })

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            CanonHopperState.entries.forEach { state ->
                val key = state.name.lowercase()
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
            regions.put("frozen", atlas.findRegion("$TAG/frozen"))
        }
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        loop.reset()
        standTimer.reset()

        frozen = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun takeDamageFrom(damager: IDamager): Boolean {
        val damaged = super.takeDamageFrom(damager)
        if (damaged && damager is IFreezerEntity) frozen = true
        return damaged
    }

    private fun shoot() {
        val spawn = body.getCenter().add(0.75f * ConstVals.PPM * facing.value, 0.25f * ConstVals.PPM)

        val trajectory = GameObjectPools.fetch(Vector2::class).set(BULLET_SPEED * ConstVals.PPM * facing.value, 0f)

        val bullet = MegaEntityFactory.fetch(Bullet::class)!!
        bullet.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory
            )
        )

        requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
    }

    private fun jump() =
        body.physics.velocity.set(JUMP_X * ConstVals.PPM * facing.value, JUMP_Y * ConstVals.PPM)

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            frozenTimer.update(delta)
            if (frozen) {
                body.physics.velocity.x = 0f
                return@add
            }
            if (frozenTimer.isJustFinished()) IceShard.spawn5(body.getCenter())

            when (currentState) {
                CanonHopperState.STAND -> {
                    facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

                    standTimer.update(delta)

                    if (standTimer.isFinished()) {
                        jump()
                        loop.next()
                        standTimer.reset()
                    }
                }
                CanonHopperState.HOP ->
                    if (body.isSensing(BodySense.FEET_ON_GROUND) && body.physics.velocity.y <= 0f) loop.next()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
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
            val region = if (frozen) regions["frozen"] else regions[currentState.name.lowercase()]
            sprite.setRegion(region)

            sprite.hidden = damageBlink

            sprite.setFlip(isFacing(Facing.LEFT), false)

            val position = when {
                frozen || currentState == CanonHopperState.STAND -> Position.BOTTOM_CENTER
                else -> Position.CENTER
            }
            sprite.setPosition(body.getPositionPoint(position), position)
        }
        .build()
}
