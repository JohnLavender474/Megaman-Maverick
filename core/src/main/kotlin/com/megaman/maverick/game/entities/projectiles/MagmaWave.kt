package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.isAny
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IFireEntity
import com.megaman.maverick.game.entities.enemies.LumberJoe
import com.megaman.maverick.game.entities.enemies.ShieldGuardBot
import com.megaman.maverick.game.entities.hazards.MagmaFlame
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*

class MagmaWave(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity, IFireEntity, IFaceable,
    IDirectional {

    companion object {
        const val TAG = "MagmaWave"
        private const val DISINTEGRATE_TIME = 0.3f
        private const val DEFAULT_DROP_FLAME_DELAY = 0.25f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }
    override lateinit var facing: Facing

    var disintegrating = false

    private val dropFlameDelay = Timer()
    private val disintegrationTimer = Timer(DISINTEGRATE_TIME)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_2.source)
            gdxArrayOf("wave", "disintegrate").forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        direction = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        val position = DirectionPositionMapper.getPosition(direction).opposite()
        body.positionOnPoint(spawn, position)

        val trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
        body.physics.velocity.set(trajectory)

        facing = when {
            spawnProps.containsKey(ConstKeys.FACING) -> spawnProps.get(ConstKeys.FACING, Facing::class)!!
            trajectory.x < 0f -> Facing.LEFT
            else -> Facing.RIGHT
        }

        val dropFlameDelayDur = spawnProps.getOrDefault(
            "${ConstKeys.DROP}_${ConstKeys.FLAME}_${ConstKeys.DELAY}",
            DEFAULT_DROP_FLAME_DELAY,
            Float::class
        )
        dropFlameDelay.resetDuration(dropFlameDelayDur)

        disintegrating = false
        disintegrationTimer.reset()
    }

    override fun canDamage(damageable: IDamageable) = !disintegrating

    override fun hitWater(waterFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        disintegrating = true
    }

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val entity = shieldFixture.getEntity()
        if (entity.isAny(LumberJoe::class, ShieldGuardBot::class)) return
        disintegrating = true
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (disintegrating) {
            disintegrationTimer.update(delta)
            if (disintegrationTimer.isFinished()) destroy()
        } else {
            dropFlameDelay.update(delta)
            if (dropFlameDelay.isFinished()) {
                dropFlame()
                dropFlameDelay.reset()
            }
        }
    })

    private fun dropFlame() {
        val flame = MegaEntityFactory.fetch(MagmaFlame::class)!!
        flame.spawn(
            props(
                ConstKeys.OWNER pairTo owner,
                ConstKeys.DIRECTION pairTo direction,
                ConstKeys.POSITION pairTo body.getCenter(),
            )
        )
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.25f * ConstVals.PPM, 2.5f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.BLUE

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val projectileFixture =
            Fixture(body, FixtureType.PROJECTILE, GameRectangle().setSize(0.5f * ConstVals.PPM, 3f * ConstVals.PPM))
        body.addFixture(projectileFixture)
        projectileFixture.drawingColor = Color.ORANGE
        debugShapes.add { projectileFixture }

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        leftFixture.offsetFromBodyAttachment.x = -0.125f * ConstVals.PPM
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.setHitByShield { _, _ -> disintegrating = true }
        body.addFixture(leftFixture)
        debugShapes.add { leftFixture }

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        rightFixture.offsetFromBodyAttachment.x = 0.125f * ConstVals.PPM
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.setHitByShield { _, _ -> disintegrating = true }
        body.addFixture(rightFixture)
        debugShapes.add { rightFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (!disintegrating &&
                ((isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) ||
                    (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)))
            ) disintegrating = true

            if (disintegrating) body.physics.velocity.setZero()
        }

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 15))
        sprite.setSize(3f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            sprite.setCenter(body.getCenter())
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { if (disintegrating) "disintegrate" else "wave" }
        val animations = objectMapOf<String, IAnimation>(
            "disintegrate" pairTo Animation(regions["disintegrate"], 3, 1, 0.1f, false),
            "wave" pairTo Animation(regions["wave"], 2, 1, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

}
