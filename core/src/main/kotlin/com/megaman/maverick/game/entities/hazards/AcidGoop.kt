package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.extensions.objectSetOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ICullableEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.events.IEventListener
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
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

class AcidGoop(game: MegamanMaverickGame) : MegaGameEntity(game), IDamagerEntity, IHazard, ISpritesEntity, IAnimatedEntity,
    IBodyEntity, ICullableEntity, IEventListener {

    companion object {
        const val TAG = "AcidGoop"
        private const val FALL_GRAVITY = -22.5f
        private const val DISSIPATE_GRAVITY = -1.5f
        private const val DISSIPATE_DUR = 0.15f
        private var fallingRegion: TextureRegion? = null
        private var splatRegion: TextureRegion? = null
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.BEGIN_ROOM_TRANS)

    private val dissipateTimer = Timer(DISSIPATE_DUR)
    private var dissipating = false

    override fun init(vararg params: Any) {
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
        sprite.setSize(2f * ConstVals.PPM, ConstVals.PPM.toFloat())
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
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
