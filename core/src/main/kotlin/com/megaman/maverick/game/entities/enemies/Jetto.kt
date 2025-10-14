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
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.ICullable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
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
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.projectiles.DeathBomb
import com.megaman.maverick.game.entities.utils.delayNextPossibleSpawn
import com.megaman.maverick.game.entities.utils.isNextPossibleSpawnDelayed
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getRandomPositionInBounds
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class Jetto(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable, ICullable {

    companion object {
        const val TAG = "Jetto"

        private const val SPEED = 12f

        private const val CULL_TIME = 0.25f

        private const val BOMB_DROP_Y = -8f
        private const val DROP_BOMB_DELAY = 0.2f

        private const val FALL_EXPLODE_DELAY = 0.1f
        private const val FALL_BLINK_DELAY = 0.05f
        private const val FALL_GRAVITY = -0.375f

        private const val NEXT_POSSIBLE_SPAWN_DELAY = 1f

        private val animDefs = orderedMapOf(
            "fly" pairTo AnimationDef(3, 1, 0.1f, true),
            "fall" pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class JettoState { FLY, FALL }

    private enum class JettoColor { GRAY, BRONZE }

    override val invincible: Boolean
        get() = state == JettoState.FALL || super.invincible

    override lateinit var facing: Facing

    private lateinit var state: JettoState

    private val cullTimer = Timer(CULL_TIME)

    private val dropBombDelay = Timer(DROP_BOMB_DELAY)

    private val fallExplodeTimer = Timer(FALL_EXPLODE_DELAY)
    private val fallBlinkTimer = Timer(FALL_BLINK_DELAY)
    private var fallBlink = false

    private lateinit var color: JettoColor

    private var passes = 0

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            JettoColor.entries.map { it.name.lowercase() }.forEach { color ->
                animDefs.keys().forEach { key -> regions.put("$color/$key", atlas.findRegion("$TAG/$color/$key")) }
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun canSpawn(spawnProps: Properties): Boolean {
        if (!super.canSpawn(spawnProps)) return false
        if (!MegaGameEntities.getOfTag(TAG).isEmpty) return false

        val id = spawnProps.get(ConstKeys.ID, Int::class)!!
        val canSpawn = !isNextPossibleSpawnDelayed(game, TAG, id)
        GameLogger.debug(TAG, "canSpawn(): canSpawn=$canSpawn")

        return canSpawn
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val center = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(center)

        FacingUtils.setFacingOf(this)

        state = JettoState.FLY

        color = JettoColor.valueOf(
            spawnProps.getOrDefault(ConstKeys.COLOR, JettoColor.BRONZE.name, String::class).uppercase()
        )

        dropBombDelay.setToEnd()

        cullTimer.reset()
        putCullable(ConstKeys.CUSTOM_CULL, this)

        fallExplodeTimer.setToEnd()
        fallBlinkTimer.reset()
        fallBlink = false

        passes = 1

        requestToPlaySound(SoundAsset.JET_SOUND, false)
    }

    override fun onHealthDepleted() {
        GameLogger.debug(TAG, "onHealthDepleted(): do not destroy; set state to FALL")
        state = JettoState.FALL
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        if (overlapsGameCamera() && isHealthDepleted()) explode()

        delayNextPossibleSpawn(game, TAG, id, NEXT_POSSIBLE_SPAWN_DELAY)
    }

    override fun shouldBeCulled(delta: Float) = cullTimer.isFinished() &&
        (state == JettoState.FALL || passes >= 2)

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            body.physics.velocity.x = SPEED * ConstVals.PPM * facing.value

            if (!body.getBounds().overlaps(game.getGameCamera().getRotatedBounds())) cullTimer.update(delta)
            else cullTimer.reset()

            if (state == JettoState.FLY) {
                if (passes == 1) {
                    if (cullTimer.isFinished()) {
                        val centerX: Float
                        val facing: Facing
                        if (body.getBounds().getMaxX() < game.getGameCamera().getRotatedBounds().getX()) {
                            centerX = game.getGameCamera().getRotatedBounds().getX()
                            facing = Facing.RIGHT
                        } else {
                            centerX = game.getGameCamera().getRotatedBounds().getMaxX()
                            facing = Facing.LEFT
                        }
                        body.setCenterX(centerX)
                        this.facing = facing

                        passes++

                        cullTimer.reset()

                        requestToPlaySound(SoundAsset.JET_SOUND, false)
                    }
                } else {
                    dropBombDelay.update(delta)
                    if (dropBombDelay.isFinished()) {
                        dropBomb()
                        dropBombDelay.reset()
                    }
                }
            } else {
                body.physics.velocity.x = 0f

                fallBlinkTimer.update(delta)
                if (fallBlinkTimer.isFinished()) {
                    fallBlink = !fallBlink
                    fallBlinkTimer.reset()
                }

                fallExplodeTimer.update(delta)
                if (fallExplodeTimer.isFinished()) {
                    val explosionProps = props(
                        ConstKeys.OWNER pairTo this,
                        ConstKeys.POSITION pairTo body.getBounds().getRandomPositionInBounds()
                    )
                    explode(explosionProps)

                    fallExplodeTimer.reset()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(3f * ConstVals.PPM, ConstVals.PPM.toFloat())
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val hitFunction: () -> Unit = { state = JettoState.FALL }

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.75f * ConstVals.PPM))
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        leftFixture.setHitByBodyReceiver { entity, processState ->
            if (processState.equalsAny(
                    ProcessState.BEGIN,
                    ProcessState.CONTINUE
                ) && entity is Jetto
            ) hitFunction.invoke()
        }
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftFixture }

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.75f * ConstVals.PPM))
        rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        rightFixture.setHitByBodyReceiver { entity, processState ->
            if (processState.equalsAny(
                    ProcessState.BEGIN,
                    ProcessState.CONTINUE
                ) && entity is Jetto
            ) hitFunction.invoke()
        }
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightFixture }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(2.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity.y = if (state == JettoState.FALL) FALL_GRAVITY * ConstVals.PPM else 0f
            if (FacingUtils.isFacingBlock(this)) state = JettoState.FALL
            if (body.isSensing(BodySense.FEET_ON_GROUND)) {
                explode()
                destroy()
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(4f * ConstVals.PPM, 2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            val position = if (isFacing(Facing.LEFT)) Position.CENTER_LEFT else Position.CENTER_RIGHT
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.translateX(0.5f * ConstVals.PPM * facing.value)
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.hidden = damageBlink || (state == JettoState.FALL && fallBlink)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { "${color.name.lowercase()}/${state.name.lowercase()}" }
                .applyToAnimations { animations ->
                    JettoColor.entries.map { it.name.lowercase() }.forEach { color ->
                        animDefs.forEach { entry ->
                            val key = entry.key
                            val (rows, columns, durations, loop) = entry.value
                            animations.put(
                                "$color/$key",
                                Animation(regions["$color/$key"], rows, columns, durations, loop)
                            )
                        }
                    }
                }
                .build()
        )
        .build()

    private fun dropBomb() {
        GameLogger.debug(TAG, "dropBomb()")

        val position = body.getCenter()
            .add(0.75f * ConstVals.PPM * facing.value, -0.75f * ConstVals.PPM)

        val impulse = GameObjectPools.fetch(Vector2::class)
            .set(0f, BOMB_DROP_Y)
            .scl(ConstVals.PPM.toFloat())

        val bomb = MegaEntityFactory.fetch(DeathBomb::class)!!
        bomb.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.IMPULSE pairTo impulse,
                ConstKeys.POSITION pairTo position
            )
        )
    }
}
