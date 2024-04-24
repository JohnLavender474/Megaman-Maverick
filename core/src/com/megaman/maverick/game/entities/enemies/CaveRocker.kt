package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.GameLogger
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.Updatable
import com.engine.common.interfaces.isFacing
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.updatables.UpdatablesComponent
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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.CaveRock
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class CaveRocker(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "CaveRocker"
        private var standingRegion: TextureRegion? = null
        private var throwingRegion: TextureRegion? = null
        private const val WAIT_DURATION = 1f
        private const val ROCK_IMPULSE_X = 12f
        private const val ROCK_IMPULSE_Y = 10f
    }

    override var facing = Facing.RIGHT

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(5), Fireball::class to dmgNeg(10), ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 15 else 10
        }, ChargedShotExplosion::class to dmgNeg(5)
    )

    private val waitTimer = Timer(WAIT_DURATION)

    private var throwing = false
    private var newRock: CaveRock? = null
    private var newRockOffsetY = 0f

    override fun init() {
        super<AbstractEnemy>.init()
        if (standingRegion == null || throwingRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            standingRegion = atlas.findRegion("CaveRocker/Stand")
            throwingRegion = atlas.findRegion("CaveRocker/Throw")
        }
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        newRockOffsetY = spawnProps.get(ConstKeys.OFFSET_Y, Float::class)!!
        waitTimer.reset()
        throwing = false
    }

    override fun onDestroy() {
        super<AbstractEnemy>.onDestroy()
        if (newRock != null) {
            newRock!!.kill(props(CAUSE_OF_DEATH_MESSAGE to "Parent entity CaveRocker is destroyed"))
            newRock = null
        }
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            facing = if (megaman.body.x >= body.x) Facing.RIGHT else Facing.LEFT
            waitTimer.update(it)
            if (waitTimer.isJustFinished()) {
                throwRock()
                spawnNewRock()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(
            body, FixtureType.BODY, GameRectangle().setSize(1.25f * ConstVals.PPM, 1.5f * ConstVals.PPM)
        )
        body.addFixture(bodyFixture)
        bodyFixture.rawShape.color = Color.YELLOW
        debugShapes.add { bodyFixture.getShape() }

        val damagerFixture = Fixture(
            body, FixtureType.DAMAGER, GameRectangle().setSize(1.25f * ConstVals.PPM, 1.5f * ConstVals.PPM)
        )
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(1.15f * ConstVals.PPM))
        body.addFixture(damageableFixture)
        damageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        val headFixture = Fixture(
            body, FixtureType.HEAD, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM)
        )
        headFixture.offsetFromBodyCenter.y = 0.55f * ConstVals.PPM
        body.addFixture(headFixture)
        headFixture.getShape().color = Color.BLUE
        debugShapes.add { headFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            newRock?.let { _newRock ->
                if (throwing && _newRock.dead) {
                    GameLogger.debug(TAG, "New rock died before reaching cave rocker, so spawning a new one")
                    spawnNewRock()
                } else if (_newRock.body.overlaps(headFixture.getShape()) ||
                    _newRock.body.y < headFixture.getShape().getY()
                ) {
                    GameLogger.debug(
                        TAG,
                        "New rock landed on cave rocker's head. Setting [throwing] to false and resetting wait timer"
                    )
                    _newRock.kill(props(CAUSE_OF_DEATH_MESSAGE to "Landed on CaveRocker's head"))
                    throwing = false
                    newRock = null
                    waitTimer.reset()
                }
            }
        })

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            _sprite.setFlip(isFacing(Facing.LEFT), false)
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (throwing) "throw" else "stand" }
        val animations = objectMapOf<String, IAnimation>(
            "stand" to Animation(standingRegion!!, 1, 2, gdxArrayOf(0.5f, 0.15f), true),
            "throw" to Animation(throwingRegion!!, 1, 3, 0.05f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun throwRock() {
        throwing = true
        val caveRockToThrow = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.CAVE_ROCK)!!
        game.engine.spawn(
            caveRockToThrow, props(
                ConstKeys.OWNER to this, ConstKeys.POSITION to body.getTopCenterPoint(), ConstKeys.IMPULSE to Vector2(
                    ROCK_IMPULSE_X * ConstVals.PPM * facing.value, ROCK_IMPULSE_Y * ConstVals.PPM
                )
            )
        )
    }

    private fun spawnNewRock() {
        val newRockSpawn = body.getCenter().add(0f, newRockOffsetY * ConstVals.PPM)
        val newRockTrajectory = Vector2(0f, -newRockOffsetY * ConstVals.PPM)
        newRock = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.CAVE_ROCK) as CaveRock
        game.engine.spawn(
            newRock!!, props(
                ConstKeys.OWNER to this,
                ConstKeys.POSITION to newRockSpawn,
                ConstKeys.TRAJECTORY to newRockTrajectory,
                ConstKeys.GRAVITY to 0f,
                ConstKeys.PASS_THROUGH to true
            )
        )
    }
}
