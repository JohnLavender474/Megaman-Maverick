package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
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
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
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
import com.megaman.maverick.game.levels.LevelUtils
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds

class LavaRiver(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ICullableEntity, ISpritesEntity,
    IAnimatedEntity {

    companion object {
        const val TAG = "LavaRiver"

        const val TOP = "top"
        const val FALL = "fall"
        const val INNER = "inner"
        const val FALL_START = "fall_start"

        private val animDefs = orderedMapOf(
            TOP pairTo AnimationDef(3, 1, 0.1f, true),
            "${TOP}_${ConstKeys.FROZEN}" pairTo AnimationDef(),

            INNER pairTo AnimationDef(3, 1, 0.1f, true),
            "${INNER}_${ConstKeys.FROZEN}" pairTo AnimationDef(),

            FALL_START pairTo AnimationDef(3, 1, 0.1f, true),
            "${FALL_START}_${ConstKeys.FROZEN}" pairTo AnimationDef(),

            FALL pairTo AnimationDef(3, 1, 0.1f, true),
            "${FALL}_${ConstKeys.FROZEN}" pairTo AnimationDef(),
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private lateinit var spawnRoom: String
    private lateinit var type: String

    private var left = false
    private var active = true
    private var frozen = false
    private var hidden = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            animDefs.keys().forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!

        type = spawnProps.get(ConstKeys.TYPE, String::class)!!
        left = spawnProps.getOrDefault(ConstKeys.LEFT, false, Boolean::class)
        active = spawnProps.getOrDefault(ConstKeys.ACTIVE, true, Boolean::class)
        hidden = spawnProps.getOrDefault(ConstKeys.HIDDEN, false, Boolean::class)
        frozen = spawnProps.getOrDefault(
            ConstKeys.FROZEN, LevelUtils.isInfernoManLevelFrozen(game.state), Boolean::class
        )
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this, objectSetOf(EventType.END_ROOM_TRANS), cull@{ event ->
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

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite())
        .updatable { _, sprite ->
            sprite.hidden = hidden

            sprite.setFlip(left, false)

            sprite.setBounds(body.getBounds())

            sprite.priority.value = 0
            sprite.priority.section = if (frozen) DrawingSection.PLAYGROUND else DrawingSection.FOREGROUND
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier keySupplier@{
                    var key = type
                    if (frozen) key = "${key}_${ConstKeys.FROZEN}"
                    return@keySupplier key
                }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val (rows, columns, durations, loop) = entry.value
                        animations.put(key, Animation(regions[key], rows, columns, durations, loop))
                    }
                }
                .build()
        )
        .build()

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

        body.preProcess.put(ConstKeys.DEATH) {
            deathFixture.setActive(active && !frozen)
            deathBounds.set(body)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
