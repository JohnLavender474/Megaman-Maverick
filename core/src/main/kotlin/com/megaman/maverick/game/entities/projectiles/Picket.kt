package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.add
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.blocks.BreakableBlock
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.decorations.BlockPiece.BlockPieceColor
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

class Picket(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity {

    companion object {
        const val TAG = "Picket"
        private var region: TextureRegion? = null
        private const val GRAVITY = -0.15f
    }

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, TAG)
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val impulse = spawnProps.get(ConstKeys.IMPULSE, Vector2::class)!!
        body.physics.velocity.set(impulse)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        GameLogger.debug(TAG, "hitBlock()")

        destroy()

        val direction = UtilMethods.getOverlapPushDirection(thisShape, otherShape) ?: Direction.UP
        val position = thisShape.getCenter().add(0.5f * ConstVals.PPM, direction.getOpposite())
        BreakableBlock.breakApart(position, BlockPieceColor.BROWN)

        playSoundNow(SoundAsset.THUMP_SOUND, false)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.75f * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }
        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.PROJECTILE, FixtureType.SHIELD, FixtureType.DAMAGER)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(GameSprite().also { sprite -> sprite.setSize(2.5f * ConstVals.PPM) })
        .updatable { _, sprite -> sprite.setCenter(body.getCenter()) }
        .build()

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 4, 0.1f)
        return AnimationsComponent(this, Animator(animation))
    }
}
