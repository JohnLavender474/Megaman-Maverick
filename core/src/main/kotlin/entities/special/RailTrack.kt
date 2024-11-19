package com.megaman.maverick.game.entities.special


import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
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
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.BlocksFactory
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.FixtureLabel

class RailTrack(game: MegamanMaverickGame) : MegaGameEntity(game), ICullableEntity, ISpritesEntity {

    companion object {
        const val TAG = "RailTrack"
        private const val DROPS = "drops"
        private const val PLATFORM_SPEED = 2.75f
        private var leftTrackRegion: TextureRegion? = null
        private var rightTrackRegion: TextureRegion? = null
        private var middleTrackRegion: TextureRegion? = null
        private var dropTrackRegion: TextureRegion? = null
    }

    private lateinit var drops: Array<GameRectangle>
    private lateinit var bounds: GameRectangle
    private var platform: RailTrackPlatform? = null
    private var platformRight = false

    override fun getEntityType() = EntityType.SPECIAL

    override fun init() {
        if (leftTrackRegion == null || rightTrackRegion == null || middleTrackRegion == null || dropTrackRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SPECIALS_1.source)
            leftTrackRegion = atlas.findRegion("$TAG/Left")
            rightTrackRegion = atlas.findRegion("$TAG/Right")
            middleTrackRegion = atlas.findRegion("$TAG/Middle")
            dropTrackRegion = atlas.findRegion("$TAG/Drop")
        }
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
        addComponent(SpritesComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        val spawn = bounds.getCenterLeftPoint()
        val width = (bounds.width / ConstVals.PPM).toInt()

        val dropIndices = if (spawnProps.containsKey(DROPS))
            spawnProps.get(DROPS, String::class)!!
                .split(",")
                .map { it.toInt() }
                .toObjectSet()
        else Array()

        drops = dropIndices.map {
            val drop = GameRectangle()
            drop.setSize(ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())
            drop.setCenterLeftToPoint(Vector2(spawn.x + it * ConstVals.PPM, spawn.y))
            drop
        }.toGdxArray()

        val leftSprite = GameSprite()
        leftSprite.setSize(ConstVals.PPM.toFloat())
        leftSprite.setPosition(spawn, Position.CENTER_LEFT)
        leftSprite.setRegion(leftTrackRegion!!)
        sprites.put(ConstKeys.LEFT, leftSprite)

        for (i in 1 until width - 1) {
            val middleSprite = GameSprite()
            middleSprite.setSize(ConstVals.PPM.toFloat())
            val position = Vector2(spawn.x + i * ConstVals.PPM, spawn.y)
            middleSprite.setPosition(position, Position.CENTER_LEFT)
            val region = if (dropIndices.contains(i)) dropTrackRegion else middleTrackRegion
            middleSprite.setRegion(region!!)
            sprites.put(ConstKeys.MIDDLE + i, middleSprite)
        }

        val rightSprite = GameSprite()
        rightSprite.setSize(ConstVals.PPM.toFloat())
        val rightPosition = Vector2(spawn.x + (width - 1) * ConstVals.PPM, spawn.y)
        rightSprite.setPosition(rightPosition, Position.CENTER_LEFT)
        rightSprite.setRegion(rightTrackRegion!!)
        sprites.put(ConstKeys.RIGHT, rightSprite)

        platform = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.RAIL_TRACK_PLATFORM)!! as RailTrackPlatform
        val platformSpawn = spawnProps.get(ConstKeys.CHILD, Int::class)!!
        platformRight = spawnProps.get(ConstKeys.RIGHT, Boolean::class)!!
        platform!!.spawn(
            props(
                ConstKeys.POSITION pairTo Vector2(spawn.x + platformSpawn * ConstVals.PPM, spawn.y),
                ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
                ConstKeys.TRAJECTORY pairTo PLATFORM_SPEED * ConstVals.PPM * if (platformRight) 1 else -1
            )
        )
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        platform?.destroy()
        platform = null
        sprites.clear()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        platform!!.body.setCenterY(bounds.getCenter().y + 0.35f * ConstVals.PPM)
        if (platformRight && platform!!.pivot.getMaxX() >= bounds.getMaxX() - 0.5f * ConstVals.PPM) {
            platform!!.body.physics.velocity.x = PLATFORM_SPEED * ConstVals.PPM * -1f
            platformRight = false
        } else if (!platformRight && platform!!.pivot.x <= bounds.getX() + 0.5f * ConstVals.PPM) {
            platform!!.body.physics.velocity.x = PLATFORM_SPEED * ConstVals.PPM
            platformRight = true
        }

        val pivot = platform!!.pivot
        if (platform!!.dropped && drops.none { drop -> pivot.x >= drop.x && pivot.getMaxX() <= drop.getMaxX() })
            platform!!.raise()
        else if (!platform!!.dropped && drops.any { drop -> pivot.x >= drop.x && pivot.getMaxX() <= drop.getMaxX() })
            platform!!.drop()
    })

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this,
                objectSetOf(EventType.BEGIN_ROOM_TRANS)
            )
        )
    )
}

class RailTrackPlatform(game: MegamanMaverickGame) : Block(game), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "RailTrackPlatform"
        private var platformRegion: TextureRegion? = null
        private var platformDropRegion: TextureRegion? = null
    }

    val pivot = GameRectangle().setSize(0.1f * ConstVals.PPM, 0.1f * ConstVals.PPM)
    val dropped: Boolean
        get() = !body.physics.collisionOn

    override fun init() {
        if (platformRegion == null || platformDropRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SPECIALS_1.source)
            platformRegion = atlas.findRegion("${RailTrack.TAG}/Platform")
            platformDropRegion = atlas.findRegion("${RailTrack.TAG}/PlatformDrop")
        }
        super.init()
        addComponent(AudioComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body }), debug = true))
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
    }

    internal fun drop() {
        body.physics.collisionOn = false
    }

    internal fun raise() {
        body.physics.collisionOn = true
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        pivot.setPosition(body.getPosition())
    })

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            val position = body.getTopCenterPoint()
            position.y += 0.1f * ConstVals.PPM
            _sprite.setPosition(position, Position.TOP_CENTER)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (body.physics.collisionOn) "platform" else "drop" }
        val animations = objectMapOf<String, IAnimation>(
            "platform" pairTo Animation(platformRegion!!),
            "drop" pairTo Animation(platformDropRegion!!, 1, 3, 0.025f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
