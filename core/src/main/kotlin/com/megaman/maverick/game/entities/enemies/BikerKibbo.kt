package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
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
import com.mega.game.engine.drawables.sprites.setPosition
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
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.decorations.DustPuff
import com.megaman.maverick.game.entities.utils.delayNextPossibleSpawn
import com.megaman.maverick.game.entities.utils.isNextPossibleSpawnDelayed
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

class BikerKibbo(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "BikerKibbo"

        private const val GRAVITY = -0.375f
        private const val GROUND_GRAVITY = -0.001f

        private const val X_VEL = 8f
        private const val X_IMPULSE = 10f

        private const val JUMP_IMPULSE = 20f

        private const val ON_SPAWN_DELAY = 0.5f

        private const val DUST_PUFF_DELAY = 0.1f
        private val DUST_PUFF_OFFSETS = gdxArrayOf(-0.75f, 1.25f)

        private val animDefs = orderedMapOf(
            "jump" pairTo AnimationDef(2, 1, 0.1f, true),
            "ride" pairTo AnimationDef(2, 1, 0.1f, true)
        )
        private val regions = ObjectMap<String, TextureRegion>()

        private const val NEXT_POSSIBLE_SPAWN_DELAY = 1f
    }

    override lateinit var facing: Facing

    private val dustPuffDelay = Timer(DUST_PUFF_DELAY)
    private var dustPuffIndex = 0

    private val onSpawnDelay = Timer(ON_SPAWN_DELAY)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun canSpawn(spawnProps: Properties): Boolean {
        if (!super.canSpawn(spawnProps)) return false

        val id = spawnProps.get(ConstKeys.ID, Int::class)!!

        val canSpawn = !isNextPossibleSpawnDelayed(game, TAG, id)
        GameLogger.debug(TAG, "canSpawn(): canSpawn=$canSpawn")

        return canSpawn
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

        dustPuffDelay.reset()
        dustPuffIndex = 0

        onSpawnDelay.reset()

        requestToPlaySound(SoundAsset.REV_SOUND, false)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        if (overlapsGameCamera() && isHealthDepleted()) explode()

        delayNextPossibleSpawn(game, TAG, id, NEXT_POSSIBLE_SPAWN_DELAY)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            onSpawnDelay.update(delta)
            when {
                !onSpawnDelay.isFinished() -> body.physics.velocity.x = 0f
                body.isSensing(BodySense.FEET_ON_GROUND) && abs(body.physics.velocity.x) < X_VEL * ConstVals.PPM ->
                    body.physics.velocity.x += X_IMPULSE * ConstVals.PPM * facing.value * Gdx.graphics.deltaTime
            }

            if (body.isSensing(BodySense.FEET_ON_GROUND) &&
                megaman.body.getMaxX() >= body.getX() &&
                megaman.body.getX() <= body.getMaxX() &&
                megaman.body.getY() > body.getY()
            ) jump()

            dustPuffDelay.update(delta)
            if (dustPuffDelay.isFinished() && body.isSensing(BodySense.FEET_ON_GROUND)) {
                spawnDustPuff(dustPuffIndex)
                dustPuffIndex++
                dustPuffDelay.reset()
            }
        }
    }

    override fun swapFacing() {
        GameLogger.debug(TAG, "swapFacing()")
        super.swapFacing()
        body.physics.velocity.x = 0f
        requestToPlaySound(SoundAsset.REV_SOUND, false)
    }

    private fun jump() {
        GameLogger.debug(TAG, "jump()")
        body.physics.velocity.y = JUMP_IMPULSE * ConstVals.PPM
        requestToPlaySound(SoundAsset.REV_SOUND, false)
    }

    private fun spawnDustPuff(index: Int) {
        GameLogger.debug(TAG, "spawnDustPuff(): index=$index")

        val offset = DUST_PUFF_OFFSETS[index % DUST_PUFF_OFFSETS.size]

        val position = body.getPositionPoint(Position.BOTTOM_CENTER)
            .add(facing.value * offset * ConstVals.PPM, 0f)

        val puff = MegaEntityFactory.fetch(DustPuff::class)!!
        puff.spawn(props(ConstKeys.OWNER pairTo this, ConstKeys.POSITION pairTo position))
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(2f * ConstVals.PPM, 1.5f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val leftFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        leftFixture.setHitByBlockReceiver(ProcessState.BEGIN) { _, _ -> if (isFacing(Facing.LEFT)) swapFacing() }
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftFixture }

        val rightFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        rightFixture.setHitByBlockReceiver(ProcessState.BEGIN) { _, _ -> if (isFacing(Facing.RIGHT)) swapFacing() }
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightFixture }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        feetFixture.setHitByBlockReceiver(ProcessState.END) { _, _ -> jump() }
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.physics.gravity.y = ConstVals.PPM * when {
                body.isSensing(BodySense.FEET_ON_GROUND) -> GROUND_GRAVITY
                else -> GRAVITY
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
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(4f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            sprite.hidden = damageBlink

            sprite.setFlip(isFacing(Facing.RIGHT), false)

            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { if (body.isSensing(BodySense.FEET_ON_GROUND)) "ride" else "jump" }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val (rows, columns, durations, loop) = entry.value
                        animations.put(key, Animation(regions[key], rows, columns, durations, loop))
                    }
                }
                .build()
        )
        .build()

    override fun getTag() = TAG
}
