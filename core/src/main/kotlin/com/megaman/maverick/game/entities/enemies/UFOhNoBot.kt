package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
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
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
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
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.projectiles.UFOBomb
import com.megaman.maverick.game.entities.utils.FreezableEntityHandler
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class UFOhNoBot(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.MEDIUM), IFreezableEntity, IAnimatedEntity,
    IFaceable {

    companion object {
        const val TAG = "UFOhNoBot"

        private const val MAX_SPAWNED = 2

        private const val RISE_VEL = 8f

        private const val X_VEL_WITH_BOMB = 5f
        private const val X_VEL_NO_BOMB = 10f

        private const val DROP_DURATION = 1f
        private const val BOMB_DESTROYED_DUR = 0.5f

        private const val BLOCK_BUMPS_BEFORE_EXPLODE = 2

        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var facing: Facing

    override var frozen: Boolean
        get() = freezeHandler.isFrozen()
        set(value) {
            freezeHandler.setFrozen(value)
        }

    private val freezeHandler = FreezableEntityHandler(this)

    private val dropTimer = Timer()
    private val triggers = Array<GameRectangle>()

    private val start = Vector2()
    private val target = Vector2()

    private var dropped = false
    private var rising = false
    private var waiting = true

    private var blockBumps = 0

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            gdxArrayOf("with_bomb", "no_bomb", "with_bomb_frozen", "no_bomb_frozen").forEach { key ->
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun canSpawn(spawnProps: Properties) = super.canSpawn(spawnProps) &&
        MegaGameEntities.getOfTag(TAG).size < MAX_SPAWNED

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

        dropped = false
        rising = false

        waiting = spawnProps.getOrDefault(ConstKeys.WAIT, true, Boolean::class)
        when {
            waiting -> {
                spawnProps.getAllMatching { it.toString().contains(ConstKeys.TRIGGER) }.forEach {
                    val trigger = (it.second as RectangleMapObject).rectangle.toGameRectangle(false)
                    triggers.add(trigger)
                }

                start.set(spawnProps.get(ConstKeys.START, RectangleMapObject::class)!!.rectangle.getCenter())
                target.set(spawnProps.get(ConstKeys.TARGET, RectangleMapObject::class)!!.rectangle.getCenter())

                dropTimer.resetDuration(DROP_DURATION)

                body.forEachFixture { it.setActive(false) }
            }

            else -> setToHover()
        }

        blockBumps = 0

        frozen = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        triggers.forEach { GameObjectPools.free(it) }
        triggers.clear()

        frozen = false
    }

    private fun isMegamanUnderMe() = megaman.body.getMaxY() <= body.getY() &&
        megaman.body.getCenter().x >= body.getX() && megaman.body.getCenter().x <= body.getMaxX()

    private fun moveX() {
        val xVel = (if (dropped) X_VEL_NO_BOMB else X_VEL_WITH_BOMB) * ConstVals.PPM * facing.value
        body.physics.velocity.set(xVel, 0f)
    }

    private fun dropBomb() {
        val spawn = body.getPositionPoint(Position.BOTTOM_CENTER).sub(0f, 0.75f * ConstVals.PPM)

        val bomb = MegaEntityFactory.fetch(UFOBomb::class)!!
        bomb.spawn(props(ConstKeys.POSITION pairTo spawn, ConstKeys.OWNER pairTo this))
    }

    private fun setToHover() {
        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
        moveX()
    }

    override fun swapFacing() {
        super.swapFacing()
        GameLogger.debug(TAG, "swapFacing(): new facing = $facing")
        moveX()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            freezeHandler.update(delta)

            if (frozen) {
                body.physics.velocity.setZero()
                return@add
            }

            if (waiting) when {
                triggers.any { trigger -> megaman.body.getBounds().overlaps(trigger) } -> {
                    waiting = false
                    rising = true

                    body.setCenter(start)

                    val trajectory = target.cpy().sub(start).nor().scl(RISE_VEL * ConstVals.PPM)
                    body.physics.velocity.set(trajectory)

                    body.forEachFixture { it.setActive(true) }

                    facing = when {
                        trajectory.x == 0f -> if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
                        trajectory.x < 0f -> Facing.LEFT
                        else -> Facing.RIGHT
                    }
                }
                else -> return@add
            }

            if (rising) when {
                body.getCenter().epsilonEquals(target, 0.1f * ConstVals.PPM) -> {
                    rising = false
                    setToHover()
                }
                else -> return@add
            }

            if (!dropped && isMegamanUnderMe()) {
                dropBomb()
                dropped = true
                dropTimer.resetDuration(DROP_DURATION)

                body.physics.velocity.setZero()
            }

            if (!dropTimer.isFinished()) {
                dropTimer.update(delta)
                if (dropTimer.isFinished()) moveX()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat(), 1.5f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameCircle().setRadius(0.4f * ConstVals.PPM))
        body.addFixture(bodyFixture)
        bodyFixture.drawingColor = Color.RED
        debugShapes.add { bodyFixture }

        val shieldFixture =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(0.15f * ConstVals.PPM, 0.75f * ConstVals.PPM))
        shieldFixture.offsetFromBodyAttachment.y = -0.5f * ConstVals.PPM
        body.addFixture(shieldFixture)
        shieldFixture.drawingColor = Color.BLUE
        debugShapes.add { shieldFixture }

        val damagerFixture1 = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.4f * ConstVals.PPM))
        body.addFixture(damagerFixture1)

        val damagerFixture2 =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(ConstVals.PPM.toFloat(), 1.75f * ConstVals.PPM))
        damagerFixture2.attachedToBody = false
        body.addFixture(damagerFixture2)
        debugShapes.add { damagerFixture2 }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameCircle().setRadius(0.4f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        val leftSideFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 1.5f * ConstVals.PPM))
        leftSideFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        leftSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftSideFixture)
        leftSideFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftSideFixture }

        val rightSideFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 1.5f * ConstVals.PPM))
        rightSideFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        rightSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightSideFixture)
        rightSideFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightSideFixture }

        val bombDamagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.5f * ConstVals.PPM))
        bombDamagerFixture.offsetFromBodyAttachment.y = -1.25f * ConstVals.PPM
        body.addFixture(bombDamagerFixture)

        val bombConsumerFixture = Fixture(body, FixtureType.CONSUMER, GameCircle().setRadius(0.5f * ConstVals.PPM))
        bombConsumerFixture.offsetFromBodyAttachment.y = -1.25f * ConstVals.PPM
        bombConsumerFixture.setFilter { fixture ->
            !dropped && fixture.getType() == FixtureType.PROJECTILE &&
                (fixture.getEntity() as IProjectileEntity).owner == megaman
        }
        bombConsumerFixture.setConsumer { processState, fixture ->
            val projectile = fixture.getEntity() as IProjectileEntity
            if (projectile.owner == megaman) {
                // so that the enemy doesn't get stuck in place if the bomb gets destroyed while he's rising
                rising = false

                dropped = true
                dropTimer.resetDuration(BOMB_DESTROYED_DUR)

                body.physics.velocity.setZero()

                val spawn = bombConsumerFixture.getShape().getCenter()

                val explosion = MegaEntityFactory.fetch(Explosion::class)!!
                explosion.spawn(props(ConstKeys.OWNER pairTo this, ConstKeys.POSITION pairTo spawn))

                requestToPlaySound(SoundAsset.EXPLOSION_2_SOUND, false)
            }
        }
        body.addFixture(bombConsumerFixture)
        bombConsumerFixture.drawingColor = Color.ORANGE
        debugShapes.add { if (bombConsumerFixture.isActive()) bombConsumerFixture else null }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (!waiting && !rising && !frozen && FacingUtils.isFacingBlock(this)) {
                blockBumps++
                if (blockBumps >= BLOCK_BUMPS_BEFORE_EXPLODE) {
                    val explosion = MegaEntityFactory.fetch(Explosion::class)!!
                    explosion.spawn(props(ConstKeys.OWNER pairTo this, ConstKeys.POSITION pairTo body.getCenter()))

                    requestToPlaySound(SoundAsset.EXPLOSION_2_SOUND, false)

                    destroy()
                }

                swapFacing()
            }

            val active = !dropped && !waiting
            bombDamagerFixture.setActive(active)
            bombConsumerFixture.setActive(active)

            val position = Position.TOP_CENTER
            (damagerFixture2.rawShape as GameRectangle).positionOnPoint(body.getPositionPoint(position), position)
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        sprite.setSize(2.5f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val position = Position.TOP_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.hidden = damageBlink || waiting
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = keySupplier@{
            var key = if (dropped) "no_bomb" else "with_bomb"
            if (frozen) key += "_frozen"
            return@keySupplier key
        }
        val animations = objectMapOf<String, IAnimation>(
            "no_bomb" pairTo Animation(regions["no_bomb"], 2, 2, 0.1f, true),
            "with_bomb" pairTo Animation(regions["with_bomb"], 2, 2, 0.1f, true),
            "no_bomb_frozen" pairTo Animation(regions["no_bomb_frozen"]),
            "with_bomb_frozen" pairTo Animation(regions["with_bomb_frozen"])
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
