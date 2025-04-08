package com.megaman.maverick.game.entities.bosses

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
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IActivatable
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getPositionPoint

class ElecDevilBody(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    IActivatable, IFaceable, IDamager, IOwnable<ElecDevil> {

    companion object {
        const val TAG = "ElecDevilBody"

        private const val BODY_WIDTH = 9.7f
        private const val BODY_HEIGHT = 9f

        private const val SPRITE_WIDTH = 12f
        private const val SPRITE_HEIGHT = 9f
        private const val SPRITE_OFFSET_X = 0.425f

        private val animDefs = orderedMapOf(
            ElecDevilState.APPEAR pairTo AnimationDef(2, 2, 0.1f, false),
            ElecDevilState.CHARGE pairTo AnimationDef(3, 1, 0.1f, true),
            ElecDevilState.STAND pairTo AnimationDef(2, 2, 0.1f, true),
            ElecDevilState.HAND pairTo AnimationDef(),
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var owner: ElecDevil? = null
    override lateinit var facing: Facing
    override var on = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_3.source)
            animDefs.keys().forEach { state ->
                val key = state.name.lowercase()
                regions.put(key, atlas.findRegion("${TAG}/$key"))
            }
        }
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setBottomCenterToPoint(position)

        owner = spawnProps.get(ConstKeys.OWNER, ElecDevil::class)!!
        facing = spawnProps.get(ConstKeys.FACING, Facing::class)!!
        on = spawnProps.get(ConstKeys.ON, Boolean::class)!!
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    // if facing right, then column 0 is at the far left, otherwise is at the far right
    internal fun getPositionOf(row: Int, column: Int): Vector2 {
        val out = GameObjectPools.fetch(Vector2::class)
        val x = body.getX().plus(
            ConstVals.PPM * when {
                isFacing(Facing.RIGHT) -> column
                else -> ElecDevilConstants.PIECE_COLUMNS - column
            }
        )
        val y = body.getY().plus(row * ConstVals.PPM)
        return out.set(x, y)
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        // TODO: create damager fixtures for body parts

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite ->
            sprite.setSize(SPRITE_WIDTH * ConstVals.PPM, SPRITE_HEIGHT * ConstVals.PPM)
        })
        .updatable { _, sprite ->
            sprite.hidden = !on

            sprite.setFlip(isFacing(Facing.LEFT), false)

            val position = if (isFacing(Facing.RIGHT)) Position.BOTTOM_LEFT else Position.BOTTOM_RIGHT
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.translateX(SPRITE_OFFSET_X * ConstVals.PPM)
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
                        val animation = Animation(regions[key], rows, columns, durations, loop)
                        animations.put(key, animation)
                    }
                }
                .build()
        )
        .build()

    override fun getType() = EntityType.OTHER

    override fun getTag() = TAG
}
