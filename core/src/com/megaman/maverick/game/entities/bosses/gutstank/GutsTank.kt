package com.megaman.maverick.game.entities.bosses.gutstank

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedSet
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.getCenter
import com.engine.common.shapes.getPosition
import com.engine.common.shapes.toGameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.enemies.HeliMet
import com.megaman.maverick.game.entities.enemies.Met
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.BlocksFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureLabel
import com.megaman.maverick.game.world.FixtureType
import kotlin.math.abs
import kotlin.reflect.KClass

class GutsTank(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity {

    enum class GutsTankMoveState {
        PAUSE, MOVE,
    }

    enum class GutsTankAttackState {
        LAUNCH_FIST, LAUNCH_RUNNING_METS, LAUNCH_FLYING_METS, CHUNK_BULLETS, SHOOT_BLASTS, DO_NOTHING
    }

    companion object {
        const val TAG = "GutsTank"

        private val ATTACK_STATES = GutsTankAttackState.values()

        private const val KILLER_WALL = "killer_wall"

        private const val FLY1 = "fly1"
        private const val FLY2 = "fly2"

        private const val BODY_WIDTH = 8f
        private const val BODY_HEIGHT = 4.46875f

        private const val TANK_BLOCK_HEIGHT = 2.0365f

        private const val BODY_BLOCK_WIDTH = 4.75f
        private const val BODY_BLOCK_HEIGHT = 12f

        private const val MIN_X_VEL = 1.5f
        private const val MAX_X_VEL = 3f
        private const val MOVEMENT_PAUSE_DUR = 1.5f

        private const val ATTACK_DELAY_MIN = 1f
        private const val ATTACK_DELAY_MAX = 2f

        private const val BULLETS_TO_CHUNK = 5
        private const val CHUNKED_BULLET_GRAVITY = -0.15f
        private const val CHUNKED_BULLET_VELOCITY_Y = 10f
        private const val BULLET_CHUNK_DELAY = 0.25f

        private const val BLASTS_TO_SHOOT = 3
        private const val BLAST_SHOOT_DELAY = 0.15f
        private const val BLAST_VELOCITY = 5f
        private val BLAST_ANGLES = gdxArrayOf(180f, 190f, 200f, 210f, 220f)

        private const val RUNNING_METS_TO_LAUNCH = 3
        private const val RUNNING_MET_DELAY = 1f

        private const val FLYING_METS_TO_LAUNCH = 2
        private const val FLYING_MET_DELAY = 1f

        private const val LAUNCH_FIST_DELAY = 10f
        private const val DO_NOTHING_DUR = 1f
        private const val LAUGH_DUR = 1.25f

        private var laughingRegion: TextureRegion? = null
        private var mouthOpenRegion: TextureRegion? = null
        private var mouthClosedRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(1), ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 2 else 1
        }, ChargedShotExplosion::class to dmgNeg(1)
    )

    internal var tankBlock: Block? = null
    internal var bodyBlock: Block? = null
    internal val runningMetsSet = OrderedSet<Met>()
    internal val flyingMetsSet = OrderedSet<HeliMet>()

    private val attackDelayTimer = Timer(ATTACK_DELAY_MAX)
    private val movementPauseTimer = Timer(MOVEMENT_PAUSE_DUR)
    private val bulletChunkDelayTimer = Timer(BULLET_CHUNK_DELAY)
    private val blastShootDelayTimer = Timer(BLAST_SHOOT_DELAY)
    private val runningMetDelayTimer = Timer(RUNNING_MET_DELAY)
    private val flyingMetDelayTimer = Timer(FLYING_MET_DELAY)
    private val launchFistDelayTimer = Timer(LAUNCH_FIST_DELAY)
    private val doNothingTimer = Timer(DO_NOTHING_DUR)
    private val laughTimer = Timer(LAUGH_DUR)

    private val flyingMetTargets = Array<Vector2>()
    private val laughing: Boolean
        get() = !laughTimer.isFinished()

    private lateinit var frontPoint: Vector2
    private lateinit var backPoint: Vector2
    private lateinit var tankBlockOffset: Vector2
    private lateinit var bodyBlockOffset: Vector2
    private lateinit var moveState: GutsTankMoveState
    private lateinit var killerWall: GameRectangle

    private var attackState: GutsTankAttackState? = null
    private var fist: GutsTankFist? = null
    private var reachedFrontFirstTime = false
    private var moveToFront = true
    private var bulletsChunked = 0
    private var blastsShot = 0
    private var runningMets = 0
    private var flyingMets = 0

    override fun init() {
        if (laughingRegion == null || mouthOpenRegion == null || mouthClosedRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            laughingRegion = atlas.findRegion("GutsTank/Laughing")
            mouthOpenRegion = atlas.findRegion("GutsTank/MouthOpen")
            mouthClosedRegion = atlas.findRegion("GutsTank/MouthClosed")
        }
        super<AbstractBoss>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomLeftPoint()
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.setBottomLeftToPoint(spawn)

        killerWall = spawnProps.get(KILLER_WALL, RectangleMapObject::class)!!.rectangle.toGameRectangle()

        tankBlock = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.STANDARD)!! as Block
        val tankBlockBounds =
            GameRectangle(spawn.x, spawn.y, BODY_WIDTH * ConstVals.PPM, TANK_BLOCK_HEIGHT * ConstVals.PPM)
        game.engine.spawn(
            tankBlock!!, props(
                ConstKeys.CULL_OUT_OF_BOUNDS to false,
                ConstKeys.BOUNDS to tankBlockBounds,
                ConstKeys.FIXTURE_LABELS to objectSetOf(FixtureLabel.NO_PROJECTILE_COLLISION),
                ConstKeys.FIXTURES to gdxArrayOf(
                    Pair(FixtureType.SHIELD, props(ConstKeys.DIRECTION to Direction.DOWN))
                )
            )
        )

        bodyBlock = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.STANDARD)!! as Block
        val bodyBlockBounds = GameRectangle().setSize(
            BODY_BLOCK_WIDTH * ConstVals.PPM, BODY_BLOCK_HEIGHT * ConstVals.PPM
        ).setBottomRightToPoint(tankBlockBounds.getBottomRightPoint())
        game.engine.spawn(
            bodyBlock!!, props(
                ConstKeys.CULL_OUT_OF_BOUNDS to false,
                ConstKeys.BOUNDS to bodyBlockBounds,
                ConstKeys.FIXTURE_LABELS to objectSetOf(
                    FixtureLabel.NO_SIDE_TOUCHIE, FixtureLabel.NO_PROJECTILE_COLLISION
                ),
                ConstKeys.FIXTURES to gdxArrayOf(
                    Pair(FixtureType.SHIELD, props(ConstKeys.DIRECTION to Direction.UP))
                )
            )
        )

        tankBlockOffset = tankBlockBounds.getCenter().sub(body.getCenter())
        bodyBlockOffset = bodyBlockBounds.getCenter().sub(body.getCenter())

        fist = GutsTankFist(game)
        game.engine.spawn(fist!!, props(ConstKeys.PARENT to this))

        frontPoint = spawnProps.get(ConstKeys.FRONT, RectangleMapObject::class)!!.rectangle.getPosition()
        backPoint = spawnProps.get(ConstKeys.BACK, RectangleMapObject::class)!!.rectangle.getPosition()

        flyingMetTargets.clear()
        val fly1Target = spawnProps.get(FLY1, RectangleMapObject::class)!!.rectangle.getCenter()
        val fly2Target = spawnProps.get(FLY2, RectangleMapObject::class)!!.rectangle.getCenter()
        flyingMetTargets.addAll(fly1Target, fly2Target)

        moveState = GutsTankMoveState.MOVE
        moveToFront = true
        reachedFrontFirstTime = false

        attackState = null
        attackDelayTimer.resetDuration(ATTACK_DELAY_MAX)

        movementPauseTimer.reset()

        bulletChunkDelayTimer.reset()
        bulletsChunked = 0

        blastShootDelayTimer.reset()
        blastsShot = 0

        launchFistDelayTimer.reset()

        runningMets = 0
        flyingMets = 0

        doNothingTimer.reset()
        laughTimer.setToEnd()
    }

    override fun onDestroy() {
        super<AbstractBoss>.onDestroy()
        runningMetsSet.forEach { it.kill() }
        runningMetsSet.clear()
        flyingMetsSet.forEach { it.kill() }
        flyingMetsSet.clear()
        fist?.kill()
        fist = null
        tankBlock?.kill()
        tankBlock = null
        bodyBlock?.kill()
        bodyBlock = null
    }

    override fun triggerDefeat() {
        super.triggerDefeat()
        moveState = GutsTankMoveState.PAUSE
        runningMetsSet.forEach { it.setHealth(0) }
        runningMetsSet.clear()
        flyingMetsSet.forEach { it.setHealth(0) }
        flyingMetsSet.clear()
        fist?.setHealth(0)
        fist = null
        tankBlock?.kill()
        tankBlock = null
        bodyBlock?.kill()
        bodyBlock = null
    }

    override fun canBeDamagedBy(damager: IDamager): Boolean {
        if (damager is AbstractProjectile) {
            damager.owner?.let { damagerOwner ->
                if (damagerOwner == this || damagerOwner == fist || damagerOwner == tankBlock || damagerOwner == bodyBlock || (damagerOwner is Met && runningMetsSet.contains(
                        damagerOwner
                    )) || (damagerOwner is HeliMet && flyingMetsSet.contains(damagerOwner))
                ) return false
            }
        }
        return super.canBeDamagedBy(damager)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!ready) return@add
            if (defeated) {
                body.physics.velocity.setZero()
                explodeOnDefeat(delta)
                return@add
            }
            if (laughing) {
                laughTimer.update(delta)
                body.physics.velocity.x = 0f
                tankBlock?.body!!.physics.velocity.x = 0f
                bodyBlock?.body!!.physics.velocity.x = 0f
                return@add
            }

            val runningMetIter = runningMetsSet.iterator()
            while (runningMetIter.hasNext()) {
                val met = runningMetIter.next()
                if (met.body.getMaxX() <= killerWall.getMaxX()) met.kill()
                if (met.dead) runningMetIter.remove()
            }

            val flyingMetIter = flyingMetsSet.iterator()
            while (flyingMetIter.hasNext()) {
                val met = flyingMetIter.next()
                if (met.dead) flyingMetIter.remove()
            }

            if (!tankBlock!!.body.getCenter()
                    .epsilonEquals(body.getCenter().add(tankBlockOffset), 0.01f) || !bodyBlock!!.body.getCenter()
                    .epsilonEquals(body.getCenter().add(bodyBlockOffset), 0.01f)
            ) {
                tankBlock!!.body.setCenter(body.getCenter().add(tankBlockOffset))
                bodyBlock!!.body.setCenter(body.getCenter().add(bodyBlockOffset))
            }

            if (attackState == null && reachedFrontFirstTime) {
                attackDelayTimer.update(delta)
                if (attackDelayTimer.isFinished()) {
                    val attack = ATTACK_STATES.random()
                    if (attack != GutsTankAttackState.LAUNCH_FIST) attackState = attack
                    if (attack == GutsTankAttackState.SHOOT_BLASTS) requestToPlaySound(
                        SoundAsset.MM2_MECHA_DRAGON_SOUND, false
                    )
                    val newDuration = ATTACK_DELAY_MIN + (ATTACK_DELAY_MAX - ATTACK_DELAY_MIN) * getHealthRatio()
                    attackDelayTimer.resetDuration(newDuration)
                }
            }

            if (fist?.dead == true) fist = null
            if (fist?.state == GutsTankFist.GutsTankFistState.ATTACHED) {
                launchFistDelayTimer.update(delta)
                if (launchFistDelayTimer.isFinished() && !fist!!.body.overlaps(getMegaman().body as Rectangle)) {
                    launchFistDelayTimer.reset()
                    fist!!.launch()
                }
            }

            when (attackState) {
                GutsTankAttackState.CHUNK_BULLETS -> {
                    bulletChunkDelayTimer.update(delta)
                    if (bulletChunkDelayTimer.isFinished()) {
                        requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
                        val bullet = EntityFactories.fetch(EntityType.PROJECTILE, "Bullet")!!
                        val position = body.getCenter().add(-1.65f * ConstVals.PPM, 1.85f * ConstVals.PPM)
                        val xFactor = 1f - ((abs(getMegaman().body.y - position.y) / ConstVals.PPM) / 10f) + 0.2f
                        val impulseX = (getMegaman().body.x - position.x) * xFactor
                        game.engine.spawn(
                            bullet, props(
                                ConstKeys.POSITION to position,
                                ConstKeys.TRAJECTORY to Vector2(impulseX, CHUNKED_BULLET_VELOCITY_Y * ConstVals.PPM),
                                ConstKeys.GRAVITY to Vector2(0f, CHUNKED_BULLET_GRAVITY * ConstVals.PPM),
                                ConstKeys.OWNER to this,
                                ConstKeys.ON_DAMAGE_INFLICTED_TO to { damageable: IDamageable ->
                                    if (damageable is Megaman) laugh()
                                },
                                ConstKeys.CULL_OUT_OF_BOUNDS to false
                            )
                        )
                        bulletsChunked++
                        bulletChunkDelayTimer.reset()
                        if (bulletsChunked >= BULLETS_TO_CHUNK) finishAttack(attackState!!)
                    }
                }

                GutsTankAttackState.SHOOT_BLASTS -> {
                    blastShootDelayTimer.update(delta)
                    if (blastShootDelayTimer.isFinished()) {
                        requestToPlaySound(SoundAsset.MM2_MECHA_DRAGON_SOUND, false)
                        val blast = EntityFactories.fetch(EntityType.PROJECTILE, "PurpleBlast")!!
                        val angle = BLAST_ANGLES.random()
                        val trajectory = Vector2(BLAST_VELOCITY * ConstVals.PPM, 0f).setAngleDeg(angle)
                        game.engine.spawn(
                            blast, props(
                                ConstKeys.POSITION to body.getCenter().add(
                                    -1.65f * ConstVals.PPM, 1.5f * ConstVals.PPM
                                ),
                                ConstKeys.TRAJECTORY to trajectory,
                                ConstKeys.FACING to Facing.LEFT,
                                ConstKeys.OWNER to this,
                                ConstKeys.CULL_OUT_OF_BOUNDS to false
                            )
                        )
                        blastsShot++
                        blastShootDelayTimer.reset()
                        if (blastsShot >= BLASTS_TO_SHOOT) finishAttack(attackState!!)
                    }
                }

                GutsTankAttackState.LAUNCH_RUNNING_METS -> {
                    if (runningMetsSet.size >= RUNNING_METS_TO_LAUNCH) finishAttack(attackState!!)
                    else {
                        runningMetDelayTimer.update(delta)
                        if (runningMetDelayTimer.isFinished()) {
                            requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
                            val runningMet = EntityFactories.fetch(EntityType.ENEMY, "Met")!!
                            game.engine.spawn(
                                runningMet, props(
                                    ConstKeys.POSITION to Vector2(
                                        bodyBlock!!.body.x - (0.75f * ConstVals.PPM),
                                        tankBlock!!.body.getMaxY() + (0.25f * ConstVals.PPM)
                                    ),
                                    ConstKeys.RIGHT to false,
                                    Met.RUN_ONLY to true,
                                    ConstKeys.ON_DAMAGE_INFLICTED_TO to { damageable: IDamageable ->
                                        if (damageable is Megaman) laugh()
                                    },
                                    ConstKeys.DROP_ITEM_ON_DEATH to false,
                                    ConstKeys.CULL_OUT_OF_BOUNDS to false
                                )
                            )
                            runningMetsSet.add(runningMet as Met)
                            runningMetDelayTimer.reset()
                            runningMets++
                            if (runningMetsSet.size >= RUNNING_METS_TO_LAUNCH || runningMets >= RUNNING_METS_TO_LAUNCH)
                                finishAttack(attackState!!)
                        }
                    }
                }

                GutsTankAttackState.LAUNCH_FLYING_METS -> {
                    if (flyingMetsSet.size >= FLYING_METS_TO_LAUNCH) finishAttack(attackState!!)
                    else {
                        flyingMetDelayTimer.update(delta)
                        if (flyingMetDelayTimer.isFinished()) {
                            requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
                            val flyingMet = EntityFactories.fetch(EntityType.ENEMY, "HeliMet")!! as HeliMet
                            val target = flyingMetTargets.get(flyingMetsSet.size)
                            game.engine.spawn(
                                flyingMet, props(
                                    ConstKeys.POSITION to Vector2(
                                        bodyBlock!!.body.x - (0.75f * ConstVals.PPM),
                                        tankBlock!!.body.getMaxY() + (0.65f * ConstVals.PPM)
                                    ),
                                    ConstKeys.FACING to Facing.LEFT,
                                    ConstKeys.TARGET to target,
                                    ConstKeys.ON_DAMAGE_INFLICTED_TO to { damageable: IDamageable ->
                                        if (damageable is Megaman) laugh()
                                    },
                                    ConstKeys.DROP_ITEM_ON_DEATH to false,
                                    ConstKeys.CULL_OUT_OF_BOUNDS to false
                                )
                            )
                            flyingMetsSet.add(flyingMet)
                            flyingMetDelayTimer.reset()
                            flyingMets++
                            if (flyingMetsSet.size >= FLYING_METS_TO_LAUNCH || flyingMets >= FLYING_METS_TO_LAUNCH) finishAttack(
                                attackState!!
                            )
                        }
                    }
                }

                GutsTankAttackState.DO_NOTHING -> {
                    doNothingTimer.update(delta)
                    if (doNothingTimer.isFinished()) finishAttack(attackState!!)
                }

                else -> {}
            }

            when (moveState) {
                GutsTankMoveState.MOVE -> {
                    if (moveToFront) {
                        val xVel = MIN_X_VEL + (MAX_X_VEL - MIN_X_VEL) * (1f - getHealthRatio())
                        body.physics.velocity.x = -xVel * ConstVals.PPM
                        tankBlock!!.body.physics.velocity.x = -xVel * ConstVals.PPM
                        bodyBlock!!.body.physics.velocity.x = -xVel * ConstVals.PPM
                        if (body.x <= frontPoint.x) {
                            body.x = frontPoint.x
                            body.physics.velocity.x = 0f
                            moveState = GutsTankMoveState.PAUSE
                            moveToFront = false
                            movementPauseTimer.reset()
                        }
                    } else {
                        val xVel = MIN_X_VEL + (MAX_X_VEL - MIN_X_VEL) * (1f - getHealthRatio())
                        reachedFrontFirstTime = true
                        body.physics.velocity.x = xVel * ConstVals.PPM
                        tankBlock!!.body.physics.velocity.x = xVel * ConstVals.PPM
                        bodyBlock!!.body.physics.velocity.x = xVel * ConstVals.PPM
                        if (body.x >= backPoint.x) {
                            body.x = backPoint.x
                            body.physics.velocity.x = 0f
                            moveState = GutsTankMoveState.PAUSE
                            moveToFront = true
                            movementPauseTimer.reset()
                        }

                    }
                }

                GutsTankMoveState.PAUSE -> {
                    body.physics.velocity.x = 0f
                    tankBlock!!.body.physics.velocity.x = 0f
                    bodyBlock!!.body.physics.velocity.x = 0f
                    movementPauseTimer.update(delta)
                    if (movementPauseTimer.isFinished()) moveState = GutsTankMoveState.MOVE
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        val damageableFixture = Fixture(
            body,
            FixtureType.DAMAGEABLE,
            GameRectangle().setSize(1.15f * ConstVals.PPM, 0.85f * ConstVals.PPM)
        )
        damageableFixture.offsetFromBodyCenter.x = -1.45f * ConstVals.PPM
        damageableFixture.offsetFromBodyCenter.y = 2.35f * ConstVals.PPM
        body.addFixture(damageableFixture)
        damageableFixture.getShape().color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(2f * ConstVals.PPM))
        damagerFixture.offsetFromBodyCenter.x = -1f * ConstVals.PPM
        damagerFixture.offsetFromBodyCenter.y = 2.5f * ConstVals.PPM
        body.addFixture(damagerFixture)
        damagerFixture.getShape().color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
        sprite.setSize(8f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomLeftPoint(), Position.BOTTOM_LEFT)
            _sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    internal fun finishAttack(attackState: GutsTankAttackState) {
        when (attackState) {
            GutsTankAttackState.CHUNK_BULLETS -> {
                bulletsChunked = 0
                bulletChunkDelayTimer.reset()
            }

            GutsTankAttackState.SHOOT_BLASTS -> {
                blastsShot = 0
                blastShootDelayTimer.reset()
            }

            GutsTankAttackState.LAUNCH_FIST -> launchFistDelayTimer.reset()

            GutsTankAttackState.LAUNCH_RUNNING_METS -> {
                runningMetDelayTimer.reset()
                runningMets = 0
            }

            GutsTankAttackState.LAUNCH_FLYING_METS -> {
                flyingMetDelayTimer.reset()
                flyingMets = 0
            }

            GutsTankAttackState.DO_NOTHING -> doNothingTimer.reset()
        }
        this.attackState = null
    }

    internal fun laugh() {
        laughTimer.reset()
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            if (laughing) "laughing"
            else if (attackState == GutsTankAttackState.CHUNK_BULLETS || attackState == GutsTankAttackState.SHOOT_BLASTS) "mouth_open"
            else "mouth_closed"
        }
        val animations = objectMapOf<String, IAnimation>(
            "laughing" to Animation(laughingRegion!!, 1, 2, 0.1f, true),
            "mouth_open" to Animation(mouthOpenRegion!!, 1, 2, 0.1f, true),
            "mouth_closed" to Animation(mouthClosedRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}