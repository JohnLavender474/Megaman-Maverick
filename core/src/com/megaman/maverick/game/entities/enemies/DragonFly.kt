package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.overlaps
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class DragonFly(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

    enum class DragonFlyBehavior {
        MOVE_UP,
        MOVE_DOWN,
        MOVE_HORIZONTAL
    }

    companion object {
        private var textureRegion: TextureRegion? = null
        private const val CULL_TIME = 2f
        private const val VERT_SPEED = 18f
        private const val HORIZ_SPEED = 14f
        private const val CHANGE_BEHAV_DUR = .35f
        private const val VERT_SCANNER_OFFSET = 2f
        private const val HORIZ_SCANNER_OFFSET = 3f
    }

    override val damageNegotiations =
        objectMapOf<KClass<out IDamager>, DamageNegotiation>(
            Bullet::class to dmgNeg(10),
            Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
            ChargedShot::class to dmgNeg(ConstVals.MAX_HEALTH),
            ChargedShotExplosion::class to dmgNeg(ConstVals.MAX_HEALTH)
        )

    override var facing = Facing.RIGHT

    private var toLeftBounds = false
    private val behaviorTimer = Timer(CHANGE_BEHAV_DUR)
    private lateinit var currentBehavior: DragonFlyBehavior
    private lateinit var previousBehavior: DragonFlyBehavior

    override fun init() {
        super.init()
        if (textureRegion == null)
            textureRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_1.source, "DragonFly")
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_TIME, CULL_TIME)
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)
        currentBehavior = DragonFlyBehavior.MOVE_UP
        previousBehavior = DragonFlyBehavior.MOVE_UP
        behaviorTimer.reset()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.75f * ConstVals.PPM)

        // damageable fixture
        val damageableFixture =
            Fixture(GameRectangle().setSize(ConstVals.PPM.toFloat()), FixtureType.DAMAGEABLE)
        body.addFixture(damageableFixture)

        // damager fixture
        val damagerFixture =
            Fixture(GameRectangle().setSize(0.75f * ConstVals.PPM.toFloat()), FixtureType.DAMAGER)
        body.addFixture(damagerFixture)

        // megaman scanner fixture
        val megamanScannerFixture =
            Fixture(
                GameRectangle().setSize(32f * ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat()),
                FixtureType.CUSTOM
            )
        body.addFixture(megamanScannerFixture)

        // out-of-bounds scanner fixture
        val oobScannerFixture = Fixture(GameRectangle().setSize(ConstVals.PPM / 2f), FixtureType.CUSTOM)
        body.addFixture(oobScannerFixture)

        // pre-process
        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            behaviorTimer.update(it)

            if (!behaviorTimer.isFinished()) {
                body.physics.velocity.setZero()
                return@Updatable
            }

            when (currentBehavior) {
                DragonFlyBehavior.MOVE_UP -> {
                    body.physics.velocity.set(0f, VERT_SPEED * ConstVals.PPM)
                    oobScannerFixture.offsetFromBodyCenter.set(0f, VERT_SCANNER_OFFSET * ConstVals.PPM)
                }

                DragonFlyBehavior.MOVE_DOWN -> {
                    body.physics.velocity.set(0f, -VERT_SPEED * ConstVals.PPM)
                    oobScannerFixture.offsetFromBodyCenter.set(0f, -VERT_SCANNER_OFFSET * ConstVals.PPM)
                }

                DragonFlyBehavior.MOVE_HORIZONTAL -> {
                    var xVel = HORIZ_SPEED * ConstVals.PPM
                    if (toLeftBounds) xVel *= -1f
                    body.physics.velocity.set(xVel, 0f)
                    var xOffset: Float = HORIZ_SCANNER_OFFSET * ConstVals.PPM
                    if (toLeftBounds) xOffset *= -1f
                    oobScannerFixture.offsetFromBodyCenter.set(xOffset, 0f)
                }
            }
        })

        // post-process
        body.postProcess.put(ConstKeys.DEFAULT, Updatable {
            if (!behaviorTimer.isFinished()) {
                body.physics.velocity.setZero()
                return@Updatable
            }

            when (currentBehavior) {
                DragonFlyBehavior.MOVE_UP -> {
                    if (!getMegamanMaverickGame()
                            .getGameCamera()
                            .overlaps(oobScannerFixture.shape as Rectangle)
                    ) {
                        changeBehavior(DragonFlyBehavior.MOVE_HORIZONTAL)
                        toLeftBounds = isMegamanLeft()
                    }
                }

                DragonFlyBehavior.MOVE_DOWN -> {
                    if (megamanScannerFixture.shape.contains(
                            getMegamanMaverickGame().megaman.body.getCenter()
                        ) ||
                        (!isMegamanBelow() &&
                                !getMegamanMaverickGame()
                                    .getGameCamera()
                                    .overlaps(oobScannerFixture.shape as Rectangle))
                    ) {
                        changeBehavior(DragonFlyBehavior.MOVE_HORIZONTAL)
                        toLeftBounds = isMegamanLeft()
                    }
                }

                DragonFlyBehavior.MOVE_HORIZONTAL -> {
                    val doChange = (toLeftBounds && !isMegamanLeft()) || (!toLeftBounds && isMegamanLeft())
                    if (doChange &&
                        !getMegamanMaverickGame()
                            .getGameCamera()
                            .overlaps(oobScannerFixture.shape as Rectangle)
                    ) {
                        changeBehavior(
                            if (previousBehavior == DragonFlyBehavior.MOVE_UP) DragonFlyBehavior.MOVE_DOWN
                            else DragonFlyBehavior.MOVE_UP
                        )
                    }
                }
            }
        })

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.5f * ConstVals.PPM)

        val SpritesComponent = SpritesComponent(this, "dragonfly" to sprite)
        SpritesComponent.putUpdateFunction("dragonfly") { _, _sprite ->
            _sprite as GameSprite
            _sprite.setPosition(body.getCenter(), Position.CENTER)
            if (currentBehavior == DragonFlyBehavior.MOVE_UP ||
                currentBehavior == DragonFlyBehavior.MOVE_DOWN
            ) {
                facing = if (isMegamanLeft()) Facing.LEFT else Facing.RIGHT
            }
            _sprite.setFlip(facing == Facing.LEFT, false)
            // TODO: _sprite.hidden = damageBlink
        }

        return SpritesComponent
    }

    fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(textureRegion!!, 1, 2, 0.1f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    private fun changeBehavior(behavior: DragonFlyBehavior) {
        previousBehavior = currentBehavior
        currentBehavior = behavior
        behaviorTimer.reset()
    }

    private fun isMegamanLeft() = getMegamanMaverickGame().megaman.body.getCenter().x < body.x

    private fun isMegamanBelow() = getMegamanMaverickGame().megaman.body.getCenter().y < body.y
}
