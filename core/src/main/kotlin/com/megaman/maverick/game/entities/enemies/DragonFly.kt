package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.collision.BoundingBox
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.overlaps
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IFreezableEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.utils.FreezableEntityHandler
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getCenter

class DragonFly(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IFreezableEntity, IFaceable,
    IDirectional {

    enum class DragonFlyBehavior { MOVE_UP, MOVE_DOWN, MOVE_HORIZONTAL }

    companion object {
        const val TAG = "DragonFly"
        private var textureRegion: TextureRegion? = null
        private var frozenRegion: TextureRegion? = null
        private const val CULL_TIME = 2f
        private const val VERT_SPEED = 18f
        private const val HORIZ_SPEED = 14f
        private const val CHANGE_BEHAV_DUR = 0.35f
        private const val VERT_SCANNER_OFFSET = 2f
        private const val HORIZ_SCANNER_OFFSET = 3f
    }

    override lateinit var direction: Direction
    override lateinit var facing: Facing

    override var frozen: Boolean
        get() = freezeHandler.isFrozen()
        set(value) {
            freezeHandler.setFrozen(value)
        }

    private val freezeHandler = FreezableEntityHandler(this)

    private val behaviorTimer = Timer(CHANGE_BEHAV_DUR)
    private lateinit var currentBehavior: DragonFlyBehavior
    private lateinit var previousBehavior: DragonFlyBehavior

    private var toLeftBounds = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        if (textureRegion == null) {
            textureRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_1.source, "DragonFly")
            frozenRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_1.source, "$TAG/frozen")
        }
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_TIME, CULL_TIME)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        when {
            spawnProps.containsKey(ConstKeys.DIRECTION) -> {
                var direction = spawnProps.get(ConstKeys.DIRECTION)
                if (direction is String) direction = Direction.valueOf(direction.uppercase())
                this.direction = direction as Direction
            }
            else -> direction = Direction.UP
        }

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        currentBehavior = DragonFlyBehavior.MOVE_UP
        previousBehavior = DragonFlyBehavior.MOVE_UP

        behaviorTimer.reset()

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

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
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.85f * ConstVals.PPM)

        val damageableFixture =
            Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(ConstVals.PPM.toFloat()))
        body.addFixture(damageableFixture)

        val damagerFixture =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.75f * ConstVals.PPM.toFloat()))
        body.addFixture(damagerFixture)

        val megamanScannerFixture = Fixture(
            body,
            FixtureType.CUSTOM,
            GameRectangle().setSize(32f * ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())
        )
        body.addFixture(megamanScannerFixture)

        val oobScannerFixture = Fixture(body, FixtureType.CUSTOM, GameRectangle().setSize(ConstVals.PPM / 2f))
        body.addFixture(oobScannerFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            behaviorTimer.update(ConstVals.FIXED_TIME_STEP)

            if (!behaviorTimer.isFinished() || frozen) {
                body.physics.velocity.setZero()
                return@put
            }

            when (currentBehavior) {
                DragonFlyBehavior.MOVE_UP -> {
                    when (direction) {
                        Direction.UP -> {
                            body.physics.velocity.set(0f, VERT_SPEED * ConstVals.PPM)
                            oobScannerFixture.offsetFromBodyAttachment.set(0f, VERT_SCANNER_OFFSET * ConstVals.PPM)
                        }
                        Direction.DOWN -> {
                            body.physics.velocity.set(0f, -VERT_SPEED * ConstVals.PPM)
                            oobScannerFixture.offsetFromBodyAttachment.set(0f, -VERT_SCANNER_OFFSET * ConstVals.PPM)
                        }
                        Direction.LEFT -> {
                            body.physics.velocity.set(-VERT_SPEED * ConstVals.PPM, 0f)
                            oobScannerFixture.offsetFromBodyAttachment.set(-VERT_SCANNER_OFFSET * ConstVals.PPM, 0f)
                        }
                        Direction.RIGHT -> {
                            body.physics.velocity.set(VERT_SPEED * ConstVals.PPM, 0f)
                            oobScannerFixture.offsetFromBodyAttachment.set(VERT_SCANNER_OFFSET * ConstVals.PPM, 0f)
                        }
                    }
                }
                DragonFlyBehavior.MOVE_DOWN -> {
                    body.physics.velocity.set(0f, -VERT_SPEED * ConstVals.PPM)
                    oobScannerFixture.offsetFromBodyAttachment.set(0f, -VERT_SCANNER_OFFSET * ConstVals.PPM)
                }
                DragonFlyBehavior.MOVE_HORIZONTAL -> {
                    var xVel = HORIZ_SPEED * ConstVals.PPM
                    if (toLeftBounds) xVel *= -1f
                    body.physics.velocity.set(xVel, 0f)

                    var xOffset: Float = HORIZ_SCANNER_OFFSET * ConstVals.PPM
                    if (toLeftBounds) xOffset *= -1f
                    oobScannerFixture.offsetFromBodyAttachment.set(xOffset, 0f)
                }
            }
        }

        body.postProcess.put(ConstKeys.DEFAULT) {
            if (!behaviorTimer.isFinished() || frozen) {
                body.physics.velocity.setZero()
                return@put
            }

            when (currentBehavior) {
                DragonFlyBehavior.MOVE_UP -> {
                    if (!game.getGameCamera().overlaps(
                            oobScannerFixture.getShape().getBoundingRectangle(),
                            GameObjectPools.fetch(BoundingBox::class)
                        )
                    ) {
                        changeBehavior(DragonFlyBehavior.MOVE_HORIZONTAL)
                        toLeftBounds = isMegamanLeft()
                    }
                }
                DragonFlyBehavior.MOVE_DOWN -> {
                    if (megamanScannerFixture.getShape().contains(megaman.body.getCenter()) ||
                        (!isMegamanBelow() && !game.getGameCamera().overlaps(
                            oobScannerFixture.getShape().getBoundingRectangle(),
                            GameObjectPools.fetch(BoundingBox::class)
                        ))
                    ) {
                        changeBehavior(DragonFlyBehavior.MOVE_HORIZONTAL)
                        toLeftBounds = isMegamanLeft()
                    }
                }
                DragonFlyBehavior.MOVE_HORIZONTAL -> {
                    val doChange = (toLeftBounds && !isMegamanLeft()) || (!toLeftBounds && isMegamanLeft())
                    if (doChange && !game.getGameCamera().overlaps(
                            oobScannerFixture.getShape().getBoundingRectangle(),
                            GameObjectPools.fetch(BoundingBox::class)
                        )
                    ) {
                        changeBehavior(
                            if (previousBehavior == DragonFlyBehavior.MOVE_UP) DragonFlyBehavior.MOVE_DOWN
                            else DragonFlyBehavior.MOVE_UP
                        )
                    }
                }
            }
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putPreProcess { _, _ ->
            sprite.hidden = damageBlink

            sprite.setOriginCenter()
            sprite.rotation = direction.rotation

            sprite.setPosition(body.getCenter(), Position.CENTER)

            if (currentBehavior.equalsAny(DragonFlyBehavior.MOVE_UP, DragonFlyBehavior.MOVE_DOWN))
                facing = if (isMegamanLeft()) Facing.LEFT else Facing.RIGHT

            sprite.setFlip(facing == Facing.LEFT, false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { if (frozen) "frozen" else "fly" }
        val animations = objectMapOf<String, IAnimation>(
            "fly" pairTo Animation(textureRegion!!, 1, 2, 0.1f, true),
            "frozen" pairTo Animation(frozenRegion!!)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun changeBehavior(behavior: DragonFlyBehavior) {
        previousBehavior = currentBehavior
        currentBehavior = behavior
        behaviorTimer.reset()
    }

    private fun isMegamanLeft() = game.megaman.body.getCenter().x < body.getX()

    private fun isMegamanBelow() = game.megaman.body.getCenter().y < body.getY()
}
