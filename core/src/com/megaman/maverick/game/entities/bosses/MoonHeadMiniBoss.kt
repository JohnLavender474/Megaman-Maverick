package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.extensions.equalsAny
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.toGdxArray
import com.engine.common.getRandom
import com.engine.common.objects.Loop
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameCircle
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.getCenter
import com.engine.common.shapes.toGameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.motion.ArcMotion
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.Asteroid
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.BodySense
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.isSensing
import kotlin.reflect.KClass

class MoonHeadMiniBoss(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity {

    companion object {
        const val TAG = "MoonHeadMiniBoss"
        private const val SHOOT_SPEED = 6f
        private const val ASTEROID_OFFSET_Y = -0.65f
        private const val ARC_SPEED = 8f
        private const val ARC_FACTOR = 0.5f
        private const val DELAY = 0.5f
        private const val DARK_DUR = 0.5f
        private const val AWAKEN_DUR = 1.75f
        private const val SHOOT_INIT_DELAY = 0.25f
        private const val SHOOT_DELAY = 0.15f
        private const val SHOOT_DUR = 0.65f
        private const val CRUMBLE_DUR = 0.3f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class MoonHeadState {
        DELAY, DARK, AWAKEN, SHOOT, MOVE, CRUMBLE
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(2),
        Fireball::class to dmgNeg(3),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 3 else 2
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 2 else 1
        }
    )

    private val loop = Loop(MoonHeadState.values().toGdxArray())
    private val timers = objectMapOf(
        "delay" to Timer(DELAY),
        "dark" to Timer(DARK_DUR),
        "awaken" to Timer(AWAKEN_DUR),
        "shoot_init" to Timer(SHOOT_INIT_DELAY),
        "shoot_delay" to Timer(SHOOT_DELAY),
        "shoot" to Timer(SHOOT_DUR),
        "crumble" to Timer(CRUMBLE_DUR),
    )

    private lateinit var area: GameRectangle
    private lateinit var arcMotion: ArcMotion

    private var negativeArc = false
    private var firstSpawn: Vector2? = null

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            regions.put("dark", atlas.findRegion("$TAG/Dark"))
            regions.put("awaken", atlas.findRegion("$TAG/Awaken"))
            regions.put("angry", atlas.findRegion("$TAG/Angry"))
            regions.put("shoot", atlas.findRegion("$TAG/Shoot"))
            regions.put("crumble", atlas.findRegion("$TAG/Crumble"))
        }
        super<AbstractBoss>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        area = spawnProps.get(ConstKeys.AREA, RectangleMapObject::class)!!.rectangle.toGameRectangle()
        loop.reset()
        timers.values().forEach { it.reset() }
        negativeArc = false
        firstSpawn = spawnProps.get(ConstKeys.FIRST, RectangleMapObject::class)!!.rectangle.getCenter()
    }

    private fun getRandomSpawn(): Vector2 {
        val randomX = getRandom(area.x, area.getMaxX())
        val randomY = getRandom(area.y, area.getMaxY())
        return Vector2(randomX, randomY)
    }

    private fun shoot() {
        val spawn = body.getCenter().add(0f, ASTEROID_OFFSET_Y * ConstVals.PPM)
        val impulse = getMegaman().body.getCenter().sub(spawn).nor().scl(SHOOT_SPEED * ConstVals.PPM)
        val asteroid = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.ASTEROID)!!
        game.engine.spawn(
            asteroid,
            props(
                ConstKeys.POSITION to spawn,
                ConstKeys.IMPULSE to impulse,
                ConstKeys.OWNER to this,
                ConstKeys.TYPE to Asteroid.BLUE,
                "${ConstKeys.ROTATION}_${ConstKeys.SPEED}" to Asteroid.MAX_ROTATION_SPEED
            )
        )
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!ready) return@add
            if (defeated) {
                explodeOnDefeat(delta)
                return@add
            }

            when (loop.getCurrent()) {
                MoonHeadState.DELAY, MoonHeadState.DARK, MoonHeadState.AWAKEN, MoonHeadState.CRUMBLE -> {
                    val key = if (loop.getCurrent() == MoonHeadState.DELAY) "delay"
                    else if (loop.getCurrent() == MoonHeadState.DARK) "dark"
                    else if (loop.getCurrent() == MoonHeadState.AWAKEN) "awaken"
                    else "crumble"

                    val timer = timers[key]

                    timer.update(delta)
                    if (timer.isFinished()) {
                        timer.reset()
                        loop.next()

                        if (loop.getCurrent() == MoonHeadState.DARK) {
                            val spawn = if (firstSpawn != null) firstSpawn!! else getRandomSpawn()
                            firstSpawn = null
                            body.setCenter(spawn)
                        } else if (loop.getCurrent() == MoonHeadState.AWAKEN) requestToPlaySound(
                            SoundAsset.SHAKE_SOUND, false
                        )
                    }
                }

                MoonHeadState.SHOOT -> {
                    val shootInitTimer = timers["shoot_init"]
                    shootInitTimer.update(delta)
                    if (!shootInitTimer.isFinished()) return@add

                    val shootDelayTimer = timers["shoot_delay"]
                    shootDelayTimer.update(delta)
                    if (shootDelayTimer.isFinished()) {
                        shoot()
                        shootDelayTimer.reset()
                    }

                    val shootTimer = timers["shoot"]
                    shootTimer.update(delta)
                    if (shootTimer.isFinished()) {
                        shootTimer.reset()
                        shootInitTimer.reset()
                        shootDelayTimer.reset()

                        arcMotion = ArcMotion(
                            startPosition = body.getCenter(),
                            targetPosition = getMegaman().body.getCenter(),
                            speed = ARC_SPEED * ConstVals.PPM,
                            arcFactor = ARC_FACTOR * if (negativeArc) -1f else 1f,
                            continueBeyondTarget = true
                        )
                        negativeArc = !negativeArc

                        loop.next()
                        return@add
                    }
                }

                MoonHeadState.MOVE -> {
                    arcMotion.update(delta)
                    val position = arcMotion.getMotionValue()
                    if (position == null || body.isSensing(BodySense.BODY_TOUCHING_BLOCK)) {
                        loop.next()
                        requestToPlaySound(SoundAsset.BURST_SOUND, false)
                        return@add
                    }
                    body.setCenter(position)
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2.85f * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameCircle().setRadius(1.35f * ConstVals.PPM))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(1.425f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameCircle().setRadius(1.425f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.fixtures.forEach {
                (it.second as Fixture).active = !loop.getCurrent().equalsAny(
                    MoonHeadState.DELAY, MoonHeadState.DARK, MoonHeadState.CRUMBLE
                )
            }
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(3f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
            _sprite.hidden = damageBlink || loop.getCurrent() == MoonHeadState.DELAY
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (loop.getCurrent()) {
                MoonHeadState.DELAY, MoonHeadState.DARK -> "dark"
                MoonHeadState.AWAKEN -> "awaken"
                MoonHeadState.SHOOT -> "shoot"
                MoonHeadState.MOVE -> "angry"
                MoonHeadState.CRUMBLE -> "crumble"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "dark" to Animation(regions.get("dark")),
            "awaken" to Animation(regions.get("awaken"), 5, 2, 0.1f, false),
            "shoot" to Animation(regions.get("shoot")),
            "angry" to Animation(regions.get("angry")),
            "crumble" to Animation(regions.get("crumble"), 1, 3, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}