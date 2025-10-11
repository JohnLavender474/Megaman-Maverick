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
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
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
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
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
import com.megaman.maverick.game.entities.projectiles.PreciousGemBomb
import com.megaman.maverick.game.entities.projectiles.PreciousGemBomb.PreciousGemBombColor
import com.megaman.maverick.game.entities.projectiles.PreciousShard
import com.megaman.maverick.game.entities.projectiles.PreciousShard.PreciousShardColor
import com.megaman.maverick.game.entities.projectiles.PreciousShard.PreciousShardSize
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class PreciousGemCanon(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "PreciousGemCanon"

        private const val DEFAULT_SPAWN_DELAY = 0.25f

        private const val SWITCH_STATE_DELAY = 0.5f

        private const val SHOOT_GEM_OFFSET_X = 0.5f
        private const val SHOOT_GEM_OFFSET_Y = 0.6f
        private const val GEMS_TO_SHOOT = 3
        private const val EACH_SHOOT_GEM_DUR = 0.25f
        private const val SHOOT_GEM_SPEED = 10f

        private const val LAUNCH_GEM_OFFSET_X = 0.25f
        private const val LAUNCH_GEM_OFFSET_Y = 1f
        private const val GEMS_TO_LAUNCH = 2
        private const val EACH_LAUNCH_GEM_DUR = 0.5f
        private const val LAUNCH_GEM_IMPULSE = 8f

        private const val SHOOT_ANIM_DUR = 0.1f
        private const val SHOOT_SUFFIX = "_shoot"

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class PreciousGemCanonDirection { STRAIGHT, UP }

    override lateinit var facing: Facing

    private val canonDirectionLoop = Loop(PreciousGemCanonDirection.entries.toGdxArray())
    private val canonDirection: PreciousGemCanonDirection
        get() = canonDirectionLoop.getCurrent()

    private val switchStateDelay = Timer(SWITCH_STATE_DELAY)
    private val shootGemsTimer = Timer(GEMS_TO_SHOOT * EACH_SHOOT_GEM_DUR).also { timer ->
        for (i in 0 until GEMS_TO_SHOOT) {
            val time = i * EACH_SHOOT_GEM_DUR
            val runnable = TimeMarkedRunnable(time) {
                shootGemPiece()
                shootAnimTimer.reset()
            }
            timer.addRunnable(runnable)
        }
    }
    private val launchGemsTimer = Timer(GEMS_TO_LAUNCH * EACH_LAUNCH_GEM_DUR).also { timer ->
        for (i in 0 until GEMS_TO_LAUNCH) {
            val time = i * EACH_LAUNCH_GEM_DUR
            val runnable = TimeMarkedRunnable(time) {
                launchGemBomb()
                shootAnimTimer.reset()
            }
            timer.addRunnable(runnable)
        }
    }
    private val shootAnimTimer = Timer(SHOOT_ANIM_DUR)
    private val shooting: Boolean
        get() = !shootAnimTimer.isFinished()

    private val spawnDelayTimer = Timer()

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            val keys = Array<String>()
            PreciousGemCanonDirection.entries.forEach { direction ->
                val key = direction.name.lowercase()
                keys.add(key)
                keys.add("${key}${SHOOT_SUFFIX}")
            }
            keys.forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        FacingUtils.setFacingOf(this)

        canonDirectionLoop.reset()

        switchStateDelay.reset()
        launchGemsTimer.reset()
        shootGemsTimer.reset()

        shootAnimTimer.setToEnd()

        val spawnDelay = spawnProps.getOrDefault(ConstKeys.DELAY, DEFAULT_SPAWN_DELAY, Float::class)
        spawnDelayTimer.resetDuration(spawnDelay)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            shootAnimTimer.update(delta)

            spawnDelayTimer.update(delta)
            if (!spawnDelayTimer.isFinished()) return@add

            switchStateDelay.update(delta)
            if (!switchStateDelay.isFinished()) {
                FacingUtils.setFacingOf(this)
                return@add
            }

            val attackTimer = if (canonDirection == PreciousGemCanonDirection.UP) launchGemsTimer else shootGemsTimer
            attackTimer.update(delta)

            if (attackTimer.isFinished()) {
                attackTimer.reset()
                switchStateDelay.reset()
                canonDirectionLoop.next()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.5f * ConstVals.PPM, 0.75f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val damageableBounds = GameRectangle()
        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, damageableBounds)
        damageableFixture.attachedToBody = false
        body.addFixture(damageableFixture)
        damageableFixture.drawingColor = Color.PURPLE
        debugShapes.add { damageableFixture }

        val shieldFixture =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(1.5f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        shieldFixture.offsetFromBodyAttachment.y = -0.125f * ConstVals.PPM
        body.addFixture(shieldFixture)
        shieldFixture.drawingColor = Color.BLUE
        debugShapes.add { shieldFixture }

        body.preProcess.put(ConstKeys.DAMAGEABLE) {
            val damageableWidth: Float
            val damageableHeight: Float
            when (canonDirection) {
                PreciousGemCanonDirection.STRAIGHT -> {
                    damageableWidth = 1.5f
                    damageableHeight = 0.5f
                }
                PreciousGemCanonDirection.UP -> {
                    damageableWidth = 1.5f
                    damageableHeight = 0.75f
                }
            }
            damageableBounds.setSize(damageableWidth * ConstVals.PPM, damageableHeight * ConstVals.PPM)
            damageableBounds.setBottomCenterToPoint(body.getPositionPoint(Position.TOP_CENTER))
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2.5f * ConstVals.PPM, 2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier keySupplier@{
                    var key = canonDirection.name.lowercase()
                    if (shooting) key = "${key}${SHOOT_SUFFIX}"
                    return@keySupplier key
                }
                .applyToAnimations { animations ->
                    PreciousGemCanonDirection.entries.forEach { direction ->
                        val key = direction.name.lowercase()
                        animations.put(key, Animation(regions[key]))

                        val shootingKey = "${key}${SHOOT_SUFFIX}"
                        animations.put(shootingKey, Animation(regions[shootingKey], 2, 1, 0.1f, false))
                    }
                }
                .build()
        )
        .build()

    private fun shootGemPiece() {
        val spawn = GameObjectPools.fetch(Vector2::class)
            .set(body.getCenter())
            .add(SHOOT_GEM_OFFSET_X * facing.value * ConstVals.PPM, SHOOT_GEM_OFFSET_Y * ConstVals.PPM)

        val impulse = GameObjectPools.fetch(Vector2::class)
            .set(SHOOT_GEM_SPEED * ConstVals.PPM * facing.value, 0f)

        val bullet = MegaEntityFactory.fetch(PreciousShard::class)!!
        bullet.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.IMPULSE pairTo impulse,
                ConstKeys.GRAVITY_ON pairTo false,
                ConstKeys.SIZE pairTo PreciousShardSize.entries.random(),
                ConstKeys.COLOR pairTo PreciousShardColor.entries.random(),
            )
        )

        requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, loop = false, allowOverlap = false)
    }

    private fun launchGemBomb() {
        val spawn = GameObjectPools.fetch(Vector2::class)
            .set(body.getCenter())
            .add(LAUNCH_GEM_OFFSET_X * facing.value * ConstVals.PPM, LAUNCH_GEM_OFFSET_Y * ConstVals.PPM)

        val trajectory = GameObjectPools.fetch(Vector2::class)
            .set(LAUNCH_GEM_IMPULSE * facing.value, LAUNCH_GEM_IMPULSE)
            .scl(ConstVals.PPM.toFloat())

        val bomb = MegaEntityFactory.fetch(PreciousGemBomb::class)!!
        bomb.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory,
                ConstKeys.COLOR pairTo PreciousGemBombColor.entries.random()
            )
        )

        requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, loop = false, allowOverlap = false)
    }
}
