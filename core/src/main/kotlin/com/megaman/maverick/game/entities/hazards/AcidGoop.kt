package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
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
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.*

class AcidGoop(game: MegamanMaverickGame) : MegaGameEntity(game), IDamager, IHazard, ISpritesEntity, IAnimatedEntity,
    IBodyEntity, ICullableEntity, IEventListener {

    companion object {
        const val TAG = "AcidGoop"
        private const val FALL_GRAVITY = -0.15f
        private const val DISSIPATE_GRAVITY = -0.01f
        private const val DISSIPATE_DUR = 0.15f
        private var fallingRegion: TextureRegion? = null
        private var splatRegion: TextureRegion? = null
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.BEGIN_ROOM_TRANS)

    private val dissipateTimer = Timer(DISSIPATE_DUR)
    private var dissipating = false

    override fun init() {
        if (fallingRegion == null || splatRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            fallingRegion = atlas.findRegion("$TAG/falling")
            splatRegion = atlas.findRegion("$TAG/landed")
        }
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setTopCenterToPoint(spawn)

        body.physics.gravityOn = false

        dissipateTimer.reset()
        dissipating = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        game.eventsMan.removeListener(this)
    }

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "onEvent(): event=$event")
        if (event.key == EventType.BEGIN_ROOM_TRANS) destroy()
    }

    fun setToFall() {
        GameLogger.debug(TAG, "setToFall()")
        body.physics.gravityOn = true
    }

    private fun shouldStartDissipating() =
        body.isSensing(BodySense.FEET_ON_GROUND) || body.isSensing(BodySense.IN_WATER)

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (!dissipating && shouldStartDissipating()) {
            body.physics.velocity.setZero()
            dissipating = true
        }

        if (dissipating) {
            dissipateTimer.update(delta)
            if (dissipateTimer.isFinished()) {
                if (overlapsGameCamera()) playSoundNow(SoundAsset.WHOOSH_SOUND, false)
                destroy()
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(0.5f * ConstVals.PPM)

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            val gravity = ConstVals.PPM * if (dissipating) DISSIPATE_GRAVITY else FALL_GRAVITY
            body.physics.gravity.y = gravity
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.WATER_LISTENER)
        )
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(1.5f * ConstVals.PPM, 0.75f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { _, _ ->
            sprite.setPosition(
                body.getPositionPoint(Position.BOTTOM_CENTER),
                Position.BOTTOM_CENTER
            )
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { if (dissipating) "dissipating" else "falling" }
        val animations = objectMapOf<String, IAnimation>(
            "falling" pairTo Animation(fallingRegion!!, 2, 3, 0.1f, false),
            "dissipating" pairTo Animation(splatRegion!!, 1, 3, 0.05f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
