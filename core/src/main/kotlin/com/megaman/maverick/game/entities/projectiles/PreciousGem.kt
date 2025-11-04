package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getOverlapPushDirection
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
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
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.IDamageNegotiator
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.PropellerPlatform
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.Disintegration
import com.megaman.maverick.game.entities.projectiles.PreciousGemBomb.Companion.SHATTER_IMPULSES
import com.megaman.maverick.game.entities.projectiles.PreciousShard.PreciousShardColor
import com.megaman.maverick.game.entities.projectiles.PreciousShard.PreciousShardSize
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class PreciousGem(game: MegamanMaverickGame) : AbstractHealthEntity(game), IProjectileEntity, ISpritesEntity,
    IAnimatedEntity, IEventListener, IDirectional {

    companion object {
        const val TAG = "PreciousGem"
        private const val DEFAULT_CULL_TIME = 5f
        private const val SIZE_DELAY_DUR = 0.1f
        private const val DEFAULT_PAUSE_DUR = 0.5f
        private val BODY_SIZES = gdxArrayOf(0.25f, 0.5f, 0.75f, 1f)
        private val IGNORE_DMG = objectSetOf<KClass<out IDamager>>(
            MoonScythe::class, Asteroid::class, PropellerPlatform::class
        )
        private val regions = ObjectMap<PreciousGemColor, TextureRegion>()
    }

    enum class PreciousGemColor { PURPLE, BLUE, PINK, GREEN }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }

    override val damageNegotiator = object : IDamageNegotiator {

        override fun get(damager: IDamager) = when {
            IGNORE_DMG.contains(damager::class) -> 0
            owner == megaman -> 5
            else -> 0
        }
    }
    override val eventKeyMask = objectSetOf<Any>(
        EventType.PLAYER_JUST_DIED,
        EventType.BEGIN_ROOM_TRANS,
        EventType.BOSS_DEFEATED
    )
    override var owner: IGameEntity? = null
    override var size = Size.MEDIUM

    val firstTarget = Vector2()

    var onFirstTargetReached: (() -> Unit)? = null
    var secondTargetSupplier: (() -> Vector2)? = null

    var speed = 0f

    var stateIndex = 0
        private set

    lateinit var color: PreciousGemColor

    var blockShatter = false

    lateinit var shieldShatterClasses: ObjectSet<KClass<out IGameEntity>>

    private val sizeDelay = Timer(SIZE_DELAY_DUR)
    private var sizeIndex = 0

    private val pauseDelay = Timer(DEFAULT_PAUSE_DUR)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            PreciousGemColor.entries.forEach { color ->
                regions.put(
                    color,
                    atlas.findRegion("$TAG/${color.name.lowercase()}")
                )
            }
        }
        super.init()
        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        if (!spawnProps.containsKey(ConstKeys.CULL_TIME))
            spawnProps.put(ConstKeys.CULL_TIME, DEFAULT_CULL_TIME)

        GameLogger.debug(TAG, "onSpawn(): hashCode=${hashCode()}, spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)

        direction = spawnProps.getOrDefault(
            ConstKeys.DIRECTION,
            if (owner is IDirectional) (owner as IDirectional).direction else megaman.direction,
            Direction::class
        )

        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(position)

        if (spawnProps.containsKey("${ConstKeys.FIRST}_${ConstKeys.TARGET}"))
            firstTarget.set(spawnProps.get("${ConstKeys.FIRST}_${ConstKeys.TARGET}", Vector2::class)!!)

        stateIndex = spawnProps.getOrDefault(ConstKeys.STATE, 0, Int::class)

        sizeIndex = 0
        sizeDelay.reset()
        setSizeByIndex(0)

        val pauseDelayDur = spawnProps.getOrDefault(ConstKeys.PAUSE, DEFAULT_PAUSE_DUR, Float::class)
        pauseDelay.resetDuration(pauseDelayDur)

        color = spawnProps.get(ConstKeys.COLOR, PreciousGemColor::class)!!
        speed = spawnProps.getOrDefault(ConstKeys.SPEED, 0f, Float::class)

        blockShatter = spawnProps.getOrDefault(
            "${ConstKeys.BLOCK}_${ConstKeys.SHATTER}", false, Boolean::class
        )
        shieldShatterClasses = spawnProps.getOrDefault(
            "${ConstKeys.SHIELD}_${ConstKeys.SHATTER}", ObjectSet<KClass<out IGameEntity>>(), ObjectSet::class
        ) as ObjectSet<KClass<out IGameEntity>>
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy(): hashCode=${hashCode()}")
        super.onDestroy()

        game.eventsMan.removeListener(this)
    }

    override fun onHealthDepleted() {
        GameLogger.debug(TAG, "onHealthDepleted()")
        super.onHealthDepleted()

        val disintegration = MegaEntityFactory.fetch(Disintegration::class)!!
        disintegration.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
    }

    override fun canBeDamagedBy(damager: IDamager): Boolean {
        if (!super.canBeDamagedBy(damager)) return false
        if (damager is IProjectileEntity) return damager.owner != owner
        return true
    }

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "onEvent(): event=$event")
        if (owner == megaman && event.key == EventType.PLAYER_JUST_DIED) destroy()
        if (owner != megaman && event.key.equalsAny(EventType.BEGIN_ROOM_TRANS, EventType.BOSS_DEFEATED)) destroy()
    }

    override fun takeDamageFrom(damager: IDamager): Boolean {
        val damaged = super.takeDamageFrom(damager)
        if (damaged) requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
        if (isHealthDepleted()) {
            val direction =
                getOverlapPushDirection(body.getBounds(), (damager as IBodyEntity).body.getBounds()) ?: Direction.UP
            explodeAndDie(direction)
        }
        return damaged
    }

    override fun hitProjectile(projectileFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        GameLogger.debug(
            TAG,
            "hitProjectile(): projectileFixture=$projectileFixture, thisShape=$thisShape, otherShape=$otherShape"
        )

        val projectile = projectileFixture.getEntity()
        if (projectile is Axe) {
            val direction = getOverlapPushDirection(thisShape, otherShape) ?: Direction.UP
            explodeAndDie(direction)
        }
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (blockShatter) {
            GameLogger.debug(
                TAG,
                "hitBlock(): blockFixture=$blockFixture, thisShape=$thisShape, otherShape=$otherShape"
            )

            val direction = getOverlapPushDirection(thisShape, otherShape) ?: Direction.UP
            explodeAndDie(direction)
        }
    }

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val shieldEntity = shieldFixture.getEntity()

        if (shieldShatterClasses.contains(shieldEntity::class)) {
            GameLogger.debug(
                TAG,
                "hitShield(): shieldFixture=$shieldFixture, thisShape=$thisShape, otherShape=$otherShape"
            )

            val direction = getOverlapPushDirection(thisShape, otherShape) ?: Direction.UP
            explodeAndDie(direction)
        }
    }

    override fun explodeAndDie(vararg params: Any?) {
        val direction = if (params.isNotEmpty()) params[0] as Direction else Direction.UP

        SHATTER_IMPULSES.get(direction).forEach { impulse ->
            val shard = MegaEntityFactory.fetch(PreciousShard::class)!!
            shard.spawn(
                props(
                    ConstKeys.OWNER pairTo owner,
                    ConstKeys.POSITION pairTo when (direction) {
                        Direction.UP -> body.getPositionPoint(Position.TOP_CENTER)
                        Direction.DOWN -> body.getPositionPoint(Position.BOTTOM_CENTER)
                        Direction.LEFT -> body.getPositionPoint(Position.CENTER_LEFT)
                        Direction.RIGHT -> body.getPositionPoint(Position.CENTER_RIGHT)
                    },
                    ConstKeys.IMPULSE pairTo GameObjectPools.fetch(Vector2::class)
                        .set(impulse)
                        .scl(ConstVals.PPM.toFloat()),
                    ConstKeys.COLOR pairTo when (color) {
                        PreciousGemColor.GREEN -> PreciousShardColor.GREEN
                        PreciousGemColor.BLUE -> PreciousShardColor.BLUE
                        PreciousGemColor.PINK -> PreciousShardColor.PINK
                        PreciousGemColor.PURPLE -> PreciousShardColor.PURPLE
                    },
                    "${ConstKeys.COLLIDE}_${ConstKeys.DELAY}" pairTo false,
                    ConstKeys.SIZE pairTo PreciousShardSize.SMALL
                )
            )
        }

        destroy()

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.DINK_SOUND, false)
    }

    private fun setSizeByIndex(index: Int) {
        val center = body.getCenter()

        val size = BODY_SIZES[index]
        body.setSize(size * ConstVals.PPM).setCenter(center)

        body.forEachFixture { fixture ->
            val bounds = (fixture as Fixture).rawShape as GameRectangle
            bounds.set(body)
        }
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            owner?.let { if (it is IDirectional) direction = it.direction }

            when (stateIndex) {
                0 -> {
                    val trajectory = firstTarget.cpy().sub(body.getCenter()).nor().scl(speed)
                    body.physics.velocity.set(trajectory)

                    if (body.getCenter().epsilonEquals(firstTarget, 0.1f * ConstVals.PPM)) {
                        onFirstTargetReached?.invoke()

                        body.physics.velocity.setZero()
                        body.setCenter(firstTarget)

                        stateIndex++

                        GameLogger.debug(TAG, "update(): target reached, stateIndex=$stateIndex")
                    }
                }
                1 -> {
                    if (sizeIndex < BODY_SIZES.size - 1) {
                        sizeDelay.update(delta)

                        if (sizeDelay.isFinished()) {
                            sizeIndex++
                            setSizeByIndex(sizeIndex)

                            sizeDelay.reset()
                        }
                    }

                    if (secondTargetSupplier != null) {
                        pauseDelay.update(delta)

                        if (pauseDelay.isFinished()) {
                            stateIndex++

                            val trajectory = secondTargetSupplier!!.invoke()
                                .sub(body.getCenter())
                                .nor()
                                .scl(speed)
                            body.physics.velocity.set(trajectory)

                            GameLogger.debug(TAG, "update(): stateIndex=$stateIndex, trajectory=$trajectory")
                        }
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(
                FixtureType.PROJECTILE,
                FixtureType.DAMAGEABLE,
                FixtureType.DAMAGER,
                FixtureType.SHIELD
            )
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
                .also { sprite -> sprite.setSize(2f * ConstVals.PPM) }
        )
        .preProcess { _, sprite ->
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
            sprite.setCenter(body.getCenter())
            sprite.hidden = damageBlink || (owner == megaman && megaman.teleporting)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { color.name.lowercase() }
                .applyToAnimations { animations ->
                    PreciousGemColor.entries.forEach { color ->
                        val animation = Animation(regions[color], 2, 2, 0.1f, false)
                        animations.put(color.name.lowercase(), animation)
                    }
                }
                .build()
        )
        .build()
}
