package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.*
import com.megaman.maverick.game.entities.special.ToxicWater
import com.megaman.maverick.game.entities.special.Water
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.utils.misc.GravityUtils
import com.megaman.maverick.game.world.body.*

class MagmaFlame(game: MegamanMaverickGame) : MegaGameEntity(game), IFireEntity, IBodyEntity, ISpritesEntity,
    IAnimatedEntity, IAudioEntity, IOwnable<IGameEntity>, IDamager, IHazard, IDirectional {

    companion object {
        const val TAG = "MagmaFlame"

        private const val DURATION = 0.75f

        private const val GRAVITY = 0.15f
        private const val GROUND_GRAVITY = 0.01f

        private const val BODY_SIZE = 0.5f

        private val DO_NOT_SPAWN_TAGS = objectSetOf(Water.TAG, ToxicWater.TAG)

        private var region: TextureRegion? = null
    }

    override var direction: Direction
        get() = body.direction
        set(value) {
            body.direction = value
        }
    override var owner: IGameEntity? = null

    private val timer = Timer(DURATION)
    private val outEntities = OrderedSet<MegaGameEntity>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.HAZARDS_1.source, TAG)
        super.init()
        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun canSpawn(spawnProps: Properties): Boolean {
        if (!super.canSpawn(spawnProps)) return false

        val bounds = GameObjectPools.fetch(GameRectangle::class)
            .setSize(BODY_SIZE * ConstVals.PPM)
            .also {
                val direction = spawnProps.getOrDefault(ConstKeys.DIRECTION, Direction.UP, Direction::class)
                val position = DirectionPositionMapper.getPosition(direction).opposite()
                val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
                it.positionOnPoint(spawn, position)
            }

        MegaGameEntities.getOfTags(outEntities, DO_NOT_SPAWN_TAGS)
        var canSpawn = outEntities.none { it is IBodyEntity && it.body.getBounds().overlaps(bounds) }
        outEntities.clear()

        val bodyType = spawnProps.getOrDefault("${ConstKeys.BODY}_${ConstKeys.TYPE}", BodyType.DYNAMIC, BodyType::class)
        if (canSpawn && bodyType == BodyType.DYNAMIC) {
            MegaGameEntities.getOfType(EntityType.BLOCK)
            canSpawn = outEntities.none { it is IBodyEntity && it.body.getBounds().overlaps(bounds) }
            outEntities.clear()
        }

        return canSpawn
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)

        direction = spawnProps.getOrDefault(ConstKeys.DIRECTION, megaman.direction, Direction::class)

        val position = DirectionPositionMapper.getPosition(direction).opposite()
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.positionOnPoint(spawn, position)

        val impulse = spawnProps.getOrDefault(ConstKeys.IMPULSE, Vector2.Zero, Vector2::class)
        body.physics.velocity.set(impulse)

        val bodyType = spawnProps.getOrDefault("${ConstKeys.BODY}_${ConstKeys.TYPE}", BodyType.DYNAMIC, BodyType::class)
        body.type = bodyType

        timer.reset()

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ATOMIC_FIRE_SOUND, false)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (body.type == BodyType.DYNAMIC && body.isSensing(BodySense.FEET_ON_GROUND)) timer.update(delta)
        if (timer.isFinished()) destroy()
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(BODY_SIZE * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.5f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        damagerFixture.drawingColor = Color.BLUE
        debugShapes.add { damagerFixture }

        val waterListenerFixture = Fixture(body, FixtureType.WATER_LISTENER, GameRectangle(body))
        waterListenerFixture.setHitByWaterReceiver {
            destroy()
            playSoundNow(SoundAsset.WHOOSH_SOUND, false)
        }
        body.addFixture(waterListenerFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            val gravity = when (body.type) {
                BodyType.DYNAMIC -> if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
                else -> GRAVITY
            }
            GravityUtils.setGravity(body, gravity * ConstVals.PPM)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.WATER_LISTENER))
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(ConstVals.PPM.toFloat())
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val position = DirectionPositionMapper.getInvertedPosition(direction)
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 2, 1, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
