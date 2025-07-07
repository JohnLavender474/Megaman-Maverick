package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
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
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.IDamageNegotiator
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.enemies.HeliMet
import com.megaman.maverick.game.entities.enemies.Met
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.BigAssMaverickRobotOrb
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.entities.projectiles.PurpleBlast
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.*

class GutsTank(game: MegamanMaverickGame) : AbstractBoss(game, size = Size.LARGE), IAnimatedEntity {

    enum class GutsTankMoveState { PAUSE, MOVE, }
    enum class GutsTankAttackState { LAUNCH_FIST, LAUNCH_RUNNING_METS, LAUNCH_HELI_METS, CHUNK_ORBS, SHOOT_BLASTS }

    companion object {
        const val TAG = "GutsTank"

        private val ATTACK_STATES = gdxArrayOf<GutsTankAttackState>(
            GutsTankAttackState.LAUNCH_RUNNING_METS,
            GutsTankAttackState.LAUNCH_HELI_METS,
            GutsTankAttackState.CHUNK_ORBS,
            GutsTankAttackState.SHOOT_BLASTS
        )

        private const val KILLER_WALL = "killer_wall"

        private const val FLY1 = "fly1"
        private const val FLY2 = "fly2"

        private const val BODY_WIDTH = 12f
        private const val BODY_HEIGHT = 9f

        private const val TANK_BLOCK_HEIGHT = 3f

        private const val MIN_X_VEL = 2f
        private const val MAX_X_VEL = 4f
        private const val MOVEMENT_PAUSE_DUR = 1f

        private const val INIT_DUR = 0.5f

        private const val ATTACK_DELAY_MIN = 0.75f
        private const val ATTACK_DELAY_MAX = 1.25f

        private const val BULLETS_TO_CHUNK = 4
        private const val CHUNKED_BULLET_GRAVITY = -0.1f
        private const val CHUNKED_BULLET_VELOCITY_Y = 10f
        private const val BULLET_CHUNK_DELAY = 0.25f

        private const val BLASTS_TO_SHOOT = 5
        private const val BLAST_SHOOT_MAX_DELAY = 0.75f
        private const val BLAST_SHOOT_MIN_DELAY = 0.5f
        private const val BLAST_VELOCITY = 8f
        private val BLAST_ANGLES = gdxArrayOf(180f, 190f, 200f, 210f, 220f)

        private const val RUNNING_METS_TO_LAUNCH = 3
        private const val RUNNING_MET_DELAY = 1f

        private const val HELI_METS_TO_LAUNCH = 2
        private const val HELI_MET_DELAY = 1f

        private const val LAUNCH_FIST_DELAY = 10f
        private const val LAUGH_DUR = 1f

        private val FIXTURE_LABELS = objectSetOf(FixtureLabel.NO_PROJECTILE_COLLISION)
        private val FIXTURES = gdxArrayOf(GamePair(FixtureType.SHIELD, props()))

        private val regions = ObjectMap<String, TextureRegion>()
        private val animDefs = orderedMapOf(
            "laughing" pairTo AnimationDef(2, 1, 0.1f, true),
            "moving_laughing" pairTo AnimationDef(2, 1, 0.1f, true),
            "mouth_closed" pairTo AnimationDef(),
            "mouth_open" pairTo AnimationDef(),
            "moving_mouth_closed" pairTo AnimationDef(2, 1, 0.1f, true),
            "moving_mouth_open" pairTo AnimationDef(2, 1, 0.1f, true)
        )
    }

    override val damageNegotiator = object : IDamageNegotiator {

        override fun get(damager: IDamager): Int {
            if (damager !is IProjectileEntity || damager.owner != megaman) return 0
            return when (damager) {
                // is ChargedShot -> if (damager.fullyCharged) 2 else 1
                is MoonScythe, /* is MagmaWave, */ is Axe -> 2
                else -> 1
            }
        }
    }

    internal var tankBlock: Block? = null
    internal val runningMets = Array<Met>()
    internal val heliMets = Array<HeliMet>()

    private val initTimer = Timer(INIT_DUR)
    private val attackDelayTimer = Timer(ATTACK_DELAY_MAX)
    private val movementPauseTimer = Timer(MOVEMENT_PAUSE_DUR)
    private val bulletChunkDelayTimer = Timer(BULLET_CHUNK_DELAY)
    private val blastShootDelayTimer = Timer(BLAST_SHOOT_MAX_DELAY)
    private val runningMetDelayTimer = Timer(RUNNING_MET_DELAY)
    private val heliMetDelayTimer = Timer(HELI_MET_DELAY)
    private val launchFistDelayTimer = Timer(LAUNCH_FIST_DELAY)
    private val laughTimer = Timer(LAUGH_DUR)

    private val heliMetTargets = Array<Vector2>()
    private val laughing: Boolean
        get() = !laughTimer.isFinished()
    private val metSpawn: Vector2
        get() = GameObjectPools.fetch(Vector2::class).set(
            body.getCenter().x - 2.25f * ConstVals.PPM,
            tankBlock!!.body.getMaxY() + 1f * ConstVals.PPM
        )

    private val frontPoint = Vector2()
    private val backPoint = Vector2()
    private val tankBlockOffset = Vector2()

    private val killerWall = GameRectangle()

    private lateinit var moveState: GutsTankMoveState

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
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.MUSIC, MusicAsset.MMX6_SIGMA_2_BATTLE_INTRO_MUSIC.name)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_LEFT)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.setBottomLeftToPoint(spawn)

        killerWall.set(spawnProps.get(KILLER_WALL, RectangleMapObject::class)!!.rectangle.toGameRectangle())

        tankBlock = MegaEntityFactory.fetch(Block::class)!!
        val tankBlockBounds = GameObjectPools.fetch(GameRectangle::class)
            .set(spawn.x, spawn.y, BODY_WIDTH * ConstVals.PPM, TANK_BLOCK_HEIGHT * ConstVals.PPM)
        tankBlock!!.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
                ConstKeys.BOUNDS pairTo tankBlockBounds,
                ConstKeys.FIXTURE_LABELS pairTo FIXTURE_LABELS,
                ConstKeys.FIXTURES pairTo FIXTURES,
                "${ConstKeys.FEET}_${ConstKeys.SOUND}" pairTo false
            )
        )
        tankBlockOffset.set(tankBlockBounds.getCenter().sub(body.getCenter()))

        fist = GutsTankFist(game)
        fist!!.spawn(props(ConstKeys.PARENT pairTo this))

        frontPoint.set(
            spawnProps.get(
                ConstKeys.FRONT,
                RectangleMapObject::class
            )!!.rectangle.getPosition(GameObjectPools.fetch(Vector2::class))
        )
        backPoint.set(
            spawnProps.get(
                ConstKeys.BACK,
                RectangleMapObject::class
            )!!.rectangle.getPosition(GameObjectPools.fetch(Vector2::class))
        )

        heliMetTargets.clear()
        val fly1Target = spawnProps.get(FLY1, RectangleMapObject::class)!!.rectangle.getCenter(false)
        val fly2Target = spawnProps.get(FLY2, RectangleMapObject::class)!!.rectangle.getCenter(false)
        heliMetTargets.addAll(fly1Target, fly2Target)

        moveState = GutsTankMoveState.MOVE
        moveToFront = true
        reachedFrontFirstTime = false

        attackState = null
        attackDelayTimer.resetDuration(ATTACK_DELAY_MAX)

        movementPauseTimer.reset()

        bulletChunkDelayTimer.reset()
        bulletsChunked = 0

        blastShootDelayTimer.resetDuration(BLAST_SHOOT_MAX_DELAY)
        blastsShot = 0

        launchFistDelayTimer.reset()
        laughTimer.setToEnd()

        runningMetsLaunched = 0
        heliMetsLaunched = 0

        initTimer.reset()
    }

    override fun isReady(delta: Float) = megaman.body.isSensing(BodySense.FEET_ON_GROUND) && initTimer.isFinished()

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

        runningMets.forEach { it.destroy() }
        runningMets.clear()

        heliMets.forEach { it.destroy() }
        heliMets.clear()

        fist?.destroy()
        fist = null

        tankBlock?.destroy()
        tankBlock = null
    }

    override fun canBeDamagedBy(damager: IDamager): Boolean {
        if (damager is AbstractProjectile) {
            val it = damager.owner
            if (it == this || it == fist || it == tankBlock ||
                (it is Met && runningMets.contains(it)) ||
                (it is HeliMet && heliMets.contains(it))
            ) return false
        }
        return super.canBeDamagedBy(damager)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!initTimer.isFinished() && megaman.body.isSensing(BodySense.FEET_ON_GROUND)) {
                initTimer.update(delta)
                return@add
            }

            val runningMetIter = runningMets.iterator()
            while (runningMetIter.hasNext()) {
                val met = runningMetIter.next()
                when {
                    met.dead -> runningMetIter.remove()
                    met.body.getMaxX() <= killerWall.getX() -> {
                        met.destroy()
                        runningMetIter.remove()
                    }
                }
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

            if (!tankBlock!!.body.getCenter()
                    .epsilonEquals(body.getCenter().add(tankBlockOffset), 0.01f)
            ) tankBlock!!.body.setCenter(body.getCenter().add(tankBlockOffset))

            if (attackState == null && reachedFrontFirstTime) {
                attackDelayTimer.update(delta)

                if (attackDelayTimer.isFinished()) {
                    val attack = ATTACK_STATES.random()
                    this.attackState = attack

                    if (attack == GutsTankAttackState.SHOOT_BLASTS) blastAngles = BLAST_ANGLES.toOrderedSet()

                    val newDuration = UtilMethods.interpolate(ATTACK_DELAY_MIN, ATTACK_DELAY_MAX, getHealthRatio())
                    attackDelayTimer.resetDuration(newDuration)
                }
            }

            if (fist != null && fist!!.dead) fist = null

            if (fist?.fistState == GutsTankFist.GutsTankFistState.ATTACHED) {
                launchFistDelayTimer.update(delta)

                if (launchFistDelayTimer.isFinished() &&
                    !fist!!.body.getBounds().overlaps(megaman.body.getBounds())
                ) {
                    launchFistDelayTimer.reset()
                    fist!!.launch()
                }
            }

            if (laughing) laughTimer.update(delta) else when (attackState) {
                GutsTankAttackState.CHUNK_ORBS -> {
                    bulletChunkDelayTimer.update(delta)

                    if (bulletChunkDelayTimer.isFinished()) {
                        if (bulletsChunked >= BULLETS_TO_CHUNK) finishAttack(attackState!!)
                        else {
                            requestToPlaySound(SoundAsset.BLAST_2_SOUND, false)

                            val orb = MegaEntityFactory.fetch(BigAssMaverickRobotOrb::class)!!
                            val spawn = body.getCenter().add(-2f * ConstVals.PPM, 1.25f * ConstVals.PPM)
                            val impulse = MegaUtilMethods.calculateJumpImpulse(
                                spawn,
                                megaman.body.getCenter(),
                                CHUNKED_BULLET_VELOCITY_Y * ConstVals.PPM
                            )
                            val gravity = GameObjectPools.fetch(Vector2::class)
                                .set(0f, CHUNKED_BULLET_GRAVITY * ConstVals.PPM)
                            orb.spawn(
                                props(
                                    ConstKeys.OWNER pairTo this,
                                    ConstKeys.POSITION pairTo spawn,
                                    ConstKeys.IMPULSE pairTo impulse,
                                    ConstKeys.GRAVITY pairTo gravity,
                                    ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
                                    ConstKeys.ON_DAMAGE_INFLICTED_TO pairTo { damageable: IDamageable ->
                                        if (damageable is Megaman) laugh()
                                    }
                                )
                            )
                            bulletsChunked++
                            bulletChunkDelayTimer.reset()
                        }
                    }
                }
                GutsTankAttackState.SHOOT_BLASTS -> {
                    blastShootDelayTimer.update(delta)

                    if (blastShootDelayTimer.isFinished()) {
                        if (blastsShot >= BLASTS_TO_SHOOT) finishAttack(attackState!!)
                        else {
                            requestToPlaySound(SoundAsset.BASSY_BLAST_SOUND, false)

                            val randomIndex = getRandom(0, blastAngles.size - 1)
                            val angle = blastAngles.removeIndex(randomIndex)
                            val trajectory = Vector2(BLAST_VELOCITY * ConstVals.PPM, 0f).setAngleDeg(angle)

                            val blast = MegaEntityFactory.fetch(PurpleBlast::class)!!
                            blast.spawn(
                                props(
                                    ConstKeys.POSITION pairTo body.getCenter().add(
                                        -2f * ConstVals.PPM, 1.25f * ConstVals.PPM
                                    ),
                                    ConstKeys.TRAJECTORY pairTo trajectory,
                                    ConstKeys.FACING pairTo Facing.LEFT,
                                    ConstKeys.OWNER pairTo this,
                                    ConstKeys.CULL_OUT_OF_BOUNDS pairTo false
                                )
                            )

                            blastsShot++

                            val duration =
                                UtilMethods.interpolate(BLAST_SHOOT_MIN_DELAY, BLAST_SHOOT_MAX_DELAY, getHealthRatio())
                            blastShootDelayTimer.resetDuration(duration)
                        }
                    }
                }
                GutsTankAttackState.LAUNCH_RUNNING_METS -> {
                    runningMetDelayTimer.update(delta)

                    if (runningMetDelayTimer.isFinished()) {
                        if (runningMetsLaunched >= RUNNING_METS_TO_LAUNCH ||
                            runningMets.size >= RUNNING_METS_TO_LAUNCH
                        ) finishAttack(attackState!!)
                        else {
                            requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)

                            val runningMet = MegaEntityFactory.fetch(Met::class)!!
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
                    heliMetDelayTimer.update(delta)

                    if (heliMetDelayTimer.isFinished()) {
                        if (heliMetsLaunched >= HELI_METS_TO_LAUNCH ||
                            heliMets.size >= HELI_METS_TO_LAUNCH
                        ) finishAttack(attackState!!)
                        else {
                            requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)

                            val heliMet = MegaEntityFactory.fetch(HeliMet::class)!!
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
                        val xVel = UtilMethods.interpolate(MAX_X_VEL, MIN_X_VEL, getHealthRatio())
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
                        reachedFrontFirstTime = true

                        val xVel = UtilMethods.interpolate(MAX_X_VEL, MIN_X_VEL, getHealthRatio())
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
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(2f * ConstVals.PPM))
        damageableFixture.offsetFromBodyAttachment.x = -2f * ConstVals.PPM
        damageableFixture.offsetFromBodyAttachment.y = 2f * ConstVals.PPM
        body.addFixture(damageableFixture)
        damageableFixture.drawingColor = Color.PURPLE
        debugShapes.add { damageableFixture }

        val damagerFixture1 = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(2f * ConstVals.PPM))
        damagerFixture1.offsetFromBodyAttachment.x = -2f * ConstVals.PPM
        damagerFixture1.offsetFromBodyAttachment.y = 2f * ConstVals.PPM
        body.addFixture(damagerFixture1)
        debugShapes.add { damagerFixture1 }

        val damagerFixture2 = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(2.5f * ConstVals.PPM))
        damagerFixture2.offsetFromBodyAttachment.y = 0.5f * ConstVals.PPM
        body.addFixture(damagerFixture2)
        debugShapes.add { damagerFixture2 }

        /*
        val damagerFixture3 =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.5f * ConstVals.PPM, 2f * ConstVals.PPM))
        damagerFixture3.attachedToBody = false
        body.addFixture(damagerFixture3)
        debugShapes.add { damagerFixture3 }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val damager3Bounds = damagerFixture3.rawShape as GameRectangle
            tankBlock?.body?.getBounds()?.let {
                val position = Position.CENTER_LEFT
                damager3Bounds.positionOnPoint(it.getPositionPoint(position), position)
                damager3Bounds.translate(-0.1f * ConstVals.PPM, 0f)
            }
        }
         */

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, -1))
        sprite.setSize(12f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { _, _ ->
            val position = Position.BOTTOM_LEFT
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.hidden = damageBlink
        }
        return component
    }

    internal fun finishAttack(attackState: GutsTankAttackState) {
        when (attackState) {
            GutsTankAttackState.LAUNCH_FIST -> launchFistDelayTimer.reset()
            GutsTankAttackState.CHUNK_ORBS -> {
                bulletsChunked = 0
                bulletChunkDelayTimer.reset()
            }
            GutsTankAttackState.SHOOT_BLASTS -> {
                blastsShot = 0
                blastShootDelayTimer.reset()
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

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            when {
                defeated -> "laughing"
                laughing -> when (moveState) {
                    GutsTankMoveState.MOVE -> "moving_laughing"
                    else -> "laughing"
                }
                attackState?.equalsAny(
                    GutsTankAttackState.CHUNK_ORBS,
                    GutsTankAttackState.SHOOT_BLASTS
                ) == true -> when (moveState) {
                    GutsTankMoveState.MOVE -> "moving_mouth_open"
                    else -> "mouth_open"
                }
                moveState == GutsTankMoveState.MOVE -> "moving_mouth_closed"
                else -> "mouth_closed"
            }
        }
        val animations = ObjectMap<String, IAnimation>()
        AnimationUtils.loadAnimationDefs(animDefs, animations, regions)
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    internal fun laugh() {
        attackState?.let { finishAttack(it) }
        attackDelayTimer.reset()
        laughTimer.reset()
    }
}
