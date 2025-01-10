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
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds

class LavaRiver(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ICullableEntity, ISpritesEntity,
    IAnimatedEntity {

    companion object {
        const val TAG = "LavaRiver"

        const val TOP = "top"
        const val INNER = "inner"
        const val FALL = "fall"
        const val FALL_START = "fall_start"

        private val animDefs = orderedMapOf(
            TOP pairTo AnimationDef(3, 1, 0.1f, true),
            INNER pairTo AnimationDef(3, 1, 0.1f, true),
            FALL_START pairTo AnimationDef(3, 1, 0.1f, true),
            FALL pairTo AnimationDef(3, 1, 0.1f, true),
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    internal var hidden = false

    private lateinit var spawnRoom: String
    private var ownCull = true

    override fun init() {
        GameLogger.debug(TAG, "init()")

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
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        ownCull = spawnProps.getOrDefault("${ConstKeys.OWN}_${ConstKeys.CULL}", true, Boolean::class)
        if (ownCull) spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!

        val type = spawnProps.get(ConstKeys.TYPE, String::class)!!
        val left = spawnProps.getOrDefault(ConstKeys.LEFT, false, Boolean::class)
        defineDrawables(type, left)

        val active = spawnProps.getOrDefault(ConstKeys.ACTIVE, true, Boolean::class)
        setActive(active)

        hidden = spawnProps.getOrDefault(ConstKeys.HIDDEN, false, Boolean::class)
    }

    internal fun setActive(active: Boolean) {
        body.physics.collisionOn = active
        body.forEachFixture { it.setActive(active) }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")

        super.onDestroy()
        sprites.clear()
        animators.clear()
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this, objectSetOf(EventType.END_ROOM_TRANS), cull@{ event ->
                    if (!ownCull) return@cull false

                    val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                    val doCull = room != spawnRoom
                    GameLogger.debug(
                        TAG,
                        "defineCullablesComponent(): currentRoom=$room, spawnRoom=$spawnRoom, cull=$doCull"
                    )
                    return@cull doCull
                }
            )
        )
    )

    private fun defineDrawables(type: String, left: Boolean) {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
        sprites.put(TAG, sprite)
        putUpdateFunction(TAG) { _, _ ->
            sprite.hidden = hidden
            sprite.setFlip(left, false)
            sprite.setBounds(body.getBounds())
        }

        val animDef = animDefs[type]
        val animation = Animation(regions[type], animDef.rows, animDef.cols, animDef.durations, animDef.loop)
        val animator = Animator(animation)
        putAnimator(sprite, animator)
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.BLUE

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { if (body.physics.collisionOn) body.getBounds() else null }

        val deathBounds = GameRectangle()
        val deathFixture = Fixture(body, FixtureType.DEATH, deathBounds)
        deathFixture.putProperty(ConstKeys.INSTANT, true)
        body.addFixture(deathFixture)

        body.preProcess.put(ConstKeys.DEATH) { deathBounds.set(body) }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun getEntityType() = EntityType.HAZARD

    override fun getTag() = TAG
}
