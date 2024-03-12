package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Facing
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.isFacing
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.engine.events.Event
import com.engine.events.IEventListener
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.ControllerButton
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.defineProjectileComponents
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.BodyComponentCreator

class SpiderWeb(game: MegamanMaverickGame) : GameEntity(game), IProjectileEntity, IEventListener, IFaceable {

    companion object {
        const val TAG = "SpiderWeb"
        private const val PRESSES_TO_GET_UNSTUCK = 3
        private const val BLINK_WHITE_DUR = 0.3f
        private const val SIZE_INCREASE_PER_SECOND = 0.5f
        private val BUTTONS_TO_GET_UNSTUCK = gdxArrayOf<Any>(
            ControllerButton.UP,
            ControllerButton.DOWN,
            ControllerButton.LEFT,
            ControllerButton.RIGHT,
            ControllerButton.A,
            ControllerButton.B
        )
        private val WEBS_STUCK_TO_MEGAMAN = ObjectSet<SpiderWeb>()
        private var blinkWhiteRegion: TextureRegion? = null
        private var grayRegion: TextureRegion? = null
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.PLAYER_JUST_DIED)

    override var owner: IGameEntity? = null

    override lateinit var facing: Facing

    private val megaman = game.megaman
    private val blinkWhiteTimer = Timer(BLINK_WHITE_DUR)

    private lateinit var trajectory: Vector2

    private var stuckToMegaman = false
    private var presses = 0

    override fun init() {
        if (grayRegion == null || blinkWhiteRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            grayRegion = atlas.findRegion("SpiderWeb/Gray")
            blinkWhiteRegion = atlas.findRegion("SpiderWeb/BlinkWhite")
        }
        super<GameEntity>.init()
        defineProjectileComponents().forEach { addComponent(it) }
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        body.setSize(0.75f * ConstVals.PPM)
        val spawn = if (spawnProps.containsKey(ConstKeys.POSITION)) spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        else spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)
        trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2(), Vector2::class)

        facing = if (megaman.body.x < body.x) Facing.LEFT else Facing.RIGHT
        stuckToMegaman = false
        presses = 0

        blinkWhiteTimer.setToEnd()
    }

    override fun onDestroy() {
        super<GameEntity>.onDestroy()
        if (stuckToMegaman) {
            explode()
            WEBS_STUCK_TO_MEGAMAN.remove(this)
            if (WEBS_STUCK_TO_MEGAMAN.isEmpty) {
                stuckToMegaman = false
                megaman.canMove = true
                megaman.setAllBehaviorsAllowed(true)
                megaman.body.physics.gravityOn = true
            }
        }
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.PLAYER_JUST_DIED -> kill()
        }
    }

    private fun explode() {
        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)!!
        val props = props(
            ConstKeys.POSITION to body.getCenter(),
            ConstKeys.SOUND to SoundAsset.EXPLOSION_2_SOUND
        )
        game.gameEngine.spawn(explosion, props)
    }

    private fun stickToMegaman() {
        stuckToMegaman = true

        body.setCenter(megaman.body.getCenter())
        trajectory.setZero()

        megaman.setAllBehaviorsAllowed(false)
        megaman.body.physics.velocity.setZero()
        megaman.body.physics.gravityOn = false
        megaman.canMove = false

        WEBS_STUCK_TO_MEGAMAN.add(this)
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        val debugShapes = gdxArrayOf<() -> IDrawableShape?>({ body })
        body.preProcess.put(ConstKeys.DEFAULT) { delta ->
            body.physics.velocity.set(trajectory)
            if (!stuckToMegaman) {
                val oldCenter = body.getCenter()
                val sizeIncrease = SIZE_INCREASE_PER_SECOND * delta * ConstVals.PPM
                body.setSize(body.width + sizeIncrease, body.height + sizeIncrease)
                body.setCenter(oldCenter)
            }
        }
        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))
        return BodyComponentCreator.create(this, body)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, {
        blinkWhiteTimer.update(it)
        if (!stuckToMegaman && !megaman.dead && body.overlaps(megaman.body as Rectangle)) stickToMegaman()
        else if (stuckToMegaman) {
            body.setCenter(megaman.body.getCenter())
            if (game.controllerPoller.isAnyJustReleased(BUTTONS_TO_GET_UNSTUCK)) {
                presses++
                blinkWhiteTimer.reset()
                requestToPlaySound(SoundAsset.THUMP_SOUND, false)
                if (presses >= PRESSES_TO_GET_UNSTUCK) kill()
            }
        }
    })

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 5))
        val spritesComponent = SpritesComponent(this, TAG to sprite)
        spritesComponent.putUpdateFunction(TAG) { _, _sprite ->
            _sprite as GameSprite
            _sprite.setSize(body.width, body.height)
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
            _sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier = { if (blinkWhiteTimer.isFinished()) "gray" else "blink_white" }
        val animations = objectMapOf<String, IAnimation>(
            "gray" to Animation(grayRegion!!),
            "blink_white" to Animation(blinkWhiteRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}