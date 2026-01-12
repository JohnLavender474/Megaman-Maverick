package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IBodyEntity
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
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.decorations.KoopaShellTrailSprite
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.components.feetFixture
import com.megaman.maverick.game.entities.megaman.components.leftSideFixture
import com.megaman.maverick.game.entities.megaman.components.rightSideFixture
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.utils.DrawableShapesComponentBuilder
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.world.body.*

class GreenKoopaShell(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

    companion object {
        const val TAG = "GreenKoopaShell"

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.001f

        private const val SPEED = 8f

        private const val KNOCKED_TO_DEATH_BUMP = 5f

        private val OTHER_SHELL_TAGS = objectSetOf(TAG, RedKoopaShell.TAG)

        private val animDefs = orderedMapOf(
            "shell" pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class GreenKoopaShellState {
        IDLE, ROLLING, KNOCKED_TO_DEATH
    }

    override var facing = Facing.LEFT

    var koopaId = -1
        private set

    private lateinit var state: GreenKoopaShellState
    private val tempOut = ObjectSet<MegaGameEntity>()
    private val trailSpriteTimer = Timer(ConstVals.STANDARD_TRAIL_SPRITE_DELAY)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SMB3_ENEMIES.source)
            AnimationUtils.loadRegions(TAG, atlas, animDefs.keys(), regions)
        }
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setBottomCenterToPoint(position)

        state = GreenKoopaShellState.IDLE
        trailSpriteTimer.reset()

        koopaId = spawnProps.getOrDefault("${ConstKeys.PARENT}_${ConstKeys.ID}", -1, Int::class)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        tempOut.clear()
        koopaId = -1
    }

    override fun onHealthDepleted() {
        GameLogger.debug(TAG, "onHealthDepleted()")
        super.onHealthDepleted()
        spawnFloatingPoints()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            tempOut.clear()

            if (state != GreenKoopaShellState.KNOCKED_TO_DEATH) {
                val otherShells = MegaGameEntities.getOfTags(tempOut, OTHER_SHELL_TAGS)
                for (otherShell in otherShells) {
                    if (otherShell == this) continue

                    otherShell as IBodyEntity
                    if (body.getBounds().overlaps(otherShell.body.getBounds())) {
                        getKnockedToDeath(otherShell.body.getBounds())
                        break
                    }
                }
            }

            when (state) {
                GreenKoopaShellState.IDLE -> {
                    if (shouldStartRolling()) {
                        startRolling()

                        facing = when {
                            megaman.body.getBounds().getX() < body.getBounds().getX() -> Facing.RIGHT
                            else -> Facing.LEFT
                        }
                    }
                }
                GreenKoopaShellState.ROLLING -> {
                    val speed = SPEED * ConstVals.PPM * facing.value
                    body.physics.velocity.x = speed

                    trailSpriteTimer.update(delta)
                    if (trailSpriteTimer.isFinished()) {
                        val trailSprite = MegaEntityFactory.fetch(KoopaShellTrailSprite::class)!!
                        trailSprite.spawn(
                            props(
                                ConstKeys.COLOR pairTo KoopaShellTrailSprite.ShellColor.GREEN,
                                ConstKeys.POSITION pairTo body.getPositionPoint(Position.BOTTOM_CENTER)
                            )
                        )

                        trailSpriteTimer.reset()
                    }
                }
                GreenKoopaShellState.KNOCKED_TO_DEATH -> body.physics.velocity.x = 0f
            }
        }
    }

    private fun shouldStartRolling() =
        megaman.leftSideFixture.getShape().overlaps(body.getBounds()) ||
            megaman.rightSideFixture.getShape().overlaps(body.getBounds()) ||
            megaman.feetFixture.getShape().overlaps(body.getBounds()) ||
            megaman.body.getBounds().overlaps(body.getBounds())

    private fun startRolling() {
        state = GreenKoopaShellState.ROLLING
        requestToPlaySound(SoundAsset.SMB3_KICK_SOUND, false)
        facing = if (megaman.body.getBounds().getX() < body.getBounds().getX()) Facing.RIGHT else Facing.LEFT
    }

    private fun getKnockedToDeath(shellBounds: GameRectangle) {
        state = GreenKoopaShellState.KNOCKED_TO_DEATH
        body.physics.velocity.y = KNOCKED_TO_DEATH_BUMP * ConstVals.PPM
        requestToPlaySound(SoundAsset.SMB3_KICK_SOUND, false)
        spawnWhackForOverlap(body.getBounds(), shellBounds)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.RED

        val drawableShapesComponent = DrawableShapesComponentBuilder().addDebug { body.getBounds() }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        headFixture.setHitByFeetReceiver(ProcessState.BEGIN) { feet, _ ->
            if (state == GreenKoopaShellState.KNOCKED_TO_DEATH) return@setHitByFeetReceiver

            val entity = feet.getEntity()
            if (entity is Megaman) {
                if (state == GreenKoopaShellState.IDLE) startRolling()

                spawnWhackForOverlap(headFixture.getShape(), feet.getShape())

                megaman.body.physics.velocity.y = MegamanValues.JUMP_VEL * ConstVals.PPM / 2f
                requestToPlaySound(SoundAsset.SWIM_SOUND, false)
            }
        }
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.ORANGE
        drawableShapesComponent.addDebug { headFixture }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        drawableShapesComponent.addDebug { feetFixture }

        val leftFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        drawableShapesComponent.addDebug { leftFixture }

        val rightFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.YELLOW
        drawableShapesComponent.addDebug { rightFixture }

        body.preProcess.put(ConstKeys.DEATH) {
            body.physics.collisionOn = state != GreenKoopaShellState.KNOCKED_TO_DEATH
            if (state == GreenKoopaShellState.KNOCKED_TO_DEATH) body.forEachFixture { it.setActive(false) }
        }

        body.preProcess.put(ConstKeys.DAMAGER) {
            val damager = body.fixtures.get(FixtureType.DAMAGER).first()
            damager.setActive(state == GreenKoopaShellState.ROLLING)
        }

        body.preProcess.put(ConstKeys.GRAVITY) {
            val gravity = when {
                !body.isSensing(BodySense.FEET_ON_GROUND) || state == GreenKoopaShellState.KNOCKED_TO_DEATH -> GRAVITY
                else -> GROUND_GRAVITY
            }
            body.physics.gravity.y = gravity * ConstVals.PPM
        }

        body.postProcess.put(ConstKeys.SCANNER) {
            if (state == GreenKoopaShellState.KNOCKED_TO_DEATH) return@put

            if (isFacing(Facing.LEFT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_LEFT)) {
                if (overlapsGameCamera()) requestToPlaySound(SoundAsset.SMB3_BUMP_SOUND, false)
                facing = Facing.RIGHT
            } else if (isFacing(Facing.RIGHT) && body.isSensing(BodySense.SIDE_TOUCHING_BLOCK_RIGHT)) {
                if (overlapsGameCamera()) requestToPlaySound(SoundAsset.SMB3_BUMP_SOUND, false)
                facing = Facing.LEFT
            }
        }

        addComponent(drawableShapesComponent.build())

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGEABLE, FixtureType.DAMAGER),
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { it.setSize(2f * ConstVals.PPM.toFloat()) })
        .preProcess { _, sprite ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.setFlip(isFacing(Facing.RIGHT), state == GreenKoopaShellState.KNOCKED_TO_DEATH)
            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { "shell" }
                .applyToAnimations { animations ->
                    AnimationUtils.loadAnimationDefs(animDefs, animations, regions)
                }
                .build()
        )
        .build()
}
