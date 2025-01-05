package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setBounds
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds

class LavaRiver(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ICullableEntity, ISpritesEntity,
    IAnimatedEntity {

    companion object {
        const val TAG = "LavaRiver"

        private const val TOP = "top"
        private const val INNER = "inner"
        private const val FALL_START = "fall_start"
        private const val FALL = "fall"

        private val animDefs = orderedMapOf(
            TOP pairTo AnimationDef(3, 1, 0.1f, true),
            INNER pairTo AnimationDef(3, 1, 0.1f, true),
            FALL_START pairTo AnimationDef(3, 1, 0.1f, true),
            FALL pairTo AnimationDef(3, 1, 0.1f, true),
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private lateinit var spawnRoom: String

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            animDefs.keys().forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
        addComponent(SpritesComponent())
        addComponent(AnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        val type = spawnProps.get(ConstKeys.TYPE, String::class)!!
        val left = spawnProps.getOrDefault(ConstKeys.LEFT, false, Boolean::class)
        val index = 0 // spawnProps.getOrDefault(ConstKeys.INDEX, 0, Int::class)
        defineDrawables(bounds, type, left, index)
    }

    override fun onDestroy() {
        super.onDestroy()
        sprites.clear()
        animators.clear()
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this, objectSetOf(EventType.END_ROOM_TRANS), { event ->
                    val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                    val cull = room != spawnRoom
                    GameLogger.debug(
                        TAG,
                        "defineCullablesComponent(): currentRoom=$room, spawnRoom=$spawnRoom, cull=$cull"
                    )
                    cull
                }
            )
        )
    )

    private fun defineDrawables(bounds: GameRectangle, type: String, left: Boolean, index: Int) {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprites.put(TAG, sprite)
        putUpdateFunction(TAG) { _, _ ->
            sprite.setBounds(bounds)
            sprite.setFlip(left, false)
        }

        val animDef = animDefs[type]
        val animation = Animation(regions[type], animDef.rows, animDef.cols, animDef.durations, animDef.loop)

        var startTime = 0f
        for (i in 0 until index) startTime += animDef.durations[i]
        animation.setStartTime(startTime)
        animation.setCurrentTime(startTime)

        val animator = Animator(animation)

        putAnimator(sprite, animator)
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.drawingColor = Color.BLUE

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val deathFixture = Fixture(body, FixtureType.DEATH)
        deathFixture.putProperty(ConstKeys.INSTANT, true)
        body.addFixture(deathFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun getEntityType() = EntityType.HAZARD

    override fun getTag() = TAG
}
