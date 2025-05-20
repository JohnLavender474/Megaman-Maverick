package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.world.body.*

class SteamRoller(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IFaceable {

    companion object {
        const val TAG = "SteamRoller"
        private var region: TextureRegion? = null
    }

    override lateinit var facing: Facing

    private val blocks = OrderedSet<Block>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, TAG)
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val blockBounds = spawnProps.get(ConstKeys.BLOCKS) as Iterable<GameRectangle>
        blockBounds.forEach { blockBound ->
            val block = MegaEntityFactory.fetch(Block::class)!!
            block.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.BOUNDS pairTo blockBound,
                    ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
                    "${ConstKeys.FEET}_${ConstKeys.SOUND}" pairTo false,
                    ConstKeys.BODY_LABELS pairTo objectSetOf(
                        BodyLabel.COLLIDE_DOWN_ONLY
                    ),
                    ConstKeys.FIXTURE_LABELS pairTo objectSetOf(
                        FixtureLabel.NO_SIDE_TOUCHIE,
                        FixtureLabel.NO_BODY_TOUCHIE,
                        FixtureLabel.NO_PROJECTILE_COLLISION
                    ),
                    ConstKeys.FIXTURES pairTo gdxArrayOf(
                        FixtureType.SHIELD pairTo props(ConstKeys.DIRECTION pairTo Direction.UP)
                    )
                )
            )
            blocks.add(block)
        }

        facing = spawnProps.get(ConstKeys.FACING, Facing::class)!!

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setBottomCenterToPoint(spawn)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        blocks.forEach { it.destroy() }
        blocks.clear()
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo getGameCameraCullingLogic(this))
    )

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(4.75f * ConstVals.PPM, 4f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(region!!).also { sprite -> sprite.setSize(7f * ConstVals.PPM, 6f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.setFlip(isFacing(Facing.LEFT), false)
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.translateX(0.75f * ConstVals.PPM * -facing.value)
        }
        .build()

    override fun getType() = EntityType.OTHER

    override fun getTag() = TAG
}
