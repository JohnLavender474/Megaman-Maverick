package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setBounds
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.decorations.FloatingEmber
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.levels.LevelUtils
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getRandomPositionInBounds

class LavaRiver(game: MegamanMaverickGame) : MegaGameEntity(game), ICullableEntity, ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "LavaRiver"

        const val TOP = "top"
        const val FALL = "fall"
        const val INNER = "inner"
        const val FALL_START = "fall_start"

        private const val EMBER_MIN_DELAY = 0.5f
        private const val EMBER_MAX_DELAY = 1f
        private const val EMBER_SPAWN_X_BUFFER = ConstVals.VIEW_WIDTH + 6
        private const val EMBER_SPAWN_Y_BUFFER = ConstVals.VIEW_HEIGHT + 12

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

    private lateinit var spawnRooms: HashSet<String>
    private lateinit var type: String

    private val bounds = GameRectangle()

    private var left = false
    private var frozen = false

    private val emberDelay = Timer()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            animDefs.keys().forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(defineSpritesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        this.bounds.set(bounds)

        spawnRooms = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!
            .split(",").map { it.trim() }.toHashSet()

        type = spawnProps.get(ConstKeys.TYPE, String::class)!!
        left = spawnProps.getOrDefault(ConstKeys.LEFT, false, Boolean::class)
        frozen = spawnProps.getOrDefault(
            ConstKeys.FROZEN, LevelUtils.isInfernoManLevelFrozen(game.state), Boolean::class
        )

        resetEmberDelay()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        when {
            !frozen -> {
                emberDelay.update(delta)
                if (emberDelay.isFinished()) {
                    val position = GameObjectPools.fetch(Vector2::class)
                        .setX(bounds.getRandomPositionInBounds().x)
                        .setY(bounds.getMaxY())

                    val canSpawnBounds = GameObjectPools.fetch(GameRectangle::class)
                        .setWidth(EMBER_SPAWN_X_BUFFER * ConstVals.PPM)
                        .setHeight(EMBER_SPAWN_Y_BUFFER * ConstVals.PPM)
                        .setCenter(game.getGameCamera().getRotatedBounds().getCenter())

                    if (canSpawnBounds.contains(position)) {
                        val ember = MegaEntityFactory.fetch(FloatingEmber::class)!!
                        ember.spawn(props(ConstKeys.POSITION pairTo position))
                        resetEmberDelay()
                    }
                }
            }
            else -> emberDelay.setToEnd()
        }
    })

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this, objectSetOf(EventType.END_ROOM_TRANS), cull@{ event ->
                    val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                    val doCull = !spawnRooms.contains(room)
                    GameLogger.debug(
                        TAG,
                        "defineCullablesComponent(): currentRoom=$room, spawnRooms=$spawnRooms, cull=$doCull"
                    )
                    return@cull doCull
                }
            )
        )
    )

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite())
        .preProcess { _, sprite ->
            sprite.setFlip(left, false)
            sprite.setBounds(bounds)
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

    private fun resetEmberDelay() {
        val duration = UtilMethods.getRandom(EMBER_MIN_DELAY, EMBER_MAX_DELAY)
        emberDelay.resetDuration(duration)
    }

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
