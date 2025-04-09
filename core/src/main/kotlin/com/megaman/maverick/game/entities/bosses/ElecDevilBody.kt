package com.megaman.maverick.game.entities.bosses

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
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IActivatable
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.bosses.ElecDevilConstants.BODY_SPRITE_FACING_LEFT_OFFSET_X
import com.megaman.maverick.game.entities.bosses.ElecDevilConstants.BODY_SPRITE_FACING_RIGHT_OFFSET_X
import com.megaman.maverick.game.entities.contracts.ILightSourceEntity
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.misc.LightSourceUtils
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getPositionPoint

class ElecDevilBody(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    IActivatable, IFaceable, IDamager, IOwnable<ElecDevil>, ILightSourceEntity {

    companion object {
        const val TAG = "ElecDevilBody"

        private const val LIGHT_SOURCE_RADIUS = 5
        private const val LIGHT_SOURCE_RADIANCE = 3f

        private val animDefs = orderedMapOf(
            ElecDevilState.TURN_TO_PIECES pairTo AnimationDef(4, 2, 0.1f, false),
            ElecDevilState.APPEAR pairTo AnimationDef(2, 2, 0.1f, false),
            ElecDevilState.CHARGE pairTo AnimationDef(2, 2, 0.1f, true),
            ElecDevilState.STAND pairTo AnimationDef(2, 2, 0.1f, true),
            ElecDevilState.HAND pairTo AnimationDef(),
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var owner: ElecDevil? = null
    override lateinit var facing: Facing
    override var on = false

    override val lightSourceKeys = ObjectSet<Int>()
    override val lightSourceCenter: Vector2
        get() = body.getCenter()
    override var lightSourceRadius = LIGHT_SOURCE_RADIUS
    override var lightSourceRadiance = LIGHT_SOURCE_RADIANCE

    private val lightSourceSendEventDelay = Timer(ElecDevilConstants.LIGHT_SOURCE_SEND_EVENT_DELAY)

    override fun init() {
        GameLogger.debug(TAG, "init(): hashcode=${hashCode()}")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_3.source)
            animDefs.keys().forEach { state ->
                val key = state.name.lowercase()
                regions.put(key, atlas.findRegion("${ElecDevil.TAG}/$key"))
            }
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
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
    internal fun getPositionOf(row: Int, column: Int): Vector2 {
        // fetched Vector2 should be freed manually
        val out = GameObjectPools.fetch(Vector2::class, false)

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
                    LightSourceUtils.sendLightSourceEvent(game, this)
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
        .updatable { _, sprite ->
            sprite.hidden = !on || owner!!.isDamageBlinking()

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
                .setKeySupplier { if (on) owner!!.getCurrentState().name.lowercase() else null }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val state = entry.key
                        val key = state.name.lowercase()
                        val (rows, columns, durations, loop) = entry.value
                        try {
                            val animation = Animation(regions[key], rows, columns, durations, loop)
                            animations.put(key, animation)
                        } catch (e: Exception) {
                            throw IllegalStateException("Failed to create animation for key=$key", e)
                        }
                    }
                }
                .build()
        )
        .build()

    override fun getType() = EntityType.OTHER

    override fun getTag() = TAG

    override fun toString() =
        "{ ElecDevilBody:[" + if (spawned) "facing=$facing, bounds=${body.getBounds()}] }" else "not spawned] }"
}
