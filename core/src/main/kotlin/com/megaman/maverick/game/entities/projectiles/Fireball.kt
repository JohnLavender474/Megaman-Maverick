package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IFireEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.SmokePuff
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.utils.extensions.toGdxRectangle
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.*

class Fireball(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity, IFireEntity {

    companion object {
        const val TAG = "Fireball"

        const val BURST_ON_DAMAGE_INFLICTED = "burst_on_damage_inflicted"
        const val BURST_ON_HIT_BODY = "burst_on_hit_body"
        const val BURST_ON_HIT_BLOCK = "burst_on_hit_block"

        private const val BURST_CULL_DUR = 0.5f

        private const val NORMAL_SIZE = 0.25f
        private const val BURST_SIZE = 1f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private lateinit var burstCullTimer: Timer
    private lateinit var burstDirection: Direction

    private var burstOnHitBlock = true
    private var burst = false

    private var canDamage = true

    override fun init() {
        if (regions.isEmpty) {
            val fireballAtlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            regions.put("fireball", fireballAtlas.findRegion("Fire/Fireball"))

            val flameAtlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            regions.put("burst", flameAtlas.findRegion("Flame"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)

        val spawn = spawnProps.getOrDefault(ConstKeys.POSITION, Vector2.Zero, Vector2::class)
        body.setCenter(spawn)

        val trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2.Zero, Vector2::class)
        body.physics.velocity.set(trajectory)

        val cullTime = spawnProps.getOrDefault(ConstKeys.CULL_TIME, BURST_CULL_DUR, Float::class)
        burstCullTimer = Timer(cullTime)

        val gravity = spawnProps.getOrDefault(ConstKeys.GRAVITY, Vector2.Zero, Vector2::class)
        body.physics.gravity.set(gravity)

        burstOnHitBlock = spawnProps.getOrDefault(BURST_ON_HIT_BLOCK, true, Boolean::class)

        burst = false
        burstDirection = Direction.UP

        canDamage = spawnProps.getOrDefault(ConstKeys.DAMAGER, true, Boolean::class)
    }

    override fun explodeAndDie(vararg params: Any?) {
        burst = true

        body.physics.gravity.setZero()

        val feetBounds = params[0] as GameRectangle
        val hitBounds = params[1] as GameRectangle
        burstDirection = UtilMethods.getOverlapPushDirection(feetBounds, hitBounds) ?: Direction.UP

        val overlap = GameObjectPools.fetch(Rectangle::class)
        Intersector.intersectRectangles(feetBounds.toGdxRectangle(), hitBounds.toGdxRectangle(), overlap)

        val position = DirectionPositionMapper.getInvertedPosition(burstDirection)
        body.positionOnPoint(overlap.toGameRectangle().getPositionPoint(position), position)

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ATOMIC_FIRE_SOUND, false)
    }

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        body.physics.velocity.x *= -1f
        requestToPlaySound(SoundAsset.DINK_SOUND, false)
    }

    override fun hitWater(waterFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        destroy()

        val spawn = GameObjectPools.fetch(Vector2::class).set(body.getCenter().x, waterFixture.getShape().getMaxY())

        val puff = MegaEntityFactory.fetch(SmokePuff::class)!!
        puff.spawn(props(ConstKeys.POSITION pairTo spawn, ConstKeys.OWNER pairTo owner))

        playSoundNow(SoundAsset.WHOOSH_SOUND, false)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        if (burst) {
            body.physics.velocity.setZero()
            burstCullTimer.update(it)
        }

        if (burstCullTimer.isFinished()) {
            destroy()

            val position = when (burstDirection) {
                Direction.UP -> body.getPositionPoint(Position.BOTTOM_CENTER)
                Direction.DOWN -> body.getPositionPoint(Position.TOP_CENTER)
                Direction.LEFT -> body.getPositionPoint(Position.CENTER_RIGHT)
                Direction.RIGHT -> body.getPositionPoint(Position.CENTER_LEFT)
            }

            val puff = MegaEntityFactory.fetch(SmokePuff::class)!!
            puff.spawn(
                props(
                    ConstKeys.OWNER pairTo owner,
                    ConstKeys.POSITION pairTo position,
                    ConstKeys.DIRECTION pairTo burstDirection
                )
            )
        }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(0.25f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.setHitByBlockReceiver(ProcessState.BEGIN) { block, _ ->
            if (burstOnHitBlock) explodeAndDie(feetFixture.getShape().getBoundingRectangle(), block.body.getBounds())
        }
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val damagerBounds = GameRectangle()
        val damagerFixture = Fixture(body, FixtureType.DAMAGER, damagerBounds)
        damagerFixture.attachedToBody = false
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        val projectileBounds = GameRectangle()
        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, projectileBounds)
        projectileFixture.attachedToBody = false
        body.addFixture(projectileFixture)

        body.preProcess.put(ConstKeys.FIXTURES) {
            val size = if (burst) BURST_SIZE else NORMAL_SIZE
            damagerBounds.setSize(size * ConstVals.PPM)
            projectileBounds.setSize(size * ConstVals.PPM)

            val position = when {
                burst -> DirectionPositionMapper.getInvertedPosition(burstDirection)
                else -> Position.BOTTOM_CENTER
            }
            val bodyPoint = body.getPositionPoint(position)
            damagerBounds.positionOnPoint(bodyPoint, position)
            projectileBounds.positionOnPoint(bodyPoint, position)

            damagerFixture.setActive(canDamage)
            damagerFixture.drawingColor = if (damagerFixture.isActive()) Color.RED else Color.GRAY
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { delta, sprite ->
            val size = if (burst) 1f else 2f
            sprite.setSize(size * ConstVals.PPM)

            val position = if (burst) DirectionPositionMapper.getInvertedPosition(burstDirection) else Position.CENTER
            val bodyPosition = body.getPositionPoint(position)
            sprite.setPosition(bodyPosition, position)

            sprite.setOriginCenter()
            sprite.rotation = burstDirection.rotation
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { if (burst) "burst" else "fireball" }
        val animations = objectMapOf<String, IAnimation>(
            "burst" pairTo Animation(regions["burst"], 1, 4, 0.1f, true),
            "fireball" pairTo Animation(regions["fireball"], 2, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
