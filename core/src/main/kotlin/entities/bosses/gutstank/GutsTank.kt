package com.megaman.maverick.game.entities.bosses.gutstank

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.*
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
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
import com.megaman.maverick.game.entities.projectiles.PurpleBlast
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.MegaUtilMethods.pooledProps
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureLabel
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getPositionPoint
import kotlin.reflect.KClass

class GutsTank(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity {

    enum class GutsTankMoveState { PAUSE, MOVE, }
    enum class GutsTankAttackState { LAUNCH_FIST, LAUNCH_RUNNING_METS, LAUNCH_HELI_METS, CHUNK_BULLETS, SHOOT_BLASTS }

    companion object {
        const val TAG = "GutsTank"

        private val ATTACK_STATES = GutsTankAttackState.entries.toTypedArray()

        private const val KILLER_WALL = "killer_wall"

        private const val FLY1 = "fly1"
        private const val FLY2 = "fly2"

        private const val BODY_WIDTH = 8f
        private const val BODY_HEIGHT = 4.46875f

        private const val TANK_BLOCK_HEIGHT = 2.0365f

        private const val MIN_X_VEL = 1.5f
        private const val MAX_X_VEL = 3f
        private const val MOVEMENT_PAUSE_DUR = 1.5f

        private const val ATTACK_DELAY_MIN = 1f
        private const val ATTACK_DELAY_MAX = 2f

        private const val BULLETS_TO_CHUNK = 4
        private const val CHUNKED_BULLET_GRAVITY = -0.1f
        private const val CHUNKED_BULLET_VELOCITY_Y = 10f
        private const val BULLET_CHUNK_DELAY = 0.25f

        private const val BLASTS_TO_SHOOT = 3
        private const val BLAST_SHOOT_DELAY = 0.35f
        private const val BLAST_VELOCITY = 5f
        private val BLAST_ANGLES = gdxArrayOf(180f, 190f, 200f, 210f, 220f)

        private const val RUNNING_METS_TO_LAUNCH = 3
        private const val RUNNING_MET_DELAY = 1f

        private const val HELI_METS_TO_LAUNCH = 2
        private const val HELI_MET_DELAY = 1f

        private const val LAUNCH_FIST_DELAY = 10f
        private const val LAUGH_DUR = 1.25f

        private var laughingRegion: TextureRegion? = null
        private var mouthOpenRegion: TextureRegion? = null
        private var mouthClosedRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(1),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 2 else 1
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 2 else 1
        }
    )

    internal var tankBlock: Block? = null
    internal val runningMets = Array<Met>()
    internal val heliMets = Array<HeliMet>()

    private val attackDelayTimer = Timer(ATTACK_DELAY_MAX)
    private val movementPauseTimer = Timer(MOVEMENT_PAUSE_DUR)
    private val bulletChunkDelayTimer = Timer(BULLET_CHUNK_DELAY)
    private val blastShootDelayTimer = Timer(BLAST_SHOOT_DELAY)
    private val runningMetDelayTimer = Timer(RUNNING_MET_DELAY)
    private val heliMetDelayTimer = Timer(HELI_MET_DELAY)
    private val launchFistDelayTimer = Timer(LAUNCH_FIST_DELAY)
    private val laughTimer = Timer(LAUGH_DUR)

    private val heliMetTargets = Array<Vector2>()
    private val laughing: Boolean
        get() = !laughTimer.isFinished()
    private val metSpawn: Vector2
        get() = Vector2(
            body.getCenter().x - 1.25f * ConstVals.PPM,
            tankBlock!!.body.getMaxY() + 0.75f * ConstVals.PPM
        )

    private lateinit var frontPoint: Vector2
    private lateinit var backPoint: Vector2
    private lateinit var tankBlockOffset: Vector2
    private lateinit var moveState: GutsTankMoveState
    private lateinit var killerWall: GameRectangle
    private lateinit var blastAngles: OrderedSet<Float>

    private var attackState: GutsTankAttackState? = null
    private var fist: GutsTankFist? = null
    private var reachedFrontFirstTime = false
    private var moveToFront = true
    private var bulletsChunked = 0
    private var blastsShot = 0
    private var runningMetsLaunched = 0
    private var heliMetsLaunched = 0

    override fun init() {
        if (laughingRegion == null || mouthOpenRegion == null || mouthClosedRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
            laughingRegion = atlas.findRegion("$TAG/Laughing")
            mouthOpenRegion = atlas.findRegion("$TAG/MouthOpen")
            mouthClosedRegion = atlas.findRegion("$TAG/MouthClosed")
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_LEFT)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.setBottomLeftToPoint(spawn)

        killerWall = spawnProps.get(KILLER_WALL, RectangleMapObject::class)!!.rectangle.toGameRectangle()

        tankBlock = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.STANDARD)!! as Block
        val tankBlockBounds =
            GameRectangle(spawn.x, spawn.y, BODY_WIDTH * ConstVals.PPM, TANK_BLOCK_HEIGHT * ConstVals.PPM)
        tankBlock!!.spawn(
            props(
                ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
                ConstKeys.BOUNDS pairTo tankBlockBounds,
                ConstKeys.FIXTURE_LABELS pairTo objectSetOf(FixtureLabel.NO_PROJECTILE_COLLISION),
                ConstKeys.FIXTURES pairTo gdxArrayOf(GamePair(FixtureType.SHIELD, props()))
            )
        )
        tankBlockOffset = tankBlockBounds.getCenter().sub(body.getCenter())

        fist = GutsTankFist(game)
        fist!!.spawn(pooledProps(ConstKeys.PARENT pairTo this))

        frontPoint = spawnProps.get(
            ConstKeys.FRONT,
            RectangleMapObject::class
        )!!.rectangle.getPosition(GameObjectPools.fetch(Vector2::class))
        backPoint = spawnProps.get(
            ConstKeys.BACK,
            RectangleMapObject::class
        )!!.rectangle.getPosition(GameObjectPools.fetch(Vector2::class))

        heliMetTargets.clear()
        val fly1Target = spawnProps.get(FLY1, RectangleMapObject::class)!!.rectangle.getCenter()
        val fly2Target = spawnProps.get(FLY2, RectangleMapObject::class)!!.rectangle.getCenter()
        heliMetTargets.addAll(fly1Target, fly2Target)

        moveState = GutsTankMoveState.MOVE
        moveToFront = true
        reachedFrontFirstTime = false

        attackState = null
        attackDelayTimer.resetDuration(ATTACK_DELAY_MAX)

        movementPauseTimer.reset()

        bulletChunkDelayTimer.reset()
        bulletsChunked = 0

        blastShootDelayTimer.setToEnd()
        blastsShot = 0

        launchFistDelayTimer.reset()
        laughTimer.setToEnd()

        runningMetsLaunched = 0
        heliMetsLaunched = 0
    }

    override fun isReady(delta: Float) = true // TODO

    override fun onDestroy() {
        super.onDestroy()

        runningMets.forEach { it.destroy() }
        runningMets.clear()

        heliMets.forEach { it.destroy() }
        heliMets.clear()

        fist?.destroy()
        fist = null

        tankBlock?.destroy()
        tankBlock = null
    }

    override fun triggerDefeat() {
        super.triggerDefeat()

        moveState = GutsTankMoveState.PAUSE

        runningMets.forEach { it.setHealth(0) }
        runningMets.clear()

        heliMets.forEach { it.setHealth(0) }
        heliMets.clear()

        fist?.setHealth(0)
        fist = null

        tankBlock?.destroy()
        tankBlock = null
    }

    override fun canBeDamagedBy(damager: IDamager): Boolean {
        if (damager is AbstractProjectile) {
            damager.owner?.let {
                if (it == this || it == fist || it == tankBlock ||
                    (it is Met && runningMets.contains(it)) ||
                    (it is HeliMet && heliMets.contains(it))
                ) return false
            }
        }
        return super.canBeDamagedBy(damager)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            val runningMetIter = runningMets.iterator()
            while (runningMetIter.hasNext()) {
                val met = runningMetIter.next()
                if (met.dead) runningMetIter.remove()
                else if (met.body.getMaxX() <= killerWall.getMaxX()) met.destroy()
            }

            val heliMetIter = heliMets.iterator()
            while (heliMetIter.hasNext()) {
                val met = heliMetIter.next()
                if (met.dead) heliMetIter.remove()
            }

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
                return@add
            }

            if (!tankBlock!!.body.getCenter()
                    .epsilonEquals(body.getCenter().add(tankBlockOffset), 0.01f)
            ) tankBlock!!.body.setCenter(body.getCenter().add(tankBlockOffset))

            if (attackState == null && reachedFrontFirstTime) {
                attackDelayTimer.update(delta)
                if (attackDelayTimer.isFinished()) {
                    val attack = ATTACK_STATES.random()
                    if (attack != GutsTankAttackState.LAUNCH_FIST) attackState = attack
                    if (attack == GutsTankAttackState.SHOOT_BLASTS) blastAngles = BLAST_ANGLES.toOrderedSet()
                    val newDuration = ATTACK_DELAY_MIN + (ATTACK_DELAY_MAX - ATTACK_DELAY_MIN) * getHealthRatio()
                    attackDelayTimer.resetDuration(newDuration)
                }
            }

            if (fist != null && fist!!.dead) fist = null
            if (fist?.fistState == GutsTankFist.GutsTankFistState.ATTACHED) {
                launchFistDelayTimer.update(delta)
                if (launchFistDelayTimer.isFinished() &&
                    !fist!!.body.getBounds().overlaps(megaman().body.getBounds())
                ) {
                    launchFistDelayTimer.reset()
                    fist!!.launch()
                }
            }

            when (attackState) {
                GutsTankAttackState.CHUNK_BULLETS -> {
                    bulletChunkDelayTimer.update(delta)
                    if (bulletChunkDelayTimer.isFinished()) {
                        requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
                        val bullet = EntityFactories.fetch(EntityType.PROJECTILE, Bullet.TAG)!!
                        val spawn = body.getCenter().add(-1.65f * ConstVals.PPM, 1.85f * ConstVals.PPM)
                        val trajectory = MegaUtilMethods.calculateJumpImpulse(
                            spawn,
                            megaman().body.getCenter(),
                            CHUNKED_BULLET_VELOCITY_Y * ConstVals.PPM
                        )
                        bullet.spawn(
                            props(
                                ConstKeys.OWNER pairTo this,
                                ConstKeys.POSITION pairTo spawn,
                                ConstKeys.TRAJECTORY pairTo trajectory,
                                ConstKeys.GRAVITY pairTo Vector2(0f, CHUNKED_BULLET_GRAVITY * ConstVals.PPM),
                                ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
                                ConstKeys.ON_DAMAGE_INFLICTED_TO pairTo { damageable: IDamageable ->
                                    if (damageable is Megaman) laugh()
                                }
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
                        requestToPlaySound(SoundAsset.BASSY_BLAST_SOUND, false)

                        val randomIndex = getRandom(0, blastAngles.size - 1)
                        val angle = blastAngles.removeIndex(randomIndex)
                        val trajectory = Vector2(BLAST_VELOCITY * ConstVals.PPM, 0f).setAngleDeg(angle)

                        val blast = EntityFactories.fetch(EntityType.PROJECTILE, PurpleBlast.TAG)!!
                        blast.spawn(
                            props(
                                ConstKeys.POSITION pairTo body.getCenter().add(
                                    -1.65f * ConstVals.PPM, 1.5f * ConstVals.PPM
                                ),
                                ConstKeys.TRAJECTORY pairTo trajectory,
                                ConstKeys.FACING pairTo Facing.LEFT,
                                ConstKeys.OWNER pairTo this,
                                ConstKeys.CULL_OUT_OF_BOUNDS pairTo false
                            )
                        )

                        blastsShot++
                        blastShootDelayTimer.reset()

                        if (blastsShot >= BLASTS_TO_SHOOT) finishAttack(attackState!!)
                    }
                }

                GutsTankAttackState.LAUNCH_RUNNING_METS -> {
                    if (runningMetsLaunched >= RUNNING_METS_TO_LAUNCH ||
                        runningMets.size >= RUNNING_METS_TO_LAUNCH
                    ) finishAttack(attackState!!)
                    else {
                        runningMetDelayTimer.update(delta)
                        if (runningMetDelayTimer.isFinished()) {
                            requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
                            val runningMet = EntityFactories.fetch(EntityType.ENEMY, Met.TAG)!! as Met
                            runningMet.spawn(
                                props(
                                    Met.RUN_ONLY pairTo true,
                                    ConstKeys.POSITION pairTo metSpawn,
                                    ConstKeys.RIGHT pairTo false,
                                    ConstKeys.ON_DAMAGE_INFLICTED_TO pairTo { damageable: IDamageable ->
                                        if (damageable is Megaman) laugh()
                                    },
                                    ConstKeys.DROP_ITEM_ON_DEATH pairTo false,
                                    ConstKeys.CULL_OUT_OF_BOUNDS pairTo false
                                )
                            )
                            runningMets.add(runningMet)
                            runningMetsLaunched++
                            runningMetDelayTimer.reset()
                        }
                    }
                }

                GutsTankAttackState.LAUNCH_HELI_METS -> {
                    if (heliMetsLaunched >= HELI_METS_TO_LAUNCH ||
                        heliMets.size >= HELI_METS_TO_LAUNCH
                    ) finishAttack(attackState!!)
                    else {
                        heliMetDelayTimer.update(delta)
                        if (heliMetDelayTimer.isFinished()) {
                            requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
                            val heliMet = EntityFactories.fetch(EntityType.ENEMY, HeliMet.TAG)!! as HeliMet
                            val target = heliMetTargets.get(heliMets.size)
                            heliMet.spawn(
                                props(
                                    ConstKeys.POSITION pairTo metSpawn,
                                    ConstKeys.FACING pairTo Facing.LEFT,
                                    ConstKeys.TARGET pairTo target,
                                    ConstKeys.ON_DAMAGE_INFLICTED_TO pairTo { damageable: IDamageable ->
                                        if (damageable is Megaman) laugh()
                                    },
                                    ConstKeys.DROP_ITEM_ON_DEATH pairTo false,
                                    ConstKeys.CULL_OUT_OF_BOUNDS pairTo false
                                )
                            )
                            heliMets.add(heliMet)
                            heliMetsLaunched++
                            heliMetDelayTimer.reset()
                        }
                    }
                }

                else -> {}
            }

            when (moveState) {
                GutsTankMoveState.MOVE -> {
                    if (moveToFront) {
                        val xVel = MIN_X_VEL + (MAX_X_VEL - MIN_X_VEL) * (1f - getHealthRatio())
                        body.physics.velocity.x = -xVel * ConstVals.PPM
                        tankBlock!!.body.physics.velocity.x = -xVel * ConstVals.PPM
                        if (body.getX() <= frontPoint.x) {
                            body.setX(frontPoint.x)
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
                        if (body.getX() >= backPoint.x) {
                            body.setX(backPoint.x)
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
                    movementPauseTimer.update(delta)
                    if (movementPauseTimer.isFinished()) moveState = GutsTankMoveState.MOVE
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val damageableFixture = Fixture(
            body,
            FixtureType.DAMAGEABLE,
            GameRectangle().setSize(1.15f * ConstVals.PPM, 0.85f * ConstVals.PPM)
        )
        damageableFixture.offsetFromBodyAttachment.x = -1.45f * ConstVals.PPM
        damageableFixture.offsetFromBodyAttachment.y = 2.35f * ConstVals.PPM
        body.addFixture(damageableFixture)
        // debugShapes.add { damageableFixture}

        val damagerFixture1 = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(2f * ConstVals.PPM))
        damagerFixture1.offsetFromBodyAttachment.x = -ConstVals.PPM.toFloat()
        damagerFixture1.offsetFromBodyAttachment.y = 2.5f * ConstVals.PPM
        body.addFixture(damagerFixture1)

        val damagerFixture2 = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(2.5f * ConstVals.PPM))
        damagerFixture2.offsetFromBodyAttachment.y = ConstVals.PPM.toFloat()
        body.addFixture(damagerFixture2)
        debugShapes.add { damagerFixture2}

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
        sprite.setSize(8f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_LEFT), Position.BOTTOM_LEFT)
            sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    internal fun finishAttack(attackState: GutsTankAttackState) {
        when (attackState) {
            GutsTankAttackState.LAUNCH_FIST -> launchFistDelayTimer.reset()
            GutsTankAttackState.CHUNK_BULLETS -> {
                bulletsChunked = 0
                bulletChunkDelayTimer.reset()
            }

            GutsTankAttackState.SHOOT_BLASTS -> {
                blastsShot = 0
                blastShootDelayTimer.setToEnd()
            }

            GutsTankAttackState.LAUNCH_RUNNING_METS -> {
                runningMetDelayTimer.reset()
                runningMetsLaunched = 0
            }

            GutsTankAttackState.LAUNCH_HELI_METS -> {
                heliMetDelayTimer.reset()
                heliMetsLaunched = 0
            }
        }
        this.attackState = null
    }

    internal fun laugh() = laughTimer.reset()

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            if (laughing) "laughing"
            else if (attackState == GutsTankAttackState.CHUNK_BULLETS ||
                attackState == GutsTankAttackState.SHOOT_BLASTS
            ) "mouth_open"
            else "mouth_closed"
        }
        val animations = objectMapOf<String, IAnimation>(
            "laughing" pairTo Animation(laughingRegion!!, 1, 2, 0.1f, true),
            "mouth_open" pairTo Animation(mouthOpenRegion!!, 1, 2, 0.1f, true),
            "mouth_closed" pairTo Animation(mouthClosedRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
