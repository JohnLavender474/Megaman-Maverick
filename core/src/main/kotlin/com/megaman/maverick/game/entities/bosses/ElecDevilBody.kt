package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedMap
import com.maverick.game.engine.animations.AnimationsComponentBuilder
import com.maverick.game.engine.animations.AnimatorBuilder
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.interfaces.IActivatable
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.bosses.ElecDevilConstants.BODY_SPRITE_FACING_LEFT_OFFSET_X
import com.megaman.maverick.game.entities.bosses.ElecDevilConstants.BODY_SPRITE_FACING_RIGHT_OFFSET_X
import com.megaman.maverick.game.entities.bosses.elecdevil.ClawPhase
import com.megaman.maverick.game.entities.bosses.elecdevil.EyePattern
import com.megaman.maverick.game.entities.contracts.ILightSource
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.misc.LightSourceUtils
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getPositionPoint

class ElecDevilBody(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    IActivatable, IFaceable, IDamagerEntity, IOwnable<ElecDevil>, ILightSource {

    companion object {
        const val TAG = "ElecDevilBody"

        private const val LIGHT_SOURCE_RADIUS = 5
        private const val LIGHT_SOURCE_RADIANCE = 3f

        private const val CHARGE_KEY = "charge"
        private const val SHOOT_KEY = "shoot"
        private const val HAND_WINDUP_KEY = "hand_windup"
        private const val HAND_FIRED_KEY = "hand_fired"
        private const val HAND_RECOIL_KEY = "hand_recoil"

        private val animDefs = OrderedMap<String, AnimationDef>().also { defs ->
            defs.put("turn_to_pieces", AnimationDef(4, 2, 0.1f, false))
            defs.put("appear", AnimationDef(4, 2, 0.05f, false))
            defs.put("stand", AnimationDef(2, 2, 0.1f, true))
            EyePattern.entries.forEach { pattern ->
                defs.put(pattern.regionKey(CHARGE_KEY), AnimationDef(2, 2, 0.1f, true))
                defs.put(pattern.regionKey(SHOOT_KEY), AnimationDef(2, 2, 0.075f, true))
            }
            defs.put(HAND_WINDUP_KEY, AnimationDef(2, 1, 0.05f, true))
            defs.put(HAND_FIRED_KEY, AnimationDef(2, 1, 0.1f, true))
            defs.put(HAND_RECOIL_KEY, AnimationDef(2, 1, 0.1f, false))
            defs.put(
                "defeated", AnimationDef(
                    rows = 4,
                    cols = 2,
                    durations = gdxArrayOf(0.08f, 0.08f, 0.1f, 0.1f, 0.1f, 0.12f, 0.14f, 0.14f),
                    loop = false
                )
            )
        }
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var owner: ElecDevil? = null
    override lateinit var facing: Facing
    override var on = false
        set(value) {
            field = value
            GameLogger.debug(TAG, "facing=$facing, on=$field")
        }

    override val lightSourceKeys = ObjectSet<Int>()
    override val lightSourceCenter: Vector2
        get() = body.getCenter()
    override var lightSourceRadius = LIGHT_SOURCE_RADIUS
    override var lightSourceRadiance = LIGHT_SOURCE_RADIANCE

    private val lightSourceSendEventDelay = Timer(ElecDevilConstants.LIGHT_SOURCE_SEND_EVENT_DELAY)

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init(): hashcode=${hashCode()}")
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.BOSSES_4.source)
            AnimationUtils.loadRegions(ElecDevil.TAG, atlas, animDefs.keys(), regions)
        }
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): hashcode=${hashCode()}, spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setBottomCenterToPoint(position)

        owner = spawnProps.get(ConstKeys.OWNER, ElecDevil::class)!!
        facing = spawnProps.get(ConstKeys.FACING, Facing::class)!!
        on = spawnProps.get(ConstKeys.ACTIVE, Boolean::class)!!

        lightSourceSendEventDelay.reset()

        lightSourceKeys.addAll(
            spawnProps.get("${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}") as ObjectSet<Int>
        )
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy(): hashcode=${hashCode()}")
        super.onDestroy()
        lightSourceKeys.clear()
    }

    // if facing right, then column 0 is at the far left, otherwise is at the far right
    internal fun getPositionOf(row: Int, column: Int, out: Vector2): Vector2 {
        val x = body.getX().plus(
            ElecDevilConstants.PIECE_WIDTH * ConstVals.PPM * when {
                isFacing(Facing.RIGHT) -> column
                else -> ElecDevilConstants.PIECE_COLUMNS - column - 1
            }
        )
        val y = body.getY().plus(row * ElecDevilConstants.PIECE_HEIGHT * ConstVals.PPM)
        return out.set(x, y)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        when {
            on -> {
                lightSourceSendEventDelay.update(delta)
                if (lightSourceSendEventDelay.isFinished()) {
                    LightSourceUtils.sendLightSourceEvent(this)
                    lightSourceSendEventDelay.reset()
                }
            }

            else -> lightSourceSendEventDelay.setToEnd()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ElecDevilConstants.BODY_WIDTH * ConstVals.PPM, ElecDevilConstants.BODY_HEIGHT * ConstVals.PPM)
        body.drawingColor = Color.YELLOW

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite ->
            sprite.setSize(
                ElecDevilConstants.BODY_SPRITE_WIDTH * ConstVals.PPM,
                ElecDevilConstants.BODY_SPRITE_HEIGHT * ConstVals.PPM
            )
        })
        .preProcess { _, sprite ->
            sprite.hidden = !on || owner!!.isDamageBlinking()
            sprite.color = if (owner!!.isPhaseFlashOn()) ElecDevil.PHASE_FLASH_COLOR else Color.WHITE

            sprite.setFlip(isFacing(Facing.LEFT), false)

            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)

            val offsetX =
                if (isFacing(Facing.LEFT)) BODY_SPRITE_FACING_LEFT_OFFSET_X else BODY_SPRITE_FACING_RIGHT_OFFSET_X
            sprite.translateX(offsetX * ConstVals.PPM)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { if (on) animationKey(owner!!) else null }
                .applyToAnimations { animations -> AnimationUtils.loadAnimationDefs(animDefs, animations, regions) }
                .build()
        )
        .build()

    private fun animationKey(owner: ElecDevil) = when {
        owner.defeated -> "defeated"
        owner.getCurrentState() == ElecDevilState.STAND && owner.isEyeSpreadShooting() ->
            owner.getEyePattern().regionKey(SHOOT_KEY)

        owner.getCurrentState() == ElecDevilState.CHARGE -> owner.getEyePattern().regionKey(CHARGE_KEY)
        owner.getCurrentState() == ElecDevilState.HAND -> when (owner.getClawPhase()) {
            ClawPhase.DELAY -> ElecDevilState.STAND.name.lowercase()
            ClawPhase.WINDUP -> HAND_WINDUP_KEY
            ClawPhase.OUT -> HAND_FIRED_KEY
            ClawPhase.RECOIL -> HAND_RECOIL_KEY
        }

        else -> owner.getCurrentState().name.lowercase()
    }

    override fun getType() = EntityType.OTHER

    override fun toString() =
        "{ ElecDevilBody:[" + if (existenceState.liveNow) "facing=$facing, bounds=${body.getBounds()}] }" else "not spawned] }"
}
