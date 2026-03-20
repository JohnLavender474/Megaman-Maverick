package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
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
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType

class WilyPlaneBody(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAudioEntity,
    IDamager, IHazard {

    companion object {
        const val TAG = "WilyPlaneBody"
        private const val FALL_SPEED = 16f
        private const val SPRITE_WIDTH = 16f
        private const val SPRITE_HEIGHT = 16f
        private const val TTL = 1f
        private const val EXPLOSION_DELAY = 0.25f
        private const val BLINK_DELAY = 0.1f
        private var region: TextureRegion? = null
    }

    private val current = Vector2()
    private val target = Vector2()

    private val ttl = Timer(TTL)
    private var targetReached = false

    private var blink = false
    private val blinkDelay = Timer(BLINK_DELAY)
    private val explosionDelay = Timer(EXPLOSION_DELAY)

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.WILY_FINAL_BOSS.source, TAG)
        super.init()
        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        current.set(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)

        target.set(spawnProps.get(ConstKeys.TARGET, Vector2::class)!!)
        targetReached = false

        ttl.reset()
        explosionDelay.reset()

        blink = false
        blinkDelay.reset()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        blinkDelay.update(delta)
        if (blinkDelay.isFinished()) {
            blink = !blink
            blinkDelay.reset()
        }

        explosionDelay.update(delta)
        if (explosionDelay.isFinished()) {
            val position = current.cpy().add(
                UtilMethods.getRandom(-1f, 1f) * ConstVals.PPM,
                UtilMethods.getRandom(-1f, 1f) * ConstVals.PPM,
            )
            val explosion = MegaEntityFactory.fetch(Explosion::class)!!
            explosion.spawn(
                props(
                    ConstKeys.DAMAGER pairTo false,
                    ConstKeys.POSITION pairTo position
                )
            )
            requestToPlaySound(SoundAsset.EXPLOSION_2_SOUND, false)
            explosionDelay.reset()
        }

        if (!targetReached) {
            val movement = target.cpy().sub(current).nor().scl(FALL_SPEED * ConstVals.PPM * delta)
            current.add(movement)

            if (current.y <= target.y) {
                current.set(target)
                targetReached = true
            }
        } else {
            ttl.update(delta)
            if (ttl.isFinished()) destroy()
        }

        body.setCenter(current)
    })

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(region!!, DrawingPriority(DrawingSection.FOREGROUND))
                .also { it.setSize(SPRITE_WIDTH * ConstVals.PPM, SPRITE_HEIGHT * ConstVals.PPM) }
        )
        .preProcess { _, sprite ->
            sprite.setCenter(current)
            sprite.hidden = blink
        }
        .build()

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(8f * ConstVals.PPM, 4f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        val bodyDamager = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(bodyDamager)

        val leftThrusterDamager =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(2f * ConstVals.PPM, 3f * ConstVals.PPM))
        leftThrusterDamager.attachedToBody = false
        body.addFixture(leftThrusterDamager)

        val leftThrusterShield =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(2f * ConstVals.PPM, 3f * ConstVals.PPM))
        leftThrusterShield.attachedToBody = false
        body.addFixture(leftThrusterShield)
        debugShapes.add add@{
            leftThrusterShield.drawingColor = if (leftThrusterShield.isActive()) Color.RED else Color.GRAY
            return@add leftThrusterShield
        }

        val rightThrusterDamager =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(2f * ConstVals.PPM, 3f * ConstVals.PPM))
        rightThrusterDamager.attachedToBody = false
        body.addFixture(rightThrusterDamager)

        val rightThrusterShield =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(2f * ConstVals.PPM, 3f * ConstVals.PPM))
        rightThrusterShield.attachedToBody = false
        body.addFixture(rightThrusterShield)
        debugShapes.add add@{
            rightThrusterShield.drawingColor = if (rightThrusterShield.isActive()) Color.RED else Color.GRAY
            return@add rightThrusterShield
        }

        val leftWingHoverDamager =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(3f * ConstVals.PPM, 1.5f * ConstVals.PPM))
        leftWingHoverDamager.offsetFromBodyAttachment.set(-6f * ConstVals.PPM, 0f)
        body.addFixture(leftWingHoverDamager)

        val leftWingHoverShield =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(3f * ConstVals.PPM, 1.5f * ConstVals.PPM))
        leftWingHoverShield.offsetFromBodyAttachment.set(-6f * ConstVals.PPM, 0f)
        body.addFixture(leftWingHoverShield)
        debugShapes.add add@{
            leftWingHoverShield.drawingColor = if (leftWingHoverShield.isActive()) Color.BLUE else Color.GRAY
            return@add leftWingHoverShield
        }

        val rightWingHoverDamager =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(3f * ConstVals.PPM, 1.5f * ConstVals.PPM))
        rightWingHoverDamager.offsetFromBodyAttachment.set(6f * ConstVals.PPM, 0f)
        body.addFixture(rightWingHoverDamager)

        val rightWingHoverShield =
            Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(3f * ConstVals.PPM, 1.5f * ConstVals.PPM))
        rightWingHoverShield.offsetFromBodyAttachment.set(-6f * ConstVals.PPM, 0f)
        body.addFixture(rightWingHoverShield)
        debugShapes.add add@{
            rightWingHoverShield.drawingColor = if (rightWingHoverShield.isActive()) Color.BLUE else Color.GRAY
            return@add rightWingHoverShield
        }

        body.preProcess.put(ConstKeys.DEFAULT) {
            bodyDamager.offsetFromBodyAttachment.y = -0.25f * ConstVals.PPM

            leftThrusterDamager.setActive(true)
            (leftThrusterDamager.rawShape as GameRectangle)
                .setCenterX(body.getMaxX())
                .setMaxY(body.getMaxY() - 1f * ConstVals.PPM)
            (leftThrusterShield.rawShape as GameRectangle)
                .setCenterX(body.getMaxX())
                .setMaxY(body.getMaxY() - 1f * ConstVals.PPM)

            rightThrusterDamager.setActive(true)
            (rightThrusterDamager.rawShape as GameRectangle)
                .setCenterX(body.getMaxX())
                .setMaxY(body.getMaxY() - 1f * ConstVals.PPM)
            (rightThrusterShield.rawShape as GameRectangle)
                .setCenterX(body.getMaxX())
                .setMaxY(body.getMaxY() - 1f * ConstVals.PPM)
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun getType() = EntityType.HAZARD
}
