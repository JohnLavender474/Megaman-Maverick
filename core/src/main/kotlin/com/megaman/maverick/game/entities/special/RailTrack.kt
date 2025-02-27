package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.*
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class RailTrack(game: MegamanMaverickGame) : MegaGameEntity(game), ICullableEntity, ISpritesEntity {

    companion object {
        const val TAG = "RailTrack"

        private const val DROPS = "drops"
        private const val PLATFORM_SPEED = 2.75f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private val bounds = GameRectangle()
    private val drops = Array<GameRectangle>()

    private lateinit var spawnRoom: String

    private var platform: RailTrackPlatform? = null
    private var platformRight = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SPECIALS_1.source)
            gdxArrayOf("left", "right", "middle", "drop").forEach { key ->
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
        }
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
        addComponent(SpritesComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        bounds.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)
        val spawn = bounds.getPositionPoint(Position.CENTER_LEFT)
        val width = (bounds.getWidth() / ConstVals.PPM).toInt()

        val dropIndices = if (spawnProps.containsKey(DROPS))
            spawnProps.get(DROPS, String::class)!!
                .split(",")
                .map { it.toInt() }
                .toObjectSet()
        else Array()

        drops.addAll(dropIndices.map {
            val drop = GameRectangle()
            drop.setSize(ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())
            drop.setCenterLeftToPoint(Vector2(spawn.x + it * ConstVals.PPM, spawn.y))
            drop
        }.toGdxArray())

        val leftSprite = GameSprite()
        leftSprite.setSize(ConstVals.PPM.toFloat())
        leftSprite.setPosition(spawn, Position.CENTER_LEFT)
        leftSprite.setRegion(regions["left"])
        sprites.put(ConstKeys.LEFT, leftSprite)

        for (i in 1 until width - 1) {
            val middleSprite = GameSprite()
            middleSprite.setSize(ConstVals.PPM.toFloat())
            val position = Vector2(spawn.x + i * ConstVals.PPM, spawn.y)
            middleSprite.setPosition(position, Position.CENTER_LEFT)
            val region = if (dropIndices.contains(i)) regions["drop"] else regions["middle"]
            middleSprite.setRegion(region!!)
            sprites.put(ConstKeys.MIDDLE + i, middleSprite)
        }

        val rightSprite = GameSprite()
        rightSprite.setSize(ConstVals.PPM.toFloat())
        val rightPosition = Vector2(spawn.x + (width - 1) * ConstVals.PPM, spawn.y)
        rightSprite.setPosition(rightPosition, Position.CENTER_LEFT)
        rightSprite.setRegion(regions["right"])
        sprites.put(ConstKeys.RIGHT, rightSprite)

        platform = MegaEntityFactory.fetch(RailTrackPlatform::class)!!
        val platformSpawn = spawnProps.get(ConstKeys.CHILD, Int::class)!!
        platformRight = spawnProps.get(ConstKeys.RIGHT, Boolean::class)!!
        platform!!.spawn(
            props(
                ConstKeys.POSITION pairTo Vector2(spawn.x + platformSpawn * ConstVals.PPM, spawn.y),
                ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
                ConstKeys.TRAJECTORY pairTo PLATFORM_SPEED * ConstVals.PPM * if (platformRight) 1 else -1
            )
        )

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")

        super.onDestroy()

        platform?.destroy()
        platform = null

        drops.clear()

        sprites.clear()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        platform!!.body.setCenterY(bounds.getCenter().y + 0.35f * ConstVals.PPM)

        when {
            platformRight && platform!!.pivot.getMaxX() >= bounds.getMaxX() - 0.5f * ConstVals.PPM -> {
                platform!!.body.physics.velocity.x = PLATFORM_SPEED * ConstVals.PPM * -1f
                platformRight = false
            }

            !platformRight && platform!!.pivot.getX() <= bounds.getX() + 0.5f * ConstVals.PPM -> {
                platform!!.body.physics.velocity.x = PLATFORM_SPEED * ConstVals.PPM
                platformRight = true
            }
        }

        val pivot = platform!!.pivot
        when {
            platform!!.dropped && drops.none { drop ->
                pivot.getX() >= drop.getX() && pivot.getMaxX() <= drop.getMaxX()
            } -> platform!!.raise()

            !platform!!.dropped && drops.any { drop ->
                pivot.getX() >= drop.getX() && pivot.getMaxX() <= drop.getMaxX()
            } -> platform!!.drop()
        }
    })

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this,
                objectSetOf(EventType.END_ROOM_TRANS),
                predicate@{ event ->
                    val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                    return@predicate room != spawnRoom
                }
            )
        )
    )

    override fun getType() = EntityType.SPECIAL

    override fun getTag() = TAG
}

class RailTrackPlatform(game: MegamanMaverickGame) : Block(game), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "RailTrackPlatform"
        private val regions = ObjectMap<String, TextureRegion>()
    }

    val pivot = GameRectangle().setSize(0.1f * ConstVals.PPM, 0.1f * ConstVals.PPM)
    val dropped: Boolean
        get() = !body.physics.collisionOn

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SPECIALS_1.source)
            gdxArrayOf("platform", "platform_drop").forEach { key ->
                regions.put(key, atlas.findRegion("${RailTrack.TAG}/$key"))
            }
        }
        super.init()
        addComponent(AudioComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))
    }

    override fun onSpawn(spawnProps: Properties) {
        val position = spawnProps.remove(ConstKeys.POSITION) as Vector2
        val bounds = GameRectangle()
            .setSize(2f * ConstVals.PPM, 0.1f * ConstVals.PPM)
            .setPosition(position)
        spawnProps.put(ConstKeys.BOUNDS, bounds)
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        spawnProps.put(ConstKeys.BODY_LABELS, objectSetOf(BodyLabel.COLLIDE_DOWN_ONLY))
        spawnProps.put(
            ConstKeys.FIXTURE_LABELS,
            objectSetOf(FixtureLabel.NO_SIDE_TOUCHIE, FixtureLabel.NO_PROJECTILE_COLLISION)
        )
        spawnProps.put(ConstKeys.RESIST_ON, false)

        super.onSpawn(spawnProps)

        val trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Float::class)!!
        body.physics.velocity.x = trajectory

        body.physics.collisionOn = true

        blockFixture.setActive(true)
    }

    internal fun drop() {
        body.physics.collisionOn = false
        blockFixture.setActive(false)
    }

    internal fun raise() {
        body.physics.collisionOn = true
        blockFixture.setActive(true)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        pivot.setPosition(body.getPosition())
    })

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
                .also { sprite -> sprite.setSize(2f * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            val position = body.getPositionPoint(Position.TOP_CENTER)
            position.y += 0.1f * ConstVals.PPM
            sprite.setPosition(position, Position.TOP_CENTER)
        }
        .build()

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { if (body.physics.collisionOn) "platform" else "drop" }
        val animations = objectMapOf<String, IAnimation>(
            "platform" pairTo Animation(regions["platform"]),
            "drop" pairTo Animation(regions["platform_drop"], 1, 3, 0.025f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun getTag() = TAG
}
