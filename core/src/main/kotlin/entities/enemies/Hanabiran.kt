package com.megaman.maverick.game.entities.enemies


import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
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
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import kotlin.reflect.KClass

class Hanabiran(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IDirectionRotatable {

    enum class HanabiranState {
        SLEEPING,
        RISING,
        DROPPING,
        PETAL_4,
        PETAL_3,
        PETAL_2,
        PETAL_1,
        PETAL_0,
    }

    companion object {
        const val TAG = "Hanabiran"
        private var atlas: TextureAtlas? = null
        private const val SLEEP_DURATION = 0.5f
        private const val RISE_DROP_DURATION = 0.45f
        private const val PETAL_DURATION = 0.5f
        private const val ANIMATION_FRAME_DURATION = 0.15f
    }

    override var directionRotation: Direction
        get() = body.cardinalRotation
        set(value) {
            body.cardinalRotation = value
        }

    private val sleepTimer = Timer(SLEEP_DURATION)
    private val riseDropTimer = Timer(RISE_DROP_DURATION)
    private val petalTimer = Timer(PETAL_DURATION)
    private var petalCount = 4
    private lateinit var hanabiranState: HanabiranState

    override val damageNegotiations =
        objectMapOf<KClass<out IDamager>, DamageNegotiation>(
            Bullet::class pairTo dmgNeg(10),
            Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            ChargedShot::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            ChargedShotExplosion::class pairTo dmgNeg(ConstVals.MAX_HEALTH)
        )

    override fun init() {
        super.init()
        if (atlas == null) atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.setBottomCenterToPoint(bounds.getBottomCenterPoint())
        hanabiranState = HanabiranState.SLEEPING
        directionRotation =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class).uppercase())
    }

    private fun shoot() {
        val start = body.getCenter()
        val target = megaman.body.getCenter()
        val trajectory = target.sub(start).nor()

        val petal = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.PETAL)!!
        GameLogger.debug(
            TAG,
            "Shooting petal. Start: $start. Target: $target. Trajectory: $trajectory. Petal: $petal"
        )
        petal.spawn(
            props(
                ConstKeys.POSITION pairTo start,
                ConstKeys.TRAJECTORY pairTo trajectory,
                ConstKeys.OWNER pairTo this
            )
        )
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            when (hanabiranState) {
                HanabiranState.SLEEPING -> {
                    sleepTimer.update(it)
                    if (sleepTimer.isJustFinished()) {
                        sleepTimer.reset()
                        hanabiranState = HanabiranState.RISING
                    }
                }

                HanabiranState.RISING -> {
                    riseDropTimer.update(it)
                    if (riseDropTimer.isJustFinished()) {
                        riseDropTimer.reset()
                        petalCount = 4
                        hanabiranState = HanabiranState.PETAL_4
                    }
                }

                HanabiranState.PETAL_4,
                HanabiranState.PETAL_3,
                HanabiranState.PETAL_2,
                HanabiranState.PETAL_1,
                HanabiranState.PETAL_0 -> {
                    petalTimer.update(it)
                    if (petalTimer.isJustFinished()) {
                        petalCount--
                        hanabiranState =
                            if (petalCount < 0) {
                                HanabiranState.DROPPING
                            } else {
                                shoot()
                                HanabiranState.valueOf("PETAL_$petalCount")
                            }
                        petalTimer.reset()
                    }
                }

                HanabiranState.DROPPING -> {
                    riseDropTimer.update(it)
                    if (riseDropTimer.isJustFinished()) {
                        riseDropTimer.reset()
                        hanabiranState = HanabiranState.SLEEPING
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val fixturesRectangle = GameRectangle()

        val bodyFixture = Fixture(body, FixtureType.BODY, fixturesRectangle)
        bodyFixture.attachedToBody = false
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, fixturesRectangle)
        damagerFixture.attachedToBody = false
        body.addFixture(damagerFixture)
        debugShapes.add { if (damagerFixture.active) damagerFixture.getShape() else null }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, fixturesRectangle)
        damageableFixture.attachedToBody = false
        body.addFixture(damageableFixture)
        debugShapes.add { if (damageableFixture.active) damageableFixture.getShape() else null }

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            fixturesRectangle.setSize(
                (when (hanabiranState) {
                    HanabiranState.SLEEPING -> Vector2.Zero
                    HanabiranState.RISING -> {
                        if (riseDropTimer.time >= 0.3f) Vector2(0.75f, 0.75f)
                        else if (riseDropTimer.time >= 0.15f) Vector2(0.75f, 0.5f)
                        else Vector2(0.75f, 0.25f)
                    }

                    HanabiranState.DROPPING -> {
                        if (riseDropTimer.time >= 0.3f) Vector2(0.75f, 0.25f)
                        else if (riseDropTimer.time >= 0.15f) Vector2(0.75f, 0.5f)
                        else Vector2(0.75f, 0.75f)
                    }

                    HanabiranState.PETAL_4,
                    HanabiranState.PETAL_3,
                    HanabiranState.PETAL_2,
                    HanabiranState.PETAL_1,
                    HanabiranState.PETAL_0 -> Vector2(0.75f, 0.85f)
                })
                    .scl(ConstVals.PPM.toFloat())
            )

            fixturesRectangle.positionOnPoint(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)

            val fixturesOn =
                !hanabiranState.equalsAny(HanabiranState.SLEEPING, HanabiranState.RISING, HanabiranState.DROPPING)
            bodyFixture.active = fixturesOn
            damagerFixture.active = fixturesOn
            damageableFixture.active = fixturesOn
        })

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setOriginCenter()
            _sprite.rotation = directionRotation.rotation
            val position = DirectionPositionMapper.getPosition(directionRotation).opposite()
            _sprite.setPosition(body.getPositionPoint(position), position)
            _sprite.hidden = hanabiranState == HanabiranState.SLEEPING || damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (hanabiranState) {
                HanabiranState.RISING -> "Rise"
                HanabiranState.DROPPING -> "Drop"
                HanabiranState.PETAL_4 -> "4PetalsSpin"
                HanabiranState.PETAL_3 -> "3PetalsSpin"
                HanabiranState.PETAL_2 -> "2PetalsSpin"
                HanabiranState.PETAL_1 -> "1PetalSpin"
                HanabiranState.PETAL_0 -> "NoPetalsSpin"
                HanabiranState.SLEEPING -> null
            }
        }
        val animations =
            objectMapOf<String, IAnimation>(
                "1PetalSpin" pairTo
                        Animation(
                            atlas!!.findRegion("Hanabiran/1PetalSpin"),
                            1,
                            4,
                            ANIMATION_FRAME_DURATION,
                            true
                        ),
                "2PetalsSpin" pairTo
                        Animation(
                            atlas!!.findRegion("Hanabiran/2PetalsSpin"),
                            1,
                            4,
                            ANIMATION_FRAME_DURATION,
                            true
                        ),
                "3PetalsSpin" pairTo
                        Animation(
                            atlas!!.findRegion("Hanabiran/3PetalsSpin"),
                            1,
                            4,
                            ANIMATION_FRAME_DURATION,
                            true
                        ),
                "4PetalsSpin" pairTo
                        Animation(
                            atlas!!.findRegion("Hanabiran/4PetalsSpin"),
                            1,
                            2,
                            ANIMATION_FRAME_DURATION,
                            true
                        ),
                "NoPetalsSpin" pairTo
                        Animation(
                            atlas!!.findRegion("Hanabiran/NoPetalsSpin"),
                            1,
                            2,
                            ANIMATION_FRAME_DURATION,
                            true
                        ),
                "Rise" pairTo
                        Animation(
                            atlas!!.findRegion("Hanabiran/Rise"), 1, 3, ANIMATION_FRAME_DURATION, false
                        ),
                "Drop" pairTo
                        Animation(
                            atlas!!.findRegion("Hanabiran/Drop"), 1, 3, ANIMATION_FRAME_DURATION, false
                        ),
            )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
