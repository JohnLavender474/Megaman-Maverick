package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
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
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getSize

class SpiderWeb(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity, IEventListener {

    companion object {
        const val TAG = "SpiderWeb"
        private const val PRESSES_TO_GET_UNSTUCK = 3
        private const val BLINK_WHITE_DUR = 0.3f
        private const val START_SIZE = 0.75f
        private const val SIZE_INCREASE_PER_SECOND = 0.5f
        private val BUTTONS_TO_GET_UNSTUCK = gdxArrayOf<Any>(
            MegaControllerButton.UP,
            MegaControllerButton.DOWN,
            MegaControllerButton.LEFT,
            MegaControllerButton.RIGHT,
            MegaControllerButton.A,
            MegaControllerButton.B
        )
        private val WEBS_STUCK_TO_MEGAMAN = ObjectSet<SpiderWeb>()
        private var blinkWhiteRegion: TextureRegion? = null
        private var grayRegion: TextureRegion? = null
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.PLAYER_JUST_DIED)

    private val blinkWhiteTimer = Timer(BLINK_WHITE_DUR)
    private lateinit var trajectory: Vector2
    private var stuckToMegaman = false
    private var presses = 0

    override fun init() {
        if (grayRegion == null || blinkWhiteRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            grayRegion = atlas.findRegion("$TAG/Gray")
            blinkWhiteRegion = atlas.findRegion("$TAG/BlinkWhite")
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        game.eventsMan.addListener(this)

        body.setSize(START_SIZE * ConstVals.PPM)
        val spawn =
            if (spawnProps.containsKey(ConstKeys.POSITION)) spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
            else spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)
        trajectory = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2.Zero, Vector2::class)
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
                megaman().let {
                    it.canMove = true
                    it.setAllBehaviorsAllowed(true)
                    it.body.physics.gravityOn = true
                }
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

        body.setCenter(megaman().body.getCenter())
        trajectory.setZero()

        megaman().let { megaman ->
            megaman.setAllBehaviorsAllowed(false)
            megaman.body.physics.velocity.setZero()
            megaman.body.physics.gravityOn = false
            megaman.canMove = false
        }

        WEBS_STUCK_TO_MEGAMAN.add(this)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        val debugShapes = gdxArrayOf<() -> IDrawableShape?>({ body.getBounds() })
        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.velocity.set(trajectory)
            if (!stuckToMegaman) {
                val oldCenter = body.getCenter()
                val sizeIncrease = SIZE_INCREASE_PER_SECOND * ConstVals.FIXED_TIME_STEP * ConstVals.PPM
                body.setSize(body.getWidth() + sizeIncrease, body.getHeight() + sizeIncrease)
                body.setCenter(oldCenter)
            }
        }
        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))
        return BodyComponentCreator.create(this, body)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({
        blinkWhiteTimer.update(it)
        if (!stuckToMegaman && !megaman().dead &&
            body.getBounds().overlaps(megaman().body.getBounds())
        ) stickToMegaman()
        else if (stuckToMegaman) {
            body.setCenter(megaman().body.getCenter())
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
        spritesComponent.putUpdateFunction { _, _ ->
            val size = body.getSize()
            sprite.setSize(size.x, size.y)
            sprite.setCenter(body.getCenter())
            sprite.setFlip(trajectory.x < 0f, false)
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
