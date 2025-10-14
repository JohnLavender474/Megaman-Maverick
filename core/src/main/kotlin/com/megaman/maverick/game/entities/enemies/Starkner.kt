package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IDirectional
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
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.decorations.StarknerTrailSprite
import com.megaman.maverick.game.entities.explosions.IceBombExplosion
import com.megaman.maverick.game.entities.explosions.MagmaExplosion
import com.megaman.maverick.game.entities.explosions.StarExplosion
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

class Starkner(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL, dmgDuration = DMG_DUR),
    IAnimatedEntity, IFaceable, IDirectional {

    companion object {
        const val TAG = "Starkner"
        private val ALIVE = ObjectSet<Int>()
        private const val CULL_TIME = 2f
        private const val DMG_DUR = 0.25f
        private const val ROTS_PER_SEC = 2
        private const val MAX_TARGET_TIME = 2f
        private const val TRAIL_SPRITE_DELAY = 0.1f
        private val STATE_DURS = objectMapOf(
            StarknerState.SLEEP pairTo 0.5f,
            StarknerState.BLUE pairTo 1.5f,
            StarknerState.YELLOW pairTo 1f,
            StarknerState.RED pairTo 0.75f,
            StarknerState.BLACKHOLE pairTo 2f
        )
        private val STATE_SPEEDS = orderedMapOf(
            StarknerState.BLUE pairTo 4f,
            StarknerState.YELLOW pairTo 6f,
            StarknerState.RED pairTo 8f,
        )
        private val SIZES = objectMapOf(
            StarknerState.SLEEP pairTo 0.75f,
            StarknerState.BLUE pairTo 0.75f,
            StarknerState.YELLOW pairTo 0.75f,
            StarknerState.RED pairTo 0.75f,
            StarknerState.BLACKHOLE pairTo 3f
        )
        private const val BLACKHOLE_FADE_DUR = 0.2f
        private val animDefs = orderedMapOf(
            "sleep" pairTo AnimationDef(),
            "blue" pairTo AnimationDef(),
            "yellow" pairTo AnimationDef(2, 1, 0.1f, true),
            "red" pairTo AnimationDef(3, 1, 0.05f, true),
            "blackhole" pairTo AnimationDef(2, 1, 0.1f, true),
            "blackhole_appear" pairTo AnimationDef(2, 1, 0.1f, false),
            "blackhole_disappear" pairTo AnimationDef(2, 1, 0.1f, false)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class StarknerState { SLEEP, BLUE, YELLOW, RED, BLACKHOLE }

    override val invincible: Boolean
        get() = state == StarknerState.BLACKHOLE || super.invincible
    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }
    override lateinit var facing: Facing

    private lateinit var state: StarknerState
    private val stateTimer = Timer()

    private val targetTimer = Timer(MAX_TARGET_TIME)
    private val target = Vector2()
    private var moving = false

    private val trailSpriteTimer = Timer(TRAIL_SPRITE_DELAY)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun canSpawn(spawnProps: Properties): Boolean {
        if (!super.canSpawn(spawnProps)) return false

        val id = spawnProps.get(ConstKeys.ID, Int::class)!!
        return !ALIVE.contains(id)
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_TIME, CULL_TIME)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val state = spawnProps.get(ConstKeys.STATE)
        this.state = when (state) {
            is StarknerState -> state
            is String -> StarknerState.valueOf(state.uppercase())
            else -> StarknerState.SLEEP
        }
        // If not the first star that is spawned, then reset the damage timer so that the "new" star cannot instantly
        // be destroyed when spawned
        if (this.state != StarknerState.SLEEP) damageTimer.reset() else damageTimer.setToEnd()

        val duration = STATE_DURS[this.state]
        if (duration != null) stateTimer.resetDuration(duration) else stateTimer.setToEnd()

        val size = SIZES[this.state]
        body.setSize(size * ConstVals.PPM)

        val center = when {
            spawnProps.containsKey(ConstKeys.BOUNDS) ->
                spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
            else -> spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        }
        body.setCenter(center)
        target.set(center)

        targetTimer.reset()
        moving = false

        trailSpriteTimer.setToEnd()

        FacingUtils.setFacingOf(this)

        ALIVE.add(id)
        GameLogger.debug(TAG, "destroy(): added mapObjectId=$id from ALIVE: $ALIVE")
    }

    override fun destroy(): Boolean {
        ALIVE.remove(id)
        GameLogger.debug(TAG, "destroy(): removed mapObjectId=$id from ALIVE: $ALIVE")
        return super.destroy()
    }

    override fun canBeDamagedBy(damager: IDamager): Boolean {
        val result = when {
            !super.canBeDamagedBy(damager) -> false
            damager is IOwnable<*> -> {
                val owner = damager.owner
                owner !is AbstractEnemy
            }
            else -> true
        }
        GameLogger.debug(TAG, "canBeDamagedBy(): damager=$damager, result=$result")
        return result
    }

    override fun onHealthDepleted() {
        GameLogger.debug(TAG, "onHealthDepleted")
        super.onHealthDepleted()

        when (state) {
            StarknerState.SLEEP, StarknerState.BLUE -> {
                val yellowStar = MegaEntityFactory.fetch(Starkner::class)!!
                yellowStar.spawn(
                    props(
                        ConstKeys.ID pairTo id,
                        ConstKeys.POSITION pairTo body.getCenter(),
                        ConstKeys.STATE pairTo StarknerState.YELLOW
                    )
                )

                val blueExplosion = MegaEntityFactory.fetch(IceBombExplosion::class)!!
                blueExplosion.spawn(
                    props(
                        ConstKeys.OWNER pairTo this,
                        ConstKeys.ACTIVE pairTo false,
                        ConstKeys.POSITION pairTo body.getCenter(),
                    )
                )
            }
            StarknerState.YELLOW -> {
                val redStar = MegaEntityFactory.fetch(Starkner::class)!!
                redStar.spawn(
                    props(
                        ConstKeys.ID pairTo id,
                        ConstKeys.STATE pairTo StarknerState.RED,
                        ConstKeys.POSITION pairTo body.getCenter()
                    )
                )

                val yellowExplosion = MegaEntityFactory.fetch(StarExplosion::class)!!
                yellowExplosion.spawn(
                    props(
                        ConstKeys.OWNER pairTo this,
                        ConstKeys.ACTIVE pairTo false,
                        ConstKeys.POSITION pairTo body.getCenter()
                    )
                )
            }
            StarknerState.RED -> {
                val blackhole = MegaEntityFactory.fetch(Starkner::class)!!
                blackhole.spawn(
                    props(
                        ConstKeys.ID pairTo id,
                        ConstKeys.POSITION pairTo body.getCenter(),
                        ConstKeys.STATE pairTo StarknerState.BLACKHOLE
                    )
                )

                val redExplosion = MegaEntityFactory.fetch(MagmaExplosion::class)!!
                redExplosion.spawn(
                    props(
                        ConstKeys.OWNER pairTo this,
                        ConstKeys.ACTIVE pairTo false,
                        ConstKeys.POSITION pairTo body.getCenter()
                    )
                )
            }
            StarknerState.BLACKHOLE -> {}
        }
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add update@{ delta ->
            direction = megaman.direction

            when (state) {
                StarknerState.SLEEP -> {
                    stateTimer.update(delta)
                    if (stateTimer.isFinished()) {
                        GameLogger.debug(TAG, "update(): state timer finished, go to blue state")
                        state = StarknerState.BLUE
                    }
                }
                StarknerState.BLUE, StarknerState.YELLOW, StarknerState.RED -> {
                    if (megaman.dead) moving = false

                    if (moving) {
                        val speed = STATE_SPEEDS[state]
                        val trajectory = GameObjectPools.fetch(Vector2::class)
                            .set(target)
                            .sub(body.getCenter())
                            .nor()
                            .scl(speed * ConstVals.PPM)
                        body.physics.velocity.set(trajectory)

                        trailSpriteTimer.update(delta)
                        if (trailSpriteTimer.isFinished()) {
                            val trailSprite = MegaEntityFactory.fetch(StarknerTrailSprite::class)!!
                            trailSprite.spawn(
                                props(
                                    ConstKeys.STATE pairTo state,
                                    ConstKeys.POSITION pairTo body.getCenter(),
                                    ConstKeys.ROTATION pairTo sprites[TAG].rotation
                                )
                            )
                            trailSpriteTimer.reset()
                        }

                        targetTimer.update(delta)

                        if (targetTimer.isFinished() || body.getCenter().epsilonEquals(target, 0.25f * ConstVals.PPM)) {
                            GameLogger.debug(TAG, "update(): reached target=$target")
                            moving = false
                            stateTimer.reset()
                            targetTimer.reset()
                        }

                        return@update
                    }

                    body.physics.velocity.setZero()

                    stateTimer.update(delta)
                    if (!megaman.dead && stateTimer.isFinished()) {
                        moving = true
                        targetTimer.reset()
                        target.set(megaman.body.getCenter())
                        GameLogger.debug(TAG, "update(): state timer finished, set moving to target=$target")
                    }

                    trailSpriteTimer.setToEnd()

                    FacingUtils.setFacingOf(this)
                }
                StarknerState.BLACKHOLE -> {
                    stateTimer.update(delta)
                    if (stateTimer.isFinished()) destroy()

                    if (!game.audioMan.isSoundPlaying(SoundAsset.BLACKHOLE_SOUND))
                        game.audioMan.playSound(SoundAsset.BLACKHOLE_SOUND, false)
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        gdxArrayOf(FixtureType.DAMAGER, FixtureType.DAMAGEABLE).forEach { type ->
            val fixture = Fixture(body, type, GameCircle())
            body.addFixture(fixture)
            debugShapes.add { fixture }
        }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val radius = body.getWidth() / 2f
            body.forEachFixture { fixture -> ((fixture as Fixture).rawShape as GameCircle).setRadius(radius) }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5)))
        .updatable { delta, sprite ->
            val size = when (state) {
                StarknerState.BLACKHOLE -> 5f
                else -> 2.5f
            }
            sprite.setSize(size * ConstVals.PPM)

            sprite.setCenter(body.getCenter())

            sprite.setOriginCenter()
            if (!game.paused) when {
                moving && !megaman.dead -> sprite.rotation += ROTS_PER_SEC * 360f * delta * -facing.value
                else -> sprite.rotation = direction.rotation
            }

            sprite.setFlip(isFacing(Facing.RIGHT), false)

            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier key@{
                    var key = state.name.lowercase()
                    if (state == StarknerState.BLACKHOLE) {
                        val time = stateTimer.time
                        if (time < BLACKHOLE_FADE_DUR) key += "_appear"
                        else if (time > STATE_DURS[state] - BLACKHOLE_FADE_DUR) key += "_disappear"
                    }
                    return@key key
                }
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
}
