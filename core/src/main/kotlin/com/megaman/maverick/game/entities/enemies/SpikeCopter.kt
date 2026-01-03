package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.epsilonEquals
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.hazards.SpikeTeeth
import com.megaman.maverick.game.entities.utils.FreezableEntityHandler
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class SpikeCopter(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IFreezableEntity,
    IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "SpikeCopter"

        private const val VEL_X = 3f
        private const val DROP_Y = -12f
        private const val GRAVITY = -0.15f

        private const val DROP_TEETH_IMPULSE_Y = -8f
        private const val DROP_TEETH_DELAY = 1f
        private const val DROP_TEETH_DUR = 0.3f
        private const val DROP_DELAY = 0.25f

        private const val MAX_SPAWNED = 2

        private val animDefs = orderedMapOf(
            "drop" pairTo AnimationDef(),
            "fly" pairTo AnimationDef(2, 1, 0.1f, true),
            "drop_teeth" pairTo AnimationDef(3, 1, 0.1f, false),
            "frozen" pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class SpikeCopterState { FLY, DROP_TEETH, DROP }

    override lateinit var facing: Facing

    override var frozen: Boolean
        get() = freezeHandler.isFrozen()
        set(value) {
            freezeHandler.setFrozen(value)
        }

    private val freezeHandler = FreezableEntityHandler(
        this,
        onFrozen = { dropTeethTimer.setToEnd() }
    )

    private lateinit var state: SpikeCopterState

    private val dropTeethDelay = Timer(DROP_TEETH_DELAY)
    private val dropTeethTimer = Timer(DROP_TEETH_DUR)
    private val dropDelay = Timer(DROP_DELAY)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            val keys = gdxArrayOf("frozen")
            SpikeCopterState.entries.forEach { keys.add(it.name.lowercase()) }
            keys.forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun canSpawn(spawnProps: Properties) = super.canSpawn(spawnProps) &&
        MegaGameEntities.getOfTag(TAG).size < MAX_SPAWNED

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        state = SpikeCopterState.FLY

        FacingUtils.setFacingOf(this)

        dropTeethDelay.reset()
        dropTeethTimer.reset()
        dropDelay.reset()

        frozen = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        frozen = false
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            freezeHandler.update(delta)

            if (frozen) {
                body.physics.velocity.setZero()
                return@add
            }

            when (state) {
                SpikeCopterState.FLY -> {
                    FacingUtils.setFacingOf(this)

                    body.physics.velocity.x = VEL_X * ConstVals.PPM * facing.value

                    if (shouldDrop()) {
                        setToDrop()
                        return@add
                    }

                    dropTeethDelay.update(delta)
                    if (dropTeethDelay.isFinished()) {
                        state = SpikeCopterState.DROP_TEETH
                        dropTeethDelay.reset()
                        dropTeeth()
                    }
                }
                SpikeCopterState.DROP_TEETH -> {
                    body.physics.velocity.setZero()

                    dropTeethTimer.update(delta)
                    if (dropTeethTimer.isFinished()) {
                        state = SpikeCopterState.FLY
                        dropTeethTimer.reset()
                    }
                }
                SpikeCopterState.DROP -> {
                    dropDelay.update(delta)
                    if (dropDelay.isJustFinished()) body.physics.velocity.set(0f, DROP_Y * ConstVals.PPM)
                }
            }
        }
    }

    private fun setToDrop() {
        body.physics.velocity.setZero()
        state = SpikeCopterState.DROP
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.setHitByBlockReceiver(ProcessState.BEGIN) { _, _ -> setToDrop() }
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftFixture }

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.5f * ConstVals.PPM))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.setHitByBlockReceiver(ProcessState.BEGIN) { _, _ -> setToDrop() }
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightFixture }

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setHeight(0.1f * ConstVals.PPM))
        feetFixture.bodyAttachmentPosition = Position.BOTTOM_CENTER
        feetFixture.setHitByBlockReceiver(ProcessState.BEGIN) { _, _ -> explodeAndDie() }
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val feetWidth = if (state == SpikeCopterState.DROP) body.getWidth() else 0.5f * ConstVals.PPM
            (feetFixture.rawShape as GameRectangle).setWidth(feetWidth)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGEABLE, FixtureType.DAMAGER)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.setCenter(body.getCenter())
            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { if (frozen) "frozen" else state.name.lowercase() }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val def = entry.value
                        animations.put(key, Animation(regions[key], def.rows, def.cols, def.durations, def.loop))
                    }
                }
                .build()
        )
        .build()

    private fun dropTeeth() {
        val spawn = body.getPositionPoint(Position.BOTTOM_CENTER)

        val impulse = GameObjectPools.fetch(Vector2::class).set(0f, DROP_TEETH_IMPULSE_Y).scl(ConstVals.PPM.toFloat())

        val teeth = MegaEntityFactory.fetch(SpikeTeeth::class)!!
        teeth.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.IMPULSE pairTo impulse
            )
        )

        requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
    }

    private fun shouldDrop(): Boolean {
        val bodyCenter = body.getCenter()
        val megamanCenter = megaman.body.getCenter()
        return megamanCenter.y <= bodyCenter.y && bodyCenter.x.epsilonEquals(megamanCenter.x, 0.25f * ConstVals.PPM)
    }

    private fun explodeAndDie() {
        explode()
        destroy()
    }
}
