package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
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

import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
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
        private const val MIN_SHOOT_ORB_ANGLE = 45f
        private const val MAX_SHOOT_ORB_ANGLE = 100f
        private val chinOffsets = gdxArrayOf(
            Vector2(-2.75f, 0.7f),
            Vector2(-2.75f, 0.1f),
            Vector2(-2.75f, -0.5f)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class SphinxMiniBossState {
        WAIT, OPENING, LAUNCH_BALL, SHOOT_ORBS, CLOSING
    }

    override val damageNegotiations =
        objectMapOf<KClass<out IDamager>, DamageNegotiation>(
            Bullet::class pairTo dmgNeg(1),
            ChargedShot::class pairTo dmgNeg {
                it as ChargedShot
                if (it.fullyCharged) 2 else 1
            }, ChargedShotExplosion::class pairTo dmgNeg {
                it as ChargedShotExplosion
                if (it.fullyCharged) 2 else 1
            }
        )

    private val loop = Loop(SphinxMiniBossState.entries.toTypedArray().toGdxArray())
    private val timers = objectMapOf(
        "wait" pairTo Timer(WAIT_DUR),
        "opening" pairTo Timer(
            OPEN_DUR, gdxArrayOf(
                TimeMarkedRunnable(0f) { activeChinIndex = 0 },
                TimeMarkedRunnable(0.1f) { activeChinIndex = 1 },
                TimeMarkedRunnable(0.2f) { activeChinIndex = 2 })
        ),
        "shoot_orbs" pairTo Timer(
            SHOOT_ORBS_DUR, gdxArrayOf(
                TimeMarkedRunnable(0.5f) { shootOrb() },
                TimeMarkedRunnable(1.5f) { shootOrb() },
                TimeMarkedRunnable(2.5f) { shootOrb() })
        ),
        "closing" pairTo Timer(
            OPEN_DUR, gdxArrayOf(
                TimeMarkedRunnable(0f) { activeChinIndex = 2 },
                TimeMarkedRunnable(0.1f) { activeChinIndex = 1 },
                TimeMarkedRunnable(0.2f) { activeChinIndex = 0 })
        ),
        "laugh" pairTo Timer(LAUGH_DUR)
    )
    private val chinBounds = GameRectangle().setSize(1.5f * ConstVals.PPM, 0.5f * ConstVals.PPM)
    private var activeChinIndex = 0
    private var sphinxBall: SphinxBall? = null
    private var chunkOrbs = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
            regions.put("defeated", atlas.findRegion("$TAG/defeated"))
            regions.put("wait", atlas.findRegion("$TAG/wait"))
            regions.put("open", atlas.findRegion("$TAG/open"))
            regions.put("laugh", atlas.findRegion("$TAG/laugh"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
        addDebugShapeSupplier { chinBounds }
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.ORB, false)
        GameLogger.debug(TAG, "spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_RIGHT)
        body.setBottomRightToPoint(spawn)

        loop.reset()
        timers.forEach {
            if (it.key == "laugh") it.value.setToEnd()
            else it.value.reset()
        }

        activeChinIndex = 0
        chunkOrbs = false
    }

    override fun isReady(delta: Float) = true // TODO

    override fun onDestroy() {
        super.onDestroy()
        sphinxBall?.destroy()
        sphinxBall = null
    }

    private fun launchBall() {
        sphinxBall = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.SPHINX_BALL)!! as SphinxBall
        sphinxBall!!.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo chinBounds.getPositionPoint(Position.TOP_CENTER),
                ConstKeys.X pairTo -BALL_SPEED,
                ConstKeys.GRAVITY_ON pairTo false,
                ConstKeys.SPIN pairTo true
            )
        )
    }

    private fun shootOrb() {
        val arigockBall = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.ARIGOCK_BALL)!!
        val spawn = chinBounds.getPositionPoint(Position.TOP_CENTER).add(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat())
        /*
        val impulse = if (chunkOrbs) MegaUtilMethods.calculateJumpImpulse(
            spawn, getMegaman().body.getCenter(), MAX_CHUNK_ORB_IMPULSE * ConstVals.PPM, CHUNK_X_SCALAR
        ) else getMegaman().body.getCenter().sub(spawn).nor().scl(ORB_SPEED * ConstVals.PPM)
         */
        val angle = getRandom(MIN_SHOOT_ORB_ANGLE, MAX_SHOOT_ORB_ANGLE)
        val impulse = Vector2(0f, ORB_SPEED * ConstVals.PPM).rotateDeg(angle)
        val gravityOn = false // chunkOrbs
        arigockBall.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.IMPULSE pairTo impulse,
                ConstKeys.GRAVITY_ON pairTo gravityOn
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

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture1 = Fixture(
            body, FixtureType.BODY, GameRectangle().setSize(
                7f * ConstVals.PPM, 2.25f * ConstVals.PPM
            )
        )
        bodyFixture1.offsetFromBodyAttachment.x = 0.5f * ConstVals.PPM
        bodyFixture1.offsetFromBodyAttachment.y = -1.125f * ConstVals.PPM
        body.addFixture(bodyFixture1)
        debugShapes.add { bodyFixture1}

        val bodyFixture2 = Fixture(
            body, FixtureType.BODY, GameRectangle().setSize(
                3f * ConstVals.PPM, ConstVals.PPM.toFloat()
            )
        )
        bodyFixture2.offsetFromBodyAttachment.x = -0.25f * ConstVals.PPM
        bodyFixture2.offsetFromBodyAttachment.y = 0.5f * ConstVals.PPM
        body.addFixture(bodyFixture2)
        debugShapes.add { bodyFixture2}

        val bodyFixture3 = Fixture(
            body, FixtureType.BODY, GameRectangle().setSize(
                2f * ConstVals.PPM, 1.15f * ConstVals.PPM
            )
        )
        bodyFixture3.offsetFromBodyAttachment.x = -0.85f * ConstVals.PPM
        bodyFixture3.offsetFromBodyAttachment.y = 1.75f * ConstVals.PPM
        body.addFixture(bodyFixture3)
        debugShapes.add { bodyFixture3}

        // create copies of body fixtures for damager and shield fixture
        gdxArrayOf(FixtureType.DAMAGER, FixtureType.SHIELD).forEach { type ->
            body.forEachFixture { fixture ->
                fixture as Fixture
                val newFixture = Fixture(
                    body = body,
                    type = type,
                    rawShape = fixture.rawShape.copy(),
                    offsetFromBodyAttachment = fixture.offsetFromBodyAttachment.cpy()
                )
                body.addFixture(newFixture)
            }
        }

        val damageableFixture = Fixture(
            body, FixtureType.DAMAGEABLE, GameRectangle().setSize(
                1.25f * ConstVals.PPM, 0.5f * ConstVals.PPM
            )
        )
        damageableFixture.offsetFromBodyAttachment.x = -1.85f * ConstVals.PPM
        damageableFixture.offsetFromBodyAttachment.y = 1.75f * ConstVals.PPM
        body.addFixture(damageableFixture)
        debugShapes.add { damageableFixture}

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(10f * ConstVals.PPM, 9f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.hidden = damageBlink
            sprite.setAlpha(if (defeated) 1f - defeatTimer.getRatio() else 1f)
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
            "wait" pairTo Animation(regions["wait"], 2, 1, gdxArrayOf(1f, 0.15f), true),
            "open" pairTo Animation(regions["open"], 3, 1, 0.1f, false),
            "close" pairTo Animation(regions["open"], 3, 1, 0.1f, false).reversed(),
            "laugh" pairTo Animation(regions["laugh"], 2, 1, 0.1f, true),
            "defeated" pairTo Animation(regions["defeated"])
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
