package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponentBuilder
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameLine
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.bosses.elecdevil.LiveWireArcSprites
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

class ElecDevilClaw(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity,
    IAnimatedEntity, IFaceable, IDamagerEntity, IOwnable<ElecDevil> {

    companion object {
        const val TAG = "ElecDevilClaw"

        internal const val BODY_WIDTH = 1.625f
        internal const val BODY_HEIGHT = 1.8125f

        private const val SPRITE_SIZE = 3f
        private const val TETHER_KEY = "tether"
        private const val TETHER_BACK_X = 0.75f
        private const val TETHER_HALF_THICKNESS = 0.15625f
        private const val GUIDE_ALPHA = 0.4f
        private const val GUIDE_FLICKER_DUR = 0.1f

        private var region: TextureRegion? = null
    }

    private enum class ClawState { AIM, OUT, HOLD, BACK }

    override var owner: ElecDevil? = null
    override lateinit var facing: Facing

    private val socket = Vector2()
    private val home = Vector2()
    private val target = Vector2()
    private lateinit var state: ClawState
    private var speed = 0f
    private val holdTimer = Timer()
    private lateinit var onBack: () -> Unit
    private var time = 0f

    private val tether = LiveWireArcSprites(TETHER_KEY, priority = 3) { from, to ->
        from.set(socket)
        when (state) {
            ClawState.AIM -> {
                tetherEnd(to.set(target))
                (time / GUIDE_FLICKER_DUR).toInt() % 2 == 0
            }

            else -> {
                tetherEnd(to.set(body.getCenter()))
                true
            }
        }
    }

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null)
            region = services.assets.getTextureRegion(TextureAsset.BOSSES_4.source, "${ElecDevil.TAG}/claw")
        LiveWireArcSprites.loadRegions(services.assets)
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER, ElecDevil::class)!!
        facing = spawnProps.get(ConstKeys.FACING, Facing::class)!!
        socket.set(spawnProps.get(ConstKeys.START, Vector2::class)!!)
        target.set(spawnProps.get(ConstKeys.TARGET, Vector2::class)!!)
        speed = spawnProps.get(ConstKeys.SPEED, Float::class)!! * ConstVals.PPM
        holdTimer.resetDuration(spawnProps.get(ConstKeys.DURATION, Float::class)!!)
        @Suppress("UNCHECKED_CAST")
        onBack = spawnProps.get(ConstKeys.END) as () -> Unit

        home.set(socket).add((if (isFacing(Facing.RIGHT)) TETHER_BACK_X else -TETHER_BACK_X) * ConstVals.PPM, 0f)
        body.setCenter(home)
        state = ClawState.AIM
        time = 0f
        tether.time = 0f
        tether.alpha = GUIDE_ALPHA
    }

    internal fun fire() {
        GameLogger.debug(TAG, "fire()")
        if (state != ClawState.AIM) throw IllegalStateException("fire() needs state AIM, was $state")
        state = ClawState.OUT
        tether.alpha = 1f
    }

    private fun tetherEnd(clawCenter: Vector2) =
        clawCenter.add((if (isFacing(Facing.RIGHT)) -TETHER_BACK_X else TETHER_BACK_X) * ConstVals.PPM, 0f)

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        time += delta
        tether.time = time
        when (state) {
            ClawState.AIM -> {}

            ClawState.OUT -> if (moveTo(target, delta)) {
                state = ClawState.HOLD
                holdTimer.reset()
                services.audio.playSound(SoundAsset.ELECTRIC_1_SOUND, loop = false)
            }

            ClawState.HOLD -> {
                holdTimer.update(delta)
                if (holdTimer.isFinished()) state = ClawState.BACK
            }

            ClawState.BACK -> if (moveTo(home, delta)) {
                onBack.invoke()
                destroy()
            }
        }
    })

    private fun moveTo(point: Vector2, delta: Float): Boolean {
        val center = body.getCenter()
        val step = speed * delta
        if (center.dst(point) <= step) {
            body.setCenter(point)
            return true
        }
        val move = GameObjectPools.fetch(Vector2::class).set(point).sub(center).nor().scl(step)
        body.setCenter(center.add(move))
        return false
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.drawingColor = Color.YELLOW

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val clawDamager = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(clawDamager)
        val clawShield = Fixture(body, FixtureType.SHIELD, GameRectangle(body))
        body.addFixture(clawShield)

        val tetherDamagers = Array<Fixture>()
        repeat(2) {
            val fixture = Fixture(body, FixtureType.DAMAGER, GameLine())
            fixture.attachedToBody = false
            fixture.drawingColor = Color.CYAN
            body.addFixture(fixture)
            tetherDamagers.add(fixture)
            debugShapes.add { if (fixture.isActive()) fixture else null }
        }

        body.preProcess.put(ConstKeys.ACTIVE) {
            val live = state != ClawState.AIM
            clawDamager.setActive(live)
            clawShield.setActive(live)

            val from = GameObjectPools.fetch(Vector2::class).set(socket)
            val to = tetherEnd(GameObjectPools.fetch(Vector2::class).set(body.getCenter()))
            val normal = GameObjectPools.fetch(Vector2::class).set(to).sub(from).nor().rotate90(1)
                .scl(TETHER_HALF_THICKNESS * ConstVals.PPM)
            tetherDamagers.forEachIndexed { index, fixture ->
                fixture.setActive(live)
                val side = if (index == 0) 1f else -1f
                (fixture.rawShape as GameLine).set(
                    from.x + normal.x * side, from.y + normal.y * side,
                    to.x + normal.x * side, to.y + normal.y * side
                )
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 4)).also { sprite ->
            sprite.setSize(SPRITE_SIZE * ConstVals.PPM)
        })
        .preProcess { _, sprite ->
            sprite.hidden = state == ClawState.AIM
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.setCenter(body.getCenter())
        }
        .also { builder -> tether.addTo(builder) }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG).animator(Animator(Animation(region!!, 1, 2, 0.05f, true)))
        .build()

    override fun getType() = EntityType.OTHER
}
