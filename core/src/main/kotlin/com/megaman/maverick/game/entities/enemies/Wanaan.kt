package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.IFreezerEntity
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.world.body.*

class Wanaan(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.MEDIUM), IAnimatedEntity, IFreezableEntity,
    IDirectional {

    companion object {
        const val TAG = "Wanaan"
        private const val GRAVITY = 0.15f
        private const val FROZEN_DUR = 0.5f
        private val animDefs =
            orderedMapOf("frozen" pairTo AnimationDef(), "chomp" pairTo AnimationDef(2, 1, 0.1f, true))
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }
    override var frozen: Boolean
        get() = !frozenTimer.isFinished()
        set(value) {
            if (value) frozenTimer.reset() else frozenTimer.setToEnd()
        }
    override var invincible = true // wanaan cannot be damaged

    val comingDown: Boolean
        get() {
            val velocity = body.physics.velocity
            return when (direction) {
                Direction.UP -> velocity.y < 0f
                Direction.DOWN -> velocity.y > 0f
                Direction.LEFT -> velocity.x > 0f
                Direction.RIGHT -> velocity.x < 0f
            }
        }
    val cullPoint: Vector2
        get() = body.getCenter()

    private val frozenTimer = Timer(ConstVals.STANDARD_FROZEN_DUR)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            animDefs.keys().forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setBottomCenterToPoint(spawn)

        val gravityOn = spawnProps.getOrDefault(ConstKeys.GRAVITY_ON, false, Boolean::class)
        body.physics.gravityOn = gravityOn

        direction = spawnProps.get(ConstKeys.DIRECTION, Direction::class)!!

        frozen = false
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (frozen) {
                frozenTimer.update(delta)
                if (frozenTimer.isJustFinished()) {
                    damageTimer.reset()
                    IceShard.spawn5(body.getCenter())
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        bodyFixture.setHitByProjectileReceiver { projectile ->
            if (!frozen && projectile is IFreezerEntity) {
                frozen = true
                projectile.shatterAndDie()
            }
        }
        body.addFixture(bodyFixture)

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(0.75f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.YELLOW
        debugShapes.add { headFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val velocity = body.physics.velocity
            if (body.isSensing(BodySense.HEAD_TOUCHING_BLOCK)) when (direction) {
                Direction.UP -> if (velocity.y > 0f) velocity.y = 0f
                Direction.DOWN -> if (velocity.y < 0f) velocity.y = 0f
                Direction.RIGHT -> if (velocity.x > 0f) velocity.x = 0f
                Direction.LEFT -> if (velocity.x < 0f) velocity.x = 0f
            }

            val gravity = body.physics.gravity
            when (direction) {
                Direction.UP -> gravity.y = -GRAVITY * ConstVals.PPM
                Direction.DOWN -> gravity.y = GRAVITY * ConstVals.PPM
                Direction.RIGHT -> gravity.x = -GRAVITY * ConstVals.PPM
                Direction.LEFT -> gravity.x = GRAVITY * ConstVals.PPM
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.SHIELD, FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, -2))
                .also { sprite -> sprite.setSize(2f * ConstVals.PPM) }
        )
        .preProcess { _, sprite ->
            sprite.hidden = damageBlink
            sprite.setCenter(body.getCenter())
            sprite.setFlip(false, comingDown)
            sprite.setOriginCenter()
            when (direction) {
                Direction.UP -> sprite.rotation = 0f
                Direction.DOWN -> sprite.rotation = 180f
                Direction.LEFT -> sprite.rotation = 90f
                Direction.RIGHT -> sprite.rotation = 270f
            }
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { if (frozen) "frozen" else "chomp" }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val (rows, columns, durations, loop) = entry.value
                        animations.put(key, Animation(regions[key], rows, columns, durations, loop))
                    }
                }
                .build()
        )
        .build()

    override fun getTag() = TAG

    fun explodeAndDie() {
        GameLogger.debug(TAG, "explodeAndDie()")
        explode()
        destroy()
    }
}
