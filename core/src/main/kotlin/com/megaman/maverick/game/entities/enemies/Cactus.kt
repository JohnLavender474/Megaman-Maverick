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
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.state.EnumStateMachineBuilder
import com.mega.game.engine.state.StateMachine
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.IDamageNegotiator
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.*
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.explosions.IceShard
import com.megaman.maverick.game.entities.explosions.SpreadExplosion
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.entities.projectiles.*
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class Cactus(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFreezableEntity, IFaceable {

    companion object {
        const val TAG = "Cactus"

        private const val TURN_DUR = 0.3f
        private const val FLASH_DUR = 1.25f
        private const val FACING_DUR = 2.5f
        private const val FROZEN_DUR = 1f

        private const val NEEDLES = 5
        private const val NEEDLE_GRAV = -0.1f
        private const val NEEDLE_IMPULSE = 10f
        private const val NEEDLE_Y_OFFSET = 0.1f

        private const val SCANNER_RADIUS = 8f

        private val angles = gdxArrayOf(80f, 45f, 0f, 315f, 280f)
        private val xOffsets = gdxArrayOf(-0.2f, -0.1f, 0f, 0.1f, 0.2f)

        private val damagers = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
            Bullet::class pairTo dmgNeg(10),
            ArigockBall::class pairTo dmgNeg(10),
            CactusMissile::class pairTo dmgNeg(10),
            ChargedShot::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            ChargedShotExplosion::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            SmallGreenMissile::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            Explosion::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            Spiky::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            SpreadExplosion::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            MoonScythe::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            SmallGreenMissile::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            SmallIceCube::class pairTo dmgNeg(5)
        )

        private val animDefs = orderedMapOf(
            "idle" pairTo AnimationDef(),
            "turn_left" pairTo AnimationDef(3, 1, 0.1f, false),
            "turn_right" pairTo AnimationDef(3, 1, 0.1f, false),
            "left" pairTo AnimationDef(1, 2, gdxArrayOf(1f, 0.15f), true),
            "right" pairTo AnimationDef(1, 2, gdxArrayOf(1f, 0.15f), true),
            "flash_left" pairTo AnimationDef(1, 3, 0.05f, true),
            "flash_right" pairTo AnimationDef(1, 3, 0.05f, true),
            "frozen" pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class CactusState { IDLE, TURN, FACING, FLASH, FROZEN }

    private enum class CactusType { BIG, SMALL }

    override val damageNegotiator: IDamageNegotiator = object : IDamageNegotiator {

        override fun get(damager: IDamager) = damagers[damager::class]?.get(damager) ?: 0
    }

    override var frozen: Boolean
        get() = !stateTimers[CactusState.FROZEN].isFinished()
        set(value) {
            GameLogger.debug(TAG, "frozen.set: value=$value")

            if (value) {
                stateTimers[CactusState.FROZEN].reset()
                if (currentState != CactusState.FROZEN) stateMachine.next()
            } else {
                stateTimers[CactusState.FROZEN].setToEnd()
                if (currentState == CactusState.FROZEN) stateMachine.next()
            }
        }
    override lateinit var facing: Facing

    private lateinit var stateMachine: StateMachine<CactusState>
    private val currentState: CactusState
        get() = stateMachine.getCurrent()
    private val stateTimers = orderedMapOf(
        CactusState.TURN pairTo Timer(TURN_DUR),
        CactusState.FLASH pairTo Timer(FLASH_DUR),
        CactusState.FACING pairTo Timer(FACING_DUR),
        CactusState.FROZEN pairTo Timer(FROZEN_DUR)
    )

    private lateinit var type: CactusType

    private val scanner = GameCircle().setRadius(SCANNER_RADIUS * ConstVals.PPM).also { it.drawingColor = Color.GRAY }

    override fun init() {
        GameLogger.debug(TAG, "init()")

        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            CactusType.entries.forEach { type ->
                animDefs.keys().forEach { key ->
                    val fullKey = "${type.name.lowercase()}/$key"
                    regions.put(fullKey, atlas.findRegion("$TAG/$fullKey"))
                }
            }
        }

        super.init()

        stateMachine = buildStateMachine()

        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val big = spawnProps.getOrDefault(ConstKeys.BIG, true, Boolean::class)
        type = if (big) CactusType.BIG else CactusType.SMALL

        body.setHeight((if (big) 2.5f else 1.5f) * ConstVals.PPM)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        stateMachine.reset()
        stateTimers.values().forEach { it.reset() }

        frozen = false

        FacingUtils.setFacingOf(this)
    }

    override fun onHealthDepleted() {
        spawnNeedles()
        super.onHealthDepleted()
        playSoundNow(SoundAsset.THUMP_SOUND, false)
    }

    override fun canBeDamagedBy(damager: IDamager) = damager is SmallGreenMissile || super.canBeDamagedBy(damager)

    override fun takeDamageFrom(damager: IDamager): Boolean {
        GameLogger.debug(TAG, "takeDamageFrom(): damager=$damager")
        val damaged = super.takeDamageFrom(damager)
        if (damaged) when {
            damager is IFreezerEntity && !frozen -> frozen = true
            damager is IFireEntity && frozen -> frozen = false
        }
        return damaged
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            scanner.setCenter(body.getCenter())

            if (stateTimers.containsKey(currentState)) {
                val stateTimer = stateTimers[currentState]!!
                stateTimer.update(delta)
                if (stateTimer.isFinished()) {
                    GameLogger.debug(TAG, "update(): currentState=$currentState, state timer is finished")
                    stateMachine.next()
                }
            }

            when (currentState) {
                CactusState.IDLE -> if (isMegamanInScanner()) {
                    GameLogger.debug(TAG, "update(): currentState=$currentState, megaman is in scanner")
                    stateMachine.next()
                }

                CactusState.FACING -> if (!isMegamanInScanner() || shouldTurn()) {
                    GameLogger.debug(TAG, "update(): currentState=$currentState, megaman not in scanner OR should turn")
                    stateMachine.next()
                }

                else -> {}
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setWidth(ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }
        debugShapes.add { scanner }

        val frozenFixture = Fixture(body, FixtureType.SHIELD, GameRectangle())
        body.addFixture(frozenFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.forEachFixture { ((it as Fixture).rawShape as GameRectangle).set(body) }
            frozenFixture.setActive(frozen)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.Companion.of(
                FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE
            )
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
                .also { sprite -> sprite.setSize(2f * ConstVals.PPM, 3f * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)

            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier keySupplier@{
                    val part1 = type.name.lowercase()
                    val part2 = when (currentState) {
                        CactusState.IDLE -> "idle"
                        CactusState.TURN -> "turn_${facing.opposite().name.lowercase()}"
                        CactusState.FLASH -> "flash_${facing.name.lowercase()}"
                        CactusState.FROZEN -> "frozen"
                        else -> facing.name.lowercase()
                    }
                    return@keySupplier "$part1/$part2"
                }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val (rows, columns, durations, loop) = entry.value
                        CactusType.entries.forEach { type ->
                            val fullKey = "${type.name.lowercase()}/$key"
                            try {
                                animations.put(fullKey, Animation(regions[fullKey], rows, columns, durations, loop))
                            } catch (e: Exception) {
                                val regionsLog = regions.entries().map map@{ entry ->
                                    val key = entry.key
                                    val defined = entry.value != null
                                    return@map key pairTo defined
                                }
                                throw Exception("Failed to put animation: fullKey=$fullKey, regions=$regionsLog", e)
                            }
                        }
                    }
                }
                .build()
        )
        .build()

    private fun buildStateMachine() = EnumStateMachineBuilder.create<CactusState>()
        .setOnChangeState(this::onChangeState)
        .initialState(CactusState.IDLE)
        // idle
        .transition(CactusState.IDLE, CactusState.FROZEN) { frozen }
        .transition(CactusState.IDLE, CactusState.IDLE) { !isMegamanInScanner() }
        .transition(CactusState.IDLE, CactusState.TURN) { shouldTurn() }
        .transition(CactusState.IDLE, CactusState.FACING) { true }
        // turn
        .transition(CactusState.TURN, CactusState.FROZEN) { frozen }
        .transition(CactusState.TURN, CactusState.FACING) { true }
        // facing
        .transition(CactusState.FACING, CactusState.FROZEN) { frozen }
        .transition(CactusState.FACING, CactusState.IDLE) { !isMegamanInScanner() }
        .transition(CactusState.FACING, CactusState.TURN) { shouldTurn() }
        .transition(CactusState.FACING, CactusState.FLASH) { true }
        // flash
        .transition(CactusState.FLASH, CactusState.FROZEN) { frozen }
        .transition(CactusState.FLASH, CactusState.IDLE) { !isMegamanInScanner() }
        .transition(CactusState.FLASH, CactusState.TURN) { shouldTurn() }
        .transition(CactusState.FLASH, CactusState.FACING) { true }
        // frozen
        .transition(CactusState.FROZEN, CactusState.IDLE) { !isMegamanInScanner() }
        .transition(CactusState.FROZEN, CactusState.FACING) { true }
        // build
        .build()

    private fun onChangeState(current: CactusState, previous: CactusState) {
        GameLogger.debug(TAG, "onChangeState(): current=$current, previous=$previous")

        stateTimers[current]?.reset()
        if (previous != CactusState.FROZEN) stateTimers[previous]?.reset()

        if (current == CactusState.FACING && previous == CactusState.TURN) {
            GameLogger.debug(TAG, "onChangeState(): swap facing")
            swapFacing()
        }

        if (previous == CactusState.FLASH) {
            GameLogger.debug(TAG, "onChangeState(): spawn needles")
            spawnNeedles()
        } else if (previous == CactusState.FROZEN) {
            IceShard.spawn5(body.getCenter())
            damageTimer.reset()
        }
    }

    private fun shouldTurn() = facing != FacingUtils.getPreferredFacingFor(this)

    private fun isMegamanInScanner() = megaman.body.getBounds().overlaps(scanner)

    private fun spawnNeedles() {
        val indexStep = when (type) {
            CactusType.BIG -> 1
            CactusType.SMALL -> 2
        }

        for (i in 0 until NEEDLES step indexStep) {
            val xOffset = xOffsets[i]

            val position = body.getCenter().add(xOffset * ConstVals.PPM, NEEDLE_Y_OFFSET * ConstVals.PPM)

            val angle = angles[i]

            val impulse = GameObjectPools.fetch(Vector2::class)
                .set(0f, NEEDLE_IMPULSE * ConstVals.PPM)
                .rotateDeg(angle)

            GameLogger.debug(TAG, "spawnNeedles(): i=$i, position=$position, impulse=$impulse")

            val needle = MegaEntityFactory.fetch(Needle::class)!!
            needle.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.IMPULSE pairTo impulse,
                    ConstKeys.POSITION pairTo position,
                    ConstKeys.GRAVITY pairTo NEEDLE_GRAV * ConstVals.PPM
                )
            )
        }
    }

    override fun getTag() = TAG
}
