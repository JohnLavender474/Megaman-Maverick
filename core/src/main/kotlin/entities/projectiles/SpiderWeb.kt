package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IFaceable

import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.controllers.MegaControllerButtons
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.BodyComponentCreator

class SpiderWeb(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity, IEventListener, IFaceable {

    companion object {
        const val TAG = "SpiderWeb"
        private const val PRESSES_TO_GET_UNSTUCK = 3
        private const val BLINK_WHITE_DUR = 0.3f
        private const val SIZE_INCREASE_PER_SECOND = 0.5f
        private val BUTTONS_TO_GET_UNSTUCK = gdxArrayOf<Any>(
            MegaControllerButtons.UP,
            MegaControllerButtons.DOWN,
            MegaControllerButtons.LEFT,
            MegaControllerButtons.RIGHT,
            MegaControllerButtons.A,
            MegaControllerButtons.B
        )
        private val WEBS_STUCK_TO_MEGAMAN = ObjectSet<SpiderWeb>()
        private var blinkWhiteRegion: TextureRegion? = null
        private var grayRegion: TextureRegion? = null
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.PLAYER_JUST_DIED)
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
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        game.eventsMan.addListener(this)

        body.setSize(0.5f * ConstVals.PPM)
        val spawn = if (spawnProps.containsKey(ConstKeys.POSITION)) spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        else spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)
        trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2(), Vector2::class)

        facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
        stuckToMegaman = false
        presses = 0

        blinkWhiteTimer.setToEnd()
    }

    override fun onDestroy() {
        game.eventsMan.removeListener(this)
        if (stuckToMegaman) {
            explode()
            WEBS_STUCK_TO_MEGAMAN.remove(this)
            if (WEBS_STUCK_TO_MEGAMAN.isEmpty) {
                stuckToMegaman = false
                megaman.canMove = true
                megaman.setAllBehaviorsAllowed(true)
                getMegaman().body.physics.gravityOn = true
            }
        }
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.PLAYER_JUST_DIED -> destroy()
        }
    }

    private fun explode() {
        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)!!
        val props = props(
            ConstKeys.POSITION pairTo body.getCenter(),
            ConstKeys.SOUND pairTo SoundAsset.EXPLOSION_2_SOUND
        )
        explosion.spawn(props)
    }

    private fun stickToMegaman() {
        stuckToMegaman = true

        body.setCenter(getMegaman().body.getCenter())
        trajectory.setZero()

        megaman.setAllBehaviorsAllowed(false)
        getMegaman().body.physics.velocity.setZero()
        getMegaman().body.physics.gravityOn = false
        megaman.canMove = false

        WEBS_STUCK_TO_MEGAMAN.add(this)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
body.physics.applyFrictionY = false
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
        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))
        return BodyComponentCreator.create(this, body)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        blinkWhiteTimer.update(it)
        if (!stuckToMegaman && !megaman.dead && body.overlaps(getMegaman().body as Rectangle)) stickToMegaman()
        else if (stuckToMegaman) {
            body.setCenter(getMegaman().body.getCenter())
            if (game.controllerPoller.isAnyJustReleased(BUTTONS_TO_GET_UNSTUCK)) {
                presses++
                blinkWhiteTimer.reset()
                if (overlapsGameCamera()) requestToPlaySound(SoundAsset.THUMP_SOUND, false)
                if (presses >= PRESSES_TO_GET_UNSTUCK) destroy()
            }
        }
    })

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 5))
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setSize(body.width, body.height)
            _sprite.setCenter(body.getCenter())
            _sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier = { if (blinkWhiteTimer.isFinished()) "gray" else "blink_white" }
        val animations = objectMapOf<String, IAnimation>(
            "gray" pairTo Animation(grayRegion!!),
            "blink_white" pairTo Animation(blinkWhiteRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
