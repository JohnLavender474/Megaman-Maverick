package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.shapes.IGameShape2D
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.facing
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.decorations.Snow
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.entities.state.hasAnyTag
import com.megaman.maverick.game.entities.state.hasTag
import com.megaman.maverick.game.entities.state.mapObjectId
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class FallingIcicle(services: MegaServices) : AbstractProjectile(services), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "FallingIcicle"
        private const val SHAKE_DUR = 0.25f
        private const val FORM_FRAMES = 4
        private const val FORM_FRAME_DUR = 0.15f
        private const val GRAVITY = -22.5f
        private const val SHATTER_DUR = 0.25f
        private const val NORMAL_CLAMP_Y = 10f
        private const val WATER_CLAMP_Y = 3f
        private val BODIES_TO_IGNORE = objectSetOf<String>(Snow.TAG)
        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class FallingIcicleState { FORM, STILL, SHAKE, FALL }

    override lateinit var facing: Facing

    private val formTimer = Timer(FORM_FRAMES * FORM_FRAME_DUR)
    private val stillTimer = Timer()
    private val shakeTimer = Timer(SHAKE_DUR)
    private var timed = false
    private val shatterTimer = Timer(SHATTER_DUR)

    private lateinit var state: FallingIcicleState

    private val ignore = ObjectSet<Int>()

    override fun init(vararg params: Any) {
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.PROJECTILES_2.source)
            gdxArrayOf("form", "still", "shake").forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = when {
            spawnProps.containsKey(ConstKeys.BOUNDS) -> spawnProps
                .get(ConstKeys.BOUNDS, GameRectangle::class)!!
                .getPositionPoint(Position.TOP_CENTER)

            else -> spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        }
        body.setTopCenterToPoint(spawn)

        body.physics.gravityOn = false

        val state = spawnProps.get(ConstKeys.STATE)
        this.state = when (state) {
            is FallingIcicleState -> state
            is String -> FallingIcicleState.valueOf(state.uppercase())
            else -> FallingIcicleState.STILL
        }

        timed = spawnProps.containsKey(ConstKeys.DELAY)
        stillTimer.resetDuration(spawnProps.getOrDefault(ConstKeys.DELAY, 0f, Float::class))
        formTimer.reset()
        shakeTimer.resetDuration(spawnProps.getOrDefault(ConstKeys.DURATION, SHAKE_DUR, Float::class))
        shatterTimer.reset()

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.IGNORE)) {
                val id = (value as RectangleMapObject).properties.get(ConstKeys.ID, Int::class.java)
                ignore.add(id)
            }
        }

        val facing = spawnProps.get(ConstKeys.FACING)
        this.facing = when (facing) {
            is String -> Facing.valueOf(facing.uppercase())
            is Facing -> facing
            else -> Facing.RIGHT
        }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        ignore.clear()
    }

    override fun hitProjectile(projectileFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val projectile = projectileFixture.getEntity()
        if (projectile.hasTag(Snowball.TAG)) return
        explodeAndDie()
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val id = blockFixture.getEntity().mapObjectId
        if (id != null && ignore.contains(id)) return
        explodeAndDie()
    }

    override fun hitBody(bodyFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val entity = bodyFixture.getEntity()
        if ((entity is IDamageableEntity && canDamage(entity)) || entity.hasAnyTag(BODIES_TO_IGNORE)) return
        explodeAndDie()
    }

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val id = shieldFixture.getEntity().mapObjectId
        if (id != null && ignore.contains(id)) return
        explodeAndDie()
    }

    override fun hitExplosion(explosionFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) =
        explodeAndDie()

    override fun onDamageInflictedTo(damageable: IDamageableEntity) = explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        GameLogger.debug(TAG, "explodeAndDie(): params=$params")
        IceShard.spawn5(body.getCenter(), TAG)
        destroy()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        when (state) {
            FallingIcicleState.FORM -> {
                formTimer.update(delta)
                if (formTimer.isFinished()) state = FallingIcicleState.STILL
            }

            FallingIcicleState.STILL -> if (timed) {
                stillTimer.update(delta)
                if (stillTimer.isFinished()) state = FallingIcicleState.SHAKE
            } else {
                if (players.anyLive {
                        it.body.getY() < body.getMaxY() && it.body.getMaxX() > body.getX() &&
                            it.body.getX() < body.getMaxX()
                    }
                ) state = FallingIcicleState.SHAKE
            }

            FallingIcicleState.SHAKE -> {
                shakeTimer.update(delta)
                if (shakeTimer.isFinished()) state = FallingIcicleState.FALL
            }

            else -> body.physics.gravityOn = true
        }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM, 1.5f * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture =
            Fixture(body, FixtureType.BODY, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.75f * ConstVals.PPM))
        body.addFixture(bodyFixture)

        val projectileFixture = Fixture(
            body,
            FixtureType.PROJECTILE,
            GameRectangle().setSize(0.25f * ConstVals.PPM, ConstVals.PPM.toFloat())
        )
        projectileFixture.offsetFromBodyAttachment.y = -0.1f * ConstVals.PPM
        body.addFixture(projectileFixture)

        val waterListener = Fixture(body, FixtureType.WATER_LISTENER, GameRectangle().setSize(0.1f * ConstVals.PPM))
        body.addFixture(waterListener)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocityClamp.y =
                (if (body.isSensing(BodySense.IN_WATER)) WATER_CLAMP_Y else NORMAL_CLAMP_Y) * ConstVals.PPM
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val position = Position.TOP_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.setFlip(isFacing(Facing.RIGHT), false)
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            when (state) {
                FallingIcicleState.STILL, FallingIcicleState.FALL -> "still"
                FallingIcicleState.FORM -> "form"
                FallingIcicleState.SHAKE -> "shake"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "form" pairTo Animation(regions["form"], FORM_FRAMES, 1, FORM_FRAME_DUR, false),
            "still" pairTo Animation(regions["still"]),
            "shake" pairTo Animation(regions["shake"], 2, 1, 0.1f, true),
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
