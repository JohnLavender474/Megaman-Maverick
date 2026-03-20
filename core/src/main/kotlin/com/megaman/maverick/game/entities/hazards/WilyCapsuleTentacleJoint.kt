package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.abs

class WilyCapsuleTentacleJoint(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IDamager,
    IHazard, IOwnable<WilyCapsuleTentacle> {

    companion object {
        const val TAG = "WilyCapsuleTentacleJoint"
        private const val SPRITE_SIZE = 1f
        private const val BODY_RADIUS = 0.5f
        private val regions = ObjectMap<Int, TextureRegion>()
    }

    override var owner: WilyCapsuleTentacle? = null

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.WILY_FINAL_BOSS.source)
            for (i in 0..2) regions.put(i, atlas.findRegion("phase_2/joints/$i"))
        }
        super.init(*params)
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val center = spawnProps.get(ConstKeys.CENTER, Vector2::class)!!
        body.setCenter(center)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(BODY_RADIUS * 2f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(BODY_RADIUS * ConstVals.PPM))
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
                .also { it.setSize(SPRITE_SIZE * ConstVals.PPM) }
        )
        .preProcess { _, sprite ->
            sprite.setCenter(body.getCenter())

            val anchor = owner!!.getAnchor(GameObjectPools.fetch(Vector2::class))!!
            val target = owner!!.getTarget(GameObjectPools.fetch(Vector2::class))!!
            val dx = target.x - anchor.x
            val dy = target.y - anchor.y

            // Base sprite points left; flip X when pointing right, flip Y when pointing down
            sprite.setFlip(dx > 0f, dy < 0f)

            // Angle from the y-axis in [0°, 90°]: 0° = vertical, 90° = horizontal
            val angleFromVertical = MathUtils.atan2(abs(dx), abs(dy)) * MathUtils.radiansToDegrees

            // Region 0 = 0° (vertical), Region 1 = 22.5°, Region 2 = 45° (diagonal) — pick nearest
            val regionIndex = when {
                angleFromVertical < 11.25f -> 0
                angleFromVertical < 33.75f -> 1
                else -> 2
            }
            sprite.setRegion(regions[regionIndex])
        }
        .build()

    override fun getType() = EntityType.HAZARD
}
