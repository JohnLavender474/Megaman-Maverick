package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.enums.Position
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.toGdxArray
import com.engine.common.objects.Loop
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.TimeMarkedRunnable
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IDrawableShapesEntity
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
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.SphinxBall
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class SphinxMiniBoss(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IDrawableShapesEntity {

    companion object {
        const val TAG = "SphinxMiniBoss"
        private const val WAIT_DUR = 1.25f
        private const val OPEN_DUR = 0.25f
        private const val SHOOT_ORBS_DUR = 2.5f
        private const val LAUGH_DUR = 0.5f
        private const val BALL_SPEED = 3f
        private const val ORB_SPEED = 8f
        private const val MAX_CHUNK_ORB_IMPULSE = 10f
        private const val CHUNK_X_SCALAR = 1.25f
        private val chinOffsets = gdxArrayOf(
            Vector2(-2.75f, 0.7f), Vector2(-2.75f, 0.1f), Vector2(-2.75f, -0.5f)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class SphinxMiniBossState {
        WAIT, OPENING, LAUNCH_BALL, SHOOT_ORBS, CLOSING
    }

    override val damageNegotiations =
        objectMapOf<KClass<out IDamager>, DamageNegotiation>(Bullet::class to dmgNeg(1), ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 2 else 1
        }, ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 2 else 1
        })

    private val loop = Loop(SphinxMiniBossState.values().toGdxArray())
    private val timers = objectMapOf(
        "wait" to Timer(WAIT_DUR), "opening" to Timer(
            OPEN_DUR, gdxArrayOf(TimeMarkedRunnable(0f) { activeChinIndex = 0 },
                TimeMarkedRunnable(0.1f) { activeChinIndex = 1 },
                TimeMarkedRunnable(0.2f) { activeChinIndex = 2 })
        ), "shoot_orbs" to Timer(
            SHOOT_ORBS_DUR, gdxArrayOf(TimeMarkedRunnable(0.5f) { shootOrb() },
                TimeMarkedRunnable(1f) { shootOrb() },
                TimeMarkedRunnable(1.5f) { shootOrb() },
                TimeMarkedRunnable(2f) { shootOrb() })
        ), "closing" to Timer(
            OPEN_DUR, gdxArrayOf(TimeMarkedRunnable(0f) { activeChinIndex = 2 },
                TimeMarkedRunnable(0.1f) { activeChinIndex = 1 },
                TimeMarkedRunnable(0.2f) { activeChinIndex = 0 })
        ), "laugh" to Timer(LAUGH_DUR)
    )
    private val chinBounds = GameRectangle().setSize(1.5f * ConstVals.PPM, 0.5f * ConstVals.PPM)
    private var activeChinIndex = 0
    private var sphinxBall: SphinxBall? = null
    private var chunkOrbs = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            regions.put("defeated", atlas.findRegion("$TAG/defeated"))
            regions.put("wait", atlas.findRegion("$TAG/wait"))
            regions.put("open", atlas.findRegion("$TAG/open"))
            regions.put("laugh", atlas.findRegion("$TAG/laugh"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
        chinBounds.color = Color.BLUE
        addDebugShapeSupplier { chinBounds }
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "spawnProps=$spawnProps")
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomRightPoint()
        body.setBottomRightToPoint(spawn)
        loop.reset()
        timers.forEach {
            if (it.key == "laugh") it.value.setToEnd()
            else it.value.reset()
        }
        activeChinIndex = 0
        chunkOrbs = false
    }

    override fun onDestroy() {
        super.onDestroy()
        sphinxBall?.kill()
        sphinxBall = null
    }

    private fun launchBall() {
        sphinxBall = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SPHINX_BALL)!! as SphinxBall
        game.engine.spawn(
            sphinxBall!!, props(
                ConstKeys.OWNER to this,
                ConstKeys.POSITION to chinBounds.getTopCenterPoint(),
                ConstKeys.X to -BALL_SPEED,
                ConstKeys.GRAVITY_ON to false,
                ConstKeys.SPIN to true
            )
        )
    }

    private fun shootOrb() {
        val arigockBall = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.ARIGOCK_BALL)!!
        val spawn = chinBounds.getTopCenterPoint().add(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat())
        val impulse = if (chunkOrbs) MegaUtilMethods.calculateJumpImpulse(
            spawn, getMegaman().body.getCenter(), MAX_CHUNK_ORB_IMPULSE * ConstVals.PPM, CHUNK_X_SCALAR
        ) else getMegaman().body.getCenter().sub(spawn).nor().scl(ORB_SPEED * ConstVals.PPM)
        game.engine.spawn(
            arigockBall, props(
                ConstKeys.POSITION to spawn, ConstKeys.IMPULSE to impulse, ConstKeys.GRAVITY_ON to chunkOrbs
            )
        )
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            val offset = chinOffsets[activeChinIndex]
            chinBounds.setCenter(body.getCenter().add(offset.x * ConstVals.PPM, offset.y * ConstVals.PPM))

            if (!ready) return@add
            if (defeated) {
                explodeOnDefeat(delta)
                return@add
            }

            val laughTimer = timers["laugh"]
            if (!laughTimer.isFinished()) {
                laughTimer.update(delta)
                return@add
            }

            when (loop.getCurrent()) {
                SphinxMiniBossState.LAUNCH_BALL -> {
                    if (!sphinxBall!!.feetFixture.getShape().overlaps(chinBounds)) {
                        sphinxBall!!.body.physics.gravityOn = true
                        sphinxBall = null
                        loop.next()
                    }
                }

                else -> {
                    val timerKey = loop.getCurrent().name.lowercase()
                    val timer = timers[timerKey]
                    timer.update(delta)
                    if (timer.isFinished()) {
                        timer.reset()
                        loop.next()
                        if (loop.getCurrent() == SphinxMiniBossState.LAUNCH_BALL) {
                            launchBall()
                            chunkOrbs = !chunkOrbs
                            GameLogger.debug(TAG, "Will chunk orbs: $chunkOrbs")
                        }
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(7.25f * ConstVals.PPM, 4.5f * ConstVals.PPM)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture1 = Fixture(
            body, FixtureType.BODY, GameRectangle().setSize(
                7f * ConstVals.PPM, 2.25f * ConstVals.PPM
            )
        )
        bodyFixture1.offsetFromBodyCenter.x = 0.5f * ConstVals.PPM
        bodyFixture1.offsetFromBodyCenter.y = -1.125f * ConstVals.PPM
        body.addFixture(bodyFixture1)
        debugShapes.add { bodyFixture1.getShape() }

        val bodyFixture2 = Fixture(
            body, FixtureType.BODY, GameRectangle().setSize(
                3f * ConstVals.PPM, ConstVals.PPM.toFloat()
            )
        )
        bodyFixture2.offsetFromBodyCenter.x = -0.25f * ConstVals.PPM
        bodyFixture2.offsetFromBodyCenter.y = 0.5f * ConstVals.PPM
        body.addFixture(bodyFixture2)
        debugShapes.add { bodyFixture2.getShape() }

        val bodyFixture3 = Fixture(
            body, FixtureType.BODY, GameRectangle().setSize(
                2f * ConstVals.PPM, 1.15f * ConstVals.PPM
            )
        )
        bodyFixture3.offsetFromBodyCenter.x = -0.85f * ConstVals.PPM
        bodyFixture3.offsetFromBodyCenter.y = 1.75f * ConstVals.PPM
        body.addFixture(bodyFixture3)
        debugShapes.add { bodyFixture3.getShape() }

        // create copies of body fixtures for damager and shield fixture
        gdxArrayOf(FixtureType.DAMAGER, FixtureType.SHIELD).forEach { fixtureType ->
            body.fixtures.map { it.second }.forEach { bodyFixture ->
                val copy = (bodyFixture as Fixture).copy()
                bodyFixture.type = fixtureType
                body.addFixture(copy)
            }
        }

        val damageableFixture = Fixture(
            body, FixtureType.DAMAGEABLE, GameRectangle().setSize(
                1.25f * ConstVals.PPM, 0.5f * ConstVals.PPM
            )
        )
        damageableFixture.offsetFromBodyCenter.x = -1.85f * ConstVals.PPM
        damageableFixture.offsetFromBodyCenter.y = 1.75f * ConstVals.PPM
        body.addFixture(damageableFixture)
        damageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(10f * ConstVals.PPM, 9f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            if (defeated) "defeated"
            else if (!timers["laugh"].isFinished()) "laugh"
            else when (loop.getCurrent()) {
                SphinxMiniBossState.WAIT -> "wait"
                SphinxMiniBossState.OPENING, SphinxMiniBossState.LAUNCH_BALL, SphinxMiniBossState.SHOOT_ORBS -> "open"
                SphinxMiniBossState.CLOSING -> "close"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "wait" to Animation(regions["wait"], 2, 1, gdxArrayOf(1f, 0.15f), true),
            "open" to Animation(regions["open"], 3, 1, 0.1f, false),
            "close" to Animation(regions["open"], 3, 1, 0.1f, false).reversed(),
            "laugh" to Animation(regions["laugh"], 2, 1, 0.1f, true),
            "defeated" to Animation(regions["defeated"])
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}