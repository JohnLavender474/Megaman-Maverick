package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
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
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
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
import com.megaman.maverick.game.damage.IDamageNegotiator
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.PropellerPlatform
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.Disintegration
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class PreciousGem(game: MegamanMaverickGame) : AbstractHealthEntity(game), IProjectileEntity, ISpritesEntity,
    IAnimatedEntity, IEventListener {

    companion object {
        const val TAG = "PreciousGem"
        private const val CULL_TIME = 5f
        private const val SIZE_INCREASE_DELAY_DUR = 0.1f
        private const val PAUSE_BEFORE_FIRST_TARGET_DUR = 0.5f
        private val BODY_SIZES = gdxArrayOf(0.25f, 0.5f, 0.75f, 1f)
        private val IGNORE_DMG = objectSetOf<KClass<out IDamager>>(
            Asteroid::class,
            PropellerPlatform::class,
        )
        private val regions = ObjectMap<PreciousGemColor, TextureRegion>()
    }

    enum class PreciousGemColor { PURPLE, BLUE, PINK }

    override val damageNegotiator = object : IDamageNegotiator {

        private val NON_MEGAMAN_DMG_NEGS = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
            MoonScythe::class pairTo dmgNeg(4)
        )

        override fun get(damager: IDamager) = when {
            IGNORE_DMG.contains(damager::class) -> 0
            else -> when (owner) {
                megaman -> 3
                else -> NON_MEGAMAN_DMG_NEGS[damager::class]?.get(damager) ?: 0
            }
        }
    }
    override val eventKeyMask = objectSetOf<Any>(EventType.PLAYER_JUST_DIED)
    override var owner: IGameEntity? = null
    override var size = Size.MEDIUM

    val firstTarget = Vector2()

    var onFirstTargetReached: (() -> Unit)? = null
    var secondTargetSupplier: (() -> Vector2)? = null

    var speed = 0f

    var stateIndex = 0
        private set

    lateinit var color: PreciousGemColor

    private val sizeIncreaseDelay = Timer(SIZE_INCREASE_DELAY_DUR)
    private var sizeIncreaseIndex = 0

    private val pauseBeforeFirstTarget = Timer(PAUSE_BEFORE_FIRST_TARGET_DUR)

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
        spawnProps.put(ConstKeys.CULL_TIME, CULL_TIME)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)

        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(position)

        if (spawnProps.containsKey("${ConstKeys.FIRST}_${ConstKeys.TARGET}"))
            firstTarget.set(spawnProps.get("${ConstKeys.FIRST}_${ConstKeys.TARGET}", Vector2::class)!!)

        stateIndex = spawnProps.getOrDefault(ConstKeys.STATE, 0, Int::class)

        sizeIncreaseDelay.reset()
        sizeIncreaseIndex = 0
        setSizeByIndex(0)

        pauseBeforeFirstTarget.reset()
        color = spawnProps.get(ConstKeys.COLOR, PreciousGemColor::class)!!
        speed = spawnProps.getOrDefault(ConstKeys.SPEED, 0f, Float::class)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
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
        if (event.key == EventType.PLAYER_JUST_DIED) destroy()
    }

    override fun takeDamageFrom(damager: IDamager): Boolean {
        val damaged = super.takeDamageFrom(damager)
        if (damaged) requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
        return damaged
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
                    if (sizeIncreaseIndex < BODY_SIZES.size - 1) {
                        sizeIncreaseDelay.update(delta)

                        if (sizeIncreaseDelay.isFinished()) {
                            sizeIncreaseIndex++
                            setSizeByIndex(sizeIncreaseIndex)

                            sizeIncreaseDelay.reset()
                        }
                    }

                    if (secondTargetSupplier != null) {
                        pauseBeforeFirstTarget.update(delta)

                        if (pauseBeforeFirstTarget.isFinished()) {
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
            BodyFixtureDef.Companion.of(
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
        .updatable { _, sprite ->
            sprite.setCenter(body.getCenter())
            sprite.hidden = damageBlink
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
