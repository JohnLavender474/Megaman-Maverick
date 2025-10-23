package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFireEntity
import com.megaman.maverick.game.entities.contracts.IFreezerEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.hazards.SmallIceCube
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.VelocityAlteration
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class Coldier(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "Coldier"

        private const val STAND_DUR = 0.25f
        private const val BEFORE_SMALL_BLOW_DUR = 0.1f
        private const val SMALL_BLOW_DUR = 0.75f
        private const val BEFORE_BIG_BLOW_DUR = 0.1f
        private const val BIG_BLOW_DUR = 1.5f
        private const val COOLDOWN_DUR = 1f
        private const val ICE_CUBE_DELAY = 0.5f

        private const val ICE_CUBE_VEL = 8f

        private const val SMALL_BLOW_FORCE = 10f
        private const val BIG_BLOW_FORCE = 20f
        private const val BLOW_MAX = 8f

        private val animDefs = objectMapOf(
            "stand" pairTo AnimationDef(),
            "big_blow" pairTo AnimationDef(3, 2, 0.1f, true),
            "small_blow" pairTo AnimationDef(3, 2, 0.1f, true),
            "before_big_blow" pairTo AnimationDef(2, 1, 0.1f, false),
            "before_small_blow" pairTo AnimationDef(2, 1, 0.1f, false),
            "cooldown" pairTo AnimationDef(2, 1, gdxArrayOf(0.5f, 0.25f), true),
        )

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class ColdierState { STAND, SMALL_BLOW, BIG_BLOW, COOLDOWN }

    override lateinit var facing: Facing

    private val loop = Loop(ColdierState.entries.toGdxArray())
    private val currentState: ColdierState
        get() = loop.getCurrent()

    private val timers = OrderedMap<String, Timer>()

    private val beforeBigBlow: Boolean
        get() = currentState == ColdierState.BIG_BLOW && !timers["before_big_blow"].isFinished()
    private val beforeSmallBlow: Boolean
        get() = currentState == ColdierState.SMALL_BLOW && !timers["before_small_blow"].isFinished()

    private val blowing: Boolean
        get() = currentState.equalsAny(ColdierState.SMALL_BLOW, ColdierState.BIG_BLOW)
    private val blowForce: Float
        get() = if (currentState == ColdierState.SMALL_BLOW) SMALL_BLOW_FORCE else BIG_BLOW_FORCE

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            val keys = Array<String>().also { it ->
                it.addAll("before_big_blow", "before_small_blow")
                ColdierState.entries.forEach { state -> it.add(state.name.lowercase()) }
            }
            keys.forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        if (timers.isEmpty) timers.putAll(
            "stand" pairTo Timer(STAND_DUR),
            "small_blow" pairTo Timer(SMALL_BLOW_DUR),
            "big_blow" pairTo Timer(BIG_BLOW_DUR).also { timer ->
                val runnables = Array<TimeMarkedRunnable>()

                val blows = (BIG_BLOW_DUR / ICE_CUBE_DELAY).toInt()
                for (i in 1..blows) {
                    val time = i * ICE_CUBE_DELAY
                    val runnable = TimeMarkedRunnable(time) { shootIceCube() }
                    runnables.add(runnable)
                }

                timer.addRunnables(runnables)
            },
            "before_small_blow" pairTo Timer(BEFORE_SMALL_BLOW_DUR),
            "before_big_blow" pairTo Timer(BEFORE_BIG_BLOW_DUR),
            "cooldown" pairTo Timer(COOLDOWN_DUR)
        )
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

        loop.reset()
        timers.values().forEach { it.reset() }
    }

    override fun editDamageFrom(damager: IDamager, baseDamage: Int) = when (damager) {
        is IFireEntity -> ConstVals.MAX_HEALTH
        is IFreezerEntity -> 1
        else -> baseDamage
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

            val timer = timers[currentState.name.lowercase()]

            when (currentState) {
                ColdierState.SMALL_BLOW -> when {
                    beforeSmallBlow -> timers["before_small_blow"].update(delta)
                    else -> timer.update(delta)
                }

                ColdierState.BIG_BLOW -> when {
                    beforeBigBlow -> timers["before_big_blow"].update(delta)
                    else -> timer.update(delta)
                }

                else -> timer.update(delta)
            }

            if (timer.isFinished()) {
                loop.next()
                timer.reset()
            }
        }
    }

    private fun shootIceCube() {
        val spawn = body.getCenter().add(0.5f * ConstVals.PPM * facing.value, 0.1f * ConstVals.PPM)

        val trajectory = GameObjectPools.fetch(Vector2::class).set(ICE_CUBE_VEL * ConstVals.PPM * facing.value, 0f)

        val cube = MegaEntityFactory.fetch(SmallIceCube::class)!!
        cube.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo trajectory,
                ConstKeys.HIT_BY_BLOCK pairTo true,
                ConstKeys.GRAVITY_ON pairTo false,
                ConstKeys.FRICTION_X pairTo false,
                ConstKeys.FRICTION_Y pairTo false,
            )
        )
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat(), 1.75f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val blowFixture = Fixture(
            body,
            FixtureType.FORCE,
            GameRectangle().setSize(10f * ConstVals.PPM, 1.5f * ConstVals.PPM)
        )
        blowFixture.setVelocityAlteration { fixture, delta, _ ->
            val entity = fixture.getEntity() as IBodyEntity
            if (entity != megaman) return@setVelocityAlteration VelocityAlteration.addNone()

            if ((isFacing(Facing.LEFT) && entity.body.physics.velocity.x <= -BLOW_MAX * ConstVals.PPM) ||
                (isFacing(Facing.RIGHT) && entity.body.physics.velocity.x >= BLOW_MAX * ConstVals.PPM)
            ) return@setVelocityAlteration VelocityAlteration.addNone()

            val force = blowForce * ConstVals.PPM * facing.value * delta
            return@setVelocityAlteration VelocityAlteration.add(force, 0f)
        }
        body.addFixture(blowFixture)
        blowFixture.drawingColor = Color.DARK_GRAY
        debugShapes.add { blowFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            blowFixture.setActive(blowing)
            val offsetX = 5f * ConstVals.PPM * facing.value
            blowFixture.offsetFromBodyAttachment.x = offsetX
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(3.5f * ConstVals.PPM, 2.625f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    when (currentState) {
                        ColdierState.BIG_BLOW -> if (beforeBigBlow) "before_big_blow" else "big_blow"
                        ColdierState.SMALL_BLOW -> if (beforeSmallBlow) "before_small_blow" else "small_blow"
                        else -> currentState.name.lowercase()
                    }
                }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val def = entry.value
                        val animation = Animation(regions[key], def.rows, def.cols, def.durations, def.loop)
                        animations.put(key, animation)
                    }
                }
                .build()
        )
        .build()

    override fun getTag() = TAG
}
