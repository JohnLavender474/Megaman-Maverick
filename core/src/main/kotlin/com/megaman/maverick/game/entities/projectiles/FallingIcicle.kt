package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.decorations.Snow
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class FallingIcicle(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "FallingIcicle"
        private const val SHAKE_DUR = 0.25f
        private const val GRAVITY = -0.15f
        private const val SHATTER_DUR = 0.25f
        private const val NORMAL_CLAMP_Y = 10f
        private const val WATER_CLAMP_Y = 3f
        private val BODIES_TO_IGNORE = objectSetOf<String>(Snow.TAG)
        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class FallingIcicleState { STILL, SHAKE, FALL }

    override lateinit var facing: Facing

    private val shakeTimer = Timer(SHAKE_DUR)
    private val shatterTimer = Timer(SHATTER_DUR)

    private lateinit var state: FallingIcicleState

    private val ignore = ObjectSet<Int>()

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_2.source)
            gdxArrayOf("still", "shake").forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
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

        shakeTimer.reset()
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
        if (projectile.getTag() == Snowball.TAG) return
        explodeAndDie()
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val id = blockFixture.getEntity().mapObjectId
        if (ignore.contains(id)) return
        explodeAndDie()
    }

    override fun hitBody(bodyFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val entity = bodyFixture.getEntity()
        if ((entity is IDamageable && canDamage(entity)) || BODIES_TO_IGNORE.contains(entity.getTag())) return
        explodeAndDie()
    }

    override fun hitExplosion(explosionFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) =
        explodeAndDie()

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        GameLogger.debug(TAG, "explodeAndDie(): params=$params")
        IceShard.spawn5(body.getCenter(), TAG)
        destroy()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        when (state) {
            FallingIcicleState.STILL -> {
                if (megaman.body.getY() < body.getMaxY() &&
                    megaman.body.getMaxX() > body.getX() &&
                    megaman.body.getX() < body.getMaxX()
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
        component.putUpdateFunction { _, _ ->
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
                FallingIcicleState.SHAKE -> "shake"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "still" pairTo Animation(regions["still"]),
            "shake" pairTo Animation(regions["shake"], 2, 1, 0.1f, true),
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
