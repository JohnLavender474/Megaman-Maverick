package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameLine
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
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
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.megaman.components.damageableFixture
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

class JetpackBlaster(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.MEDIUM), IAnimatedEntity,
    IDrawableShapesEntity, IFaceable {

    companion object {
        const val TAG = "JetpackBlaster"

        private const val FLY_TO_TARGET_SPEED = 6f
        private const val FLY_TO_TARGET_MAX_DUR = 1.25f
        private const val SHOOT_DUR = 1f
        private const val BLAST_SPEED = 12f

        private const val STRAIGHT_LINE_OF_SIGHT_ANGLE = 90f
        private const val FAR_LINE_OF_SIGHT_ANGLE = 120f
        private const val MID_LINE_OF_SIGHT_ANGLE = 135f
        private const val UNDER_LINE_OF_SIGHT_ANGLE = 180f

        private const val MAX_Y_AIM_ADJUSTMENT = 3f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class JetpackBlasterState { FLY_TO_TARGET, ADJUST_AIM, SHOOT }

    private enum class DistanceType(val angle: Float) {
        STRAIGHT(STRAIGHT_LINE_OF_SIGHT_ANGLE),
        FAR(FAR_LINE_OF_SIGHT_ANGLE),
        MID(MID_LINE_OF_SIGHT_ANGLE),
        UNDER(UNDER_LINE_OF_SIGHT_ANGLE)
    }

    override lateinit var facing: Facing

    private val loop = Loop(JetpackBlasterState.entries.toGdxArray())
    private val flyToTargetTimer = Timer(FLY_TO_TARGET_MAX_DUR)
    private val shootTimer = Timer(SHOOT_DUR, gdxArrayOf(TimeMarkedRunnable(0.25f) { shoot() }))

    private val canMoveDown: Boolean
        get() = !body.isSensing(BodySense.FEET_ON_GROUND)
    private val canMoveUp: Boolean
        get() = !body.isSensing(BodySense.HEAD_TOUCHING_BLOCK)

    private lateinit var distanceType: DistanceType
    private lateinit var target: Vector2

    private var aimLine: GameLine? = null

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            DistanceType.entries.forEach { d ->
                val regionKey = "${d.name.lowercase()}_thrust"
                val region =
                    atlas.findRegion("$TAG/$regionKey") ?: throw IllegalStateException("region is null: $regionKey")
                regions.put(regionKey, region)
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
        addDebugShapeSupplier { aimLine }
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

        distanceType = DistanceType.FAR

        target = when {
            spawnProps.containsKey(ConstKeys.TARGET) -> spawnProps.get(
                ConstKeys.TARGET,
                RectangleMapObject::class
            )!!.rectangle.getCenter(false)

            else -> {
                val target1 =
                    spawnProps.get("${ConstKeys.TARGET}_1", RectangleMapObject::class)!!.rectangle.getCenter(false)
                val target2 =
                    spawnProps.get("${ConstKeys.TARGET}_2", RectangleMapObject::class)!!.rectangle.getCenter(false)

                val megamanCenter = megaman.body.getCenter()

                if (target1.dst2(megamanCenter) < target2.dst2(megamanCenter)) target1 else target2
            }
        }
        setVelocityToTarget()

        loop.reset()
        shootTimer.reset()
    }

    private fun shoot() {
        val originOffsetX = (calculateAimLineOriginOffset(distanceType).x + when (distanceType) {
            DistanceType.STRAIGHT, DistanceType.FAR -> 0.75f
            DistanceType.MID -> 0.5f
            DistanceType.UNDER -> 0.25f
        }) * facing.value

        val originOffsetY = when (distanceType) {
            DistanceType.STRAIGHT -> 0.25f
            DistanceType.FAR -> -0.1f
            DistanceType.MID -> -0.25f
            DistanceType.UNDER -> -0.5f
        }

        val spawn = body.getCenter().add(originOffsetX * ConstVals.PPM, originOffsetY * ConstVals.PPM)

        val trajectory = Vector2(0f, BLAST_SPEED * ConstVals.PPM).rotateDeg(aimLine!!.rotation)

        val blast = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.TEARDROP_BLAST)!!
        blast.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory
            )
        )

        val flash = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.MUZZLE_FLASH)!!
        flash.spawn(props(ConstKeys.POSITION pairTo spawn))

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.BLAST_1_SOUND, false)
    }

    private fun calculateAimLineOriginOffset(distanceType: DistanceType): Vector2 {
        val offset = GameObjectPools.fetch(Vector2::class)
        when (distanceType) {
            DistanceType.STRAIGHT -> offset.set(0.25f, -0.1f)
            DistanceType.FAR -> offset.set(0.15f, 0f)
            DistanceType.MID -> offset.set(0.1f, 0f)
            DistanceType.UNDER -> offset.set(0.075f, 0f)
        }
        return offset
    }

    private fun calculateAimLine(distanceType: DistanceType): GameLine {
        val line = GameObjectPools.fetch(GameLine::class)
            .set(body.getCenter(), body.getCenter().add(0f, 10f * ConstVals.PPM))

        val originOffset = calculateAimLineOriginOffset(distanceType)
        line.setOrigin(
            body.getCenter().add(originOffset.x * facing.value * ConstVals.PPM, originOffset.y * ConstVals.PPM)
        )

        var angle = distanceType.angle
        if (isFacing(Facing.RIGHT)) angle = 360 - angle
        line.rotation = angle

        return line
    }

    private fun getBestDistanceType(): DistanceType {
        var bestDistance = Float.MAX_VALUE
        lateinit var bestType: DistanceType

        DistanceType.entries.forEach {
            val line = calculateAimLine(it)
            GameLogger.debug(
                TAG,
                "Testing if $it is best fit with facing=$facing, angle=${line.rotation}, & line=$line"
            )
            val distance = line.worldDistanceFromPoint(megaman.body.getCenter(), false)
            if (distance < bestDistance) {
                bestType = it
                bestDistance = distance
                GameLogger.debug(TAG, "New bestDistance=$bestDistance & bestType=$bestType")
            }
        }

        GameLogger.debug(TAG, "Best distance type = $bestType")

        return bestType
    }

    private fun getVerticalAdjustment(): Float {
        if (aimLine!!.overlaps(megaman.damageableFixture.getShape())) return 0f

        val megamanLine = GameObjectPools.fetch(GameLine::class)
            .set(0f, 0f, 0f, ConstVals.VIEW_HEIGHT * ConstVals.PPM)
        megamanLine.setCenter(megaman.body.getCenter())

        GameLogger.debug(TAG, "Megaman center = ${megaman.body.getCenter()}")
        GameLogger.debug(TAG, "Drawing vertical line over Megaman = $megamanLine")

        val intersection = megamanLine.intersectionPoint(aimLine!!, GameObjectPools.fetch(Vector2::class)) ?: return 0f
        GameLogger.debug(TAG, "Intersection between aim line and Megaman line = $intersection")

        var adjustment = megaman.body.getCenter().y - intersection.y

        if ((adjustment > 0f && !canMoveUp) || (adjustment < 0f && !canMoveDown)) return 0f

        if (adjustment > MAX_Y_AIM_ADJUSTMENT * ConstVals.PPM) adjustment = MAX_Y_AIM_ADJUSTMENT * ConstVals.PPM
        else if (adjustment < -MAX_Y_AIM_ADJUSTMENT * ConstVals.PPM) adjustment = -MAX_Y_AIM_ADJUSTMENT * ConstVals.PPM

        GameLogger.debug(TAG, "Vertical adjustment calculated = $adjustment")

        return adjustment
    }

    private fun setVelocityToTarget() {
        val velocity = GameObjectPools.fetch(Vector2::class)
            .set(target)
            .sub(body.getCenter())
            .nor()
            .scl(FLY_TO_TARGET_SPEED * ConstVals.PPM)
        body.physics.velocity.set(velocity)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (megaman.dead) {
                body.physics.velocity.setZero()
                return@add
            }

            when (loop.getCurrent()) {
                JetpackBlasterState.FLY_TO_TARGET -> {
                    flyToTargetTimer.update(delta)
                    if (body.getCenter().epsilonEquals(target, 0.1f * ConstVals.PPM) || flyToTargetTimer.isFinished()) {
                        loop.next()
                        flyToTargetTimer.reset()

                        if (megaman.body.getMaxX() < body.getX()) facing = Facing.LEFT
                        else if (megaman.body.getX() > body.getMaxX()) facing = Facing.RIGHT

                        distanceType = getBestDistanceType()
                        aimLine = calculateAimLine(distanceType)

                        val adjustment = getVerticalAdjustment()
                        target.set(body.getCenter().add(0f, adjustment))
                        setVelocityToTarget()

                        GameLogger.debug(TAG, "Setting state to next = ${loop.getCurrent()}")
                    }
                }

                JetpackBlasterState.ADJUST_AIM -> {
                    aimLine = calculateAimLine(distanceType)
                    flyToTargetTimer.update(delta)
                    if (body.getCenter().epsilonEquals(target, 0.1f * ConstVals.PPM) || flyToTargetTimer.isFinished()) {
                        loop.next()
                        flyToTargetTimer.reset()
                        body.physics.velocity.setZero()
                        GameLogger.debug(TAG, "Setting state to next = ${loop.getCurrent()}")
                    }
                }

                JetpackBlasterState.SHOOT -> {
                    shootTimer.update(delta)
                    if (shootTimer.isFinished()) {
                        loop.next()
                        shootTimer.reset()
                        GameLogger.debug(TAG, "Setting state to next = ${loop.getCurrent()}")
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.25f * ConstVals.PPM, 1.5f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.75f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(0.75f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)
        debugShapes.add { headFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2.5f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            sprite.setCenter(body.getCenter())
            sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { "${distanceType.name.lowercase()}_thrust" }
        val animations = objectMapOf<String, IAnimation>()
        DistanceType.entries.forEach { d ->
            val regionKey = "${d.name.lowercase()}_thrust"
            val region = regions[regionKey]
            animations.put(regionKey, Animation(region!!, 2, 1, 0.1f, true))
        }
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
