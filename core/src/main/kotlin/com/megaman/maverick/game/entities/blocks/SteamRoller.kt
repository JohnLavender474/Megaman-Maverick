package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.*
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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.explosions.AsteroidExplosion
import com.megaman.maverick.game.entities.projectiles.Rock
import com.megaman.maverick.game.entities.projectiles.Rock.RockSize
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class SteamRoller(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IDrawableShapesEntity,
    ICullableEntity, IAudioEntity, IFaceable {

    companion object {
        const val TAG = "SteamRoller"

        private const val ROLL_IMPULSE = 10f
        private const val MAX_ROLL_SPEED = 5f

        private const val SMASH_DELAY = 0.5f
        private const val SMASH_DUR = 0.3f

        private const val REVERSE_SPEED = 2f

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAV = -0.01f

        private const val SMASH_AREA_WIDTH = 2.25f
        private const val SMASH_AREA_HEIGHT = 1f
        private const val SMASH_AREA_OFFSET_X = 1f
        private const val SMASH_AREA_OFFSET_Y = 0.25f
        private const val SMASH_ROCKS = 5
        private const val SMASH_ROCKS_MIN_OFFSET_X = -0.35f
        private const val SMASH_ROCKS_MAX_OFFSET_X = 0.5f
        private const val SMASH_ROCKS_MIN_OFFSET_Y = -0.25f
        private const val SMASH_ROCKS_MAX_OFFSET_Y = -0.1f
        private const val SMASH_ROCKS_MIN_X_IMPULSE = -6f
        private const val SMASH_ROCKS_MAX_X_IMPULSE = 6f
        private const val SMASH_ROCKS_MIN_Y_IMPULSE = 2f
        private const val SMASH_ROCKS_MAX_Y_IMPULSE = 6f

        private val animDefs = orderedMapOf(
            "idle" pairTo AnimationDef(),
            "reverse" pairTo AnimationDef(2, 1, 0.1f, true),
            "roll" pairTo AnimationDef(2, 1, 0.1f, true),
            "smash" pairTo AnimationDef(3, 1, 0.1f, false)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class SteamRollerState { IDLE, ROLL, SMASH, REVERSE }

    override lateinit var facing: Facing

    private lateinit var state: SteamRollerState

    private val blocks = Array<Block>()
    private val blockOffsets = OrderedMap<Block, Vector2>()

    private val smashTimer = Timer(SMASH_DUR)
        .setRunOnJustFinished { smash() }
    private val smashDelay = Timer(SMASH_DELAY)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SPECIALS_1.source)
            AnimationUtils.loadRegions(TAG, atlas, animDefs.keys(), regions)
        }
        super.init()
        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        facing = spawnProps.get(ConstKeys.FACING, Facing::class)!!

        state = spawnProps.get(ConstKeys.STATE, SteamRollerState::class)!!

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setBottomCenterToPoint(spawn)

        smashDelay.reset()
        smashTimer.setToEnd()

        val blockBounds = spawnProps.get(ConstKeys.BLOCKS) as Array<GameRectangle>
        blockBounds.forEach { blockBound ->
            val block = MegaEntityFactory.fetch(Block::class)!!
            block.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.BOUNDS pairTo blockBound,
                    ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
                    "${ConstKeys.FEET}_${ConstKeys.SOUND}" pairTo false,
                    ConstKeys.BODY_LABELS pairTo objectSetOf(
                        BodyLabel.COLLIDE_DOWN_ONLY
                    ),
                    ConstKeys.FIXTURE_LABELS pairTo objectSetOf(
                        FixtureLabel.NO_SIDE_TOUCHIE,
                        FixtureLabel.NO_BODY_TOUCHIE,
                        FixtureLabel.NO_PROJECTILE_COLLISION
                    ),
                    ConstKeys.FIXTURES pairTo gdxArrayOf(
                        FixtureType.SHIELD pairTo props(ConstKeys.DIRECTION pairTo Direction.UP)
                    ),
                    ConstKeys.CUSTOM_PRE_PROCESS pairTo { blockBody: Body ->
                        val offset = blockOffsets.get(block)
                        val target = body.getCenter().add(offset)
                        blockBody.setCenter(target)
                    }
                )
            )

            blocks.add(block)

            val offset = blockBound.getCenter(false).sub(body.getCenter())
            blockOffsets.put(block, offset)
        }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        blockOffsets.values().forEach { GameObjectPools.free(it) }
        blockOffsets.clear()

        blocks.forEach { it.destroy() }
        blocks.clear()
    }

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo getGameCameraCullingLogic(this))
    )

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        val smashBlockActive = state != SteamRollerState.SMASH || smashTimer.isFinished()
        blocks[0].let {
            it.body.physics.collisionOn = smashBlockActive
            it.blockFixture.setActive(smashBlockActive)
        }

        when (state) {
            SteamRollerState.IDLE -> body.physics.velocity.x = 0f
            SteamRollerState.ROLL -> {
                roll(delta)
                if (FacingUtils.isFacingBlock(this)) {
                    GameLogger.debug(TAG, "ROLL -> IDLE")
                    state = SteamRollerState.IDLE
                }
            }
            SteamRollerState.SMASH -> {
                if (!smashTimer.isFinished()) {
                    smashTimer.update(delta)
                    if (smashTimer.isFinished()) smashDelay.reset()

                    body.physics.velocity.x = 0f
                } else {
                    smashDelay.update(delta)
                    if (smashDelay.isFinished()) smashTimer.reset()

                    roll(delta)

                    if (FacingUtils.isFacingBlock(this)) {
                        GameLogger.debug(TAG, "update(): SMASH -> IDLE")

                        state = SteamRollerState.IDLE

                        smashDelay.reset()
                        smashTimer.setToEnd()
                    }
                }
            }
            SteamRollerState.REVERSE -> {
                reverse()
                if (FacingUtils.isBackTouchingBlock(this)) {
                    GameLogger.debug(TAG, "update(): REVERSE -> IDLE")
                    state = SteamRollerState.IDLE
                }
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(4.75f * ConstVals.PPM, 4f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(4.5f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.25f * ConstVals.PPM, 1.5f * ConstVals.PPM))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyAttachment.set(-body.getWidth() / 2f, -0.5f * ConstVals.PPM)
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftFixture }

        val leftDamagerFixture = Fixture(body, FixtureType.DAMAGER)
        leftDamagerFixture.attachedToBody = false
        body.addFixture(leftDamagerFixture)

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.25f * ConstVals.PPM, 1.5f * ConstVals.PPM))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyAttachment.set(body.getWidth() / 2f, -0.5f * ConstVals.PPM)
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightFixture }

        val rightDamagerFixture = Fixture(body, FixtureType.DAMAGER)
        rightDamagerFixture.attachedToBody = false
        body.addFixture(rightDamagerFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAV else GRAVITY
            body.physics.gravity.y = gravity * ConstVals.PPM

            val canSideDoDamage = state == SteamRollerState.ROLL
            leftDamagerFixture.setActive(canSideDoDamage && isFacing(Facing.LEFT))
            rightDamagerFixture.setActive(canSideDoDamage && isFacing(Facing.RIGHT))
            leftDamagerFixture.setShape(leftFixture.getShape())
            rightDamagerFixture.setShape(rightFixture.getShape())
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(7f * ConstVals.PPM, 6f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.setFlip(isFacing(Facing.LEFT), false)
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.translateX(0.75f * ConstVals.PPM * -facing.value)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    when (state) {
                        SteamRollerState.SMASH -> if (!smashTimer.isFinished()) "smash" else "roll"
                        else -> state.name.lowercase()
                    }
                }
                .applyToAnimations { animations ->
                    AnimationUtils.loadAnimationDefs(animDefs, animations, regions)
                }
                .build()
        )
        .build()

    private fun roll(delta: Float) {
        if ((isFacing(Facing.LEFT) && body.physics.velocity.x > -MAX_ROLL_SPEED * ConstVals.PPM) ||
            (isFacing(Facing.RIGHT) && body.physics.velocity.x < MAX_ROLL_SPEED * ConstVals.PPM)
        ) body.physics.velocity.x += ROLL_IMPULSE * ConstVals.PPM * facing.value * delta
    }

    private fun reverse() {
        body.physics.velocity.x = -REVERSE_SPEED * ConstVals.PPM * facing.value
    }

    private fun smash() {
        GameLogger.debug(TAG, "smash()")

        val center = body.getPositionPoint(Position.BOTTOM_CENTER)
            .add(
                SMASH_AREA_OFFSET_X * ConstVals.PPM * facing.value,
                SMASH_AREA_OFFSET_Y * ConstVals.PPM
            )
            .add(
                SMASH_AREA_WIDTH * facing.value * ConstVals.PPM / 2f,
                SMASH_AREA_HEIGHT * ConstVals.PPM / 2f
            )

        (0 until SMASH_ROCKS).forEach {
            val spawn = GameObjectPools.fetch(Vector2::class)
                .set(center)
                .add(
                    UtilMethods.getRandom(
                        SMASH_ROCKS_MIN_OFFSET_X,
                        SMASH_ROCKS_MAX_OFFSET_X
                    ) * ConstVals.PPM * facing.value,
                    UtilMethods.getRandom(
                        SMASH_ROCKS_MIN_OFFSET_Y,
                        SMASH_ROCKS_MAX_OFFSET_Y
                    ) * ConstVals.PPM
                )

            val size = if (it % 2 == 0) RockSize.BIG else RockSize.SMALL

            val impulse = GameObjectPools.fetch(Vector2::class)
                .setX(
                    UtilMethods.getRandom(
                        SMASH_ROCKS_MIN_X_IMPULSE,
                        SMASH_ROCKS_MAX_X_IMPULSE
                    ) * ConstVals.PPM
                )
                .setY(
                    UtilMethods.getRandom(
                        SMASH_ROCKS_MIN_Y_IMPULSE,
                        SMASH_ROCKS_MAX_Y_IMPULSE
                    ) * ConstVals.PPM
                )

            val rock = MegaEntityFactory.fetch(Rock::class)!!
            rock.spawn(
                props(
                    ConstKeys.SIZE pairTo size,
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.POSITION pairTo spawn,
                    ConstKeys.IMPULSE pairTo impulse
                )
            )

            val explosion = MegaEntityFactory.fetch(AsteroidExplosion::class)!!
            explosion.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.SOUND pairTo false,
                    ConstKeys.POSITION pairTo spawn
                )
            )
        }

        requestToPlaySound(SoundAsset.ASTEROID_EXPLODE_SOUND, false)
    }

    override fun getType() = EntityType.OTHER

    override fun getTag() = TAG
}
