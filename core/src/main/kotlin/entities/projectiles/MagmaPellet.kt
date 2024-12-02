package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getOverlapPushDirection
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.HazardsFactory

import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGdxRectangle
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getCenter

class MagmaPellet(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        const val TAG = "MagmaPellet"
        private const val GRAVITY = -0.375f
        private var region: TextureRegion? = null
    }

    override fun init() {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_2.source, TAG)
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        val impulse = spawnProps.get(ConstKeys.IMPULSE, Vector2::class)!!
        body.physics.velocity.set(impulse)
    }

    override fun hitBlock(
        blockFixture: IFixture,
        thisShape: IGameShape2D,
        otherShape: IGameShape2D
    ) = explodeAndDie(thisShape, otherShape)

    override fun explodeAndDie(vararg params: Any?) {
        GameLogger.debug(TAG, "explodeAndDie(): params=$params")

        destroy()

        val thisShape = params[0] as IGameShape2D
        val otherShape = params[1] as IGameShape2D

        val direction = getOverlapPushDirection(thisShape, otherShape) ?: Direction.UP
        val position = DirectionPositionMapper.getPosition(direction)

        val overlap = Rectangle()
        val overlapping = Intersector.intersectRectangles(
            thisShape.toGdxRectangle(),
            otherShape.toGdxRectangle(),
            overlap
        )
        val spawn = if (overlapping) overlap.getPositionPoint(position) else thisShape.getCenter()

        GameLogger.debug(
            TAG,
            "explodeAndDie(): spawn=$spawn, overlapping=$overlapping, overlap=$overlap, " +
                "direction=$direction, position=$position"
        )

        val explosion = EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.MAGMA_FLAME)!!
        explosion.spawn(props(ConstKeys.POSITION pairTo spawn, ConstKeys.DIRECTION pairTo direction))
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.35f * ConstVals.PPM)
        body.physics.applyFrictionY = false
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        val debugShapes = Array<() -> IDrawableShape?>()
        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))
        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER),
            debugShapes
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(region!!, DrawingPriority(DrawingSection.FOREGROUND, 1))
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ -> sprite.setCenter(body.getCenter()) }
        return spritesComponent
    }
}
