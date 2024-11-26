package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.getRandom
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.getCenter
import com.mega.game.engine.common.shapes.toGameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.motion.ArcMotion
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.HazardsFactory
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.hazards.AsteroidsSpawner
import com.megaman.maverick.game.entities.projectiles.Asteroid
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.isSensing
import kotlin.reflect.KClass

class MoonHeadMiniBoss(game: MegamanMaverickGame) : AbstractBoss(game, dmgDuration = DAMAGE_DUR), IAnimatedEntity {

    companion object {
        const val TAG = "MoonHeadMiniBoss"
        private const val DAMAGE_DUR = 0.25f
        private const val SHOOT_SPEED = 6f
        private const val ASTEROID_OFFSET_Y = -0.65f
        private const val ARC_SPEED = 6f
        private const val ARC_FACTOR = 0.35f
        private const val ARC_FACTOR_CALCULATIONS = 8
        private const val DISTANCE_FACTOR = 0.75f
        private const val DELAY = 0.5f
        private const val DARK_DUR = 0.75f
        private const val AWAKEN_DUR = 1.75f
        private const val SHOOT_INIT_DELAY = 0.25f
        private const val SHOOT_DELAY = 0.25f
        private const val SHOOT_DUR = 0.75f
        private const val CRUMBLE_DUR = 0.3f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class MoonHeadState { DELAY, DARK, AWAKEN, SHOOT, MOVE, CRUMBLE }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(1),
        Fireball::class pairTo dmgNeg(1),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 2 else 1
        },
        ChargedShotExplosion::class pairTo dmgNeg(1)
    )

    private val loop = Loop(MoonHeadState.entries.toTypedArray().toGdxArray())
    private val timers = objectMapOf(
        "delay" pairTo Timer(DELAY),
        "dark" pairTo Timer(DARK_DUR),
        "awaken" pairTo Timer(AWAKEN_DUR),
        "shoot_init" pairTo Timer(SHOOT_INIT_DELAY),
        "shoot_delay" pairTo Timer(SHOOT_DELAY),
        "shoot" pairTo Timer(SHOOT_DUR),
        "crumble" pairTo Timer(CRUMBLE_DUR),
    )
    private lateinit var area: GameRectangle
    private lateinit var arcMotion: ArcMotion
    private var firstSpawn: Vector2? = null
    private var asteroidsSpawner: AsteroidsSpawner? = null

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
            regions.put("dark", atlas.findRegion("$TAG/Dark"))
            regions.put("awaken", atlas.findRegion("$TAG/Awaken"))
            regions.put("angry", atlas.findRegion("$TAG/Angry"))
            regions.put("shoot", atlas.findRegion("$TAG/Shoot"))
            regions.put("crumble", atlas.findRegion("$TAG/Crumble"))
            regions.put("damaged", atlas.findRegion("$TAG/Damaged"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.ORB, false)
        super.onSpawn(spawnProps)

        loop.reset()
        timers.values().forEach { it.reset() }

        area = spawnProps.get(ConstKeys.AREA, RectangleMapObject::class)!!.rectangle.toGameRectangle()
        firstSpawn = spawnProps.get(ConstKeys.FIRST, RectangleMapObject::class)!!.rectangle.getCenter()

        if (spawnProps.containsKey(AsteroidsSpawner.TAG)) {
            val asteroidsSpawnerBounds =
                spawnProps.get(AsteroidsSpawner.TAG, RectangleMapObject::class)!!.rectangle.toGameRectangle()
            asteroidsSpawner =
                EntityFactories.fetch(EntityType.HAZARD, HazardsFactory.ASTEROIDS_SPAWNER)!! as AsteroidsSpawner
            asteroidsSpawner!!.spawn(
                props(
                    ConstKeys.BOUNDS pairTo asteroidsSpawnerBounds,
                    "${ConstKeys.DESTROY}_${ConstKeys.CHILDREN}" pairTo true,
                )
            )
        }
    }

    override fun isReady(delta: Float) = true // TODO

    override fun onDefeated(delta: Float) {
        super.onDefeated(delta)
        destroyAsteroidsSpawner()
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyAsteroidsSpawner()
    }

    private fun destroyAsteroidsSpawner() {
        asteroidsSpawner?.destroy()
        asteroidsSpawner = null
    }

    private fun getRandomSpawn(): Vector2 {
        val randomX = getRandom(area.x, area.getMaxX())
        val randomY = getRandom(area.y, area.getMaxY())
        return Vector2(randomX, randomY)
    }

    private fun shoot() {
        val spawn = body.getCenter().add(0f, ASTEROID_OFFSET_Y * ConstVals.PPM)
        val impulse = megaman().body.getCenter().sub(spawn).nor().scl(SHOOT_SPEED * ConstVals.PPM)
        val asteroid = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.ASTEROID)!!
        asteroid.spawn(
            props(
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.IMPULSE pairTo impulse,
                ConstKeys.OWNER pairTo this,
                ConstKeys.TYPE pairTo Asteroid.BLUE,
                "${ConstKeys.ROTATION}_${ConstKeys.SPEED}" pairTo Asteroid.MAX_ROTATION_SPEED
            )
        )
    }

    private fun calculateBestArcMotion(): ArcMotion {
        val left = body.x < megaman().body.x
        val position = when {
            body.y < megaman().body.y -> if (left) Position.BOTTOM_LEFT else Position.BOTTOM_RIGHT
            else -> if (left) Position.TOP_LEFT else Position.TOP_RIGHT
        }
        val target = megaman().body.getPositionPoint(position)
        val arcMotion1 = ArcMotion(
            startPosition = body.getCenter(),
            targetPosition = target,
            speed = ARC_SPEED * ConstVals.PPM,
            arcFactor = ARC_FACTOR,
            continueBeyondTarget = true
        )
        val arcMotion2 = arcMotion1.copy()
        arcMotion2.arcFactor = -ARC_FACTOR

        val blocks = MegaGameEntities.getEntitiesOfType(EntityType.BLOCK).map { it as Block }
        val mockBody = GameRectangle(body)

        val totalDistance = body.getCenter().dst(target) * DISTANCE_FACTOR
        var winningArcMotion = arcMotion1

        for (i in 0..ARC_FACTOR_CALCULATIONS) {
            val t = i * (totalDistance / ARC_FACTOR_CALCULATIONS)
            val pos1 = arcMotion1.compute(t)
            val pos2 = arcMotion2.compute(t)

            mockBody.setCenter(pos1)
            val hitsBlockPos1 = blocks.any { it.body.overlaps(mockBody as Rectangle) }
            mockBody.setCenter(pos2)
            val hitsBlockPos2 = blocks.any { it.body.overlaps(mockBody as Rectangle) }
            when {
                hitsBlockPos1 -> {
                    winningArcMotion = arcMotion2
                    break
                }

                hitsBlockPos2 -> {
                    winningArcMotion = arcMotion1
                    break
                }
            }

            val dist1 = pos1.dst2(target)
            val dist2 = pos2.dst2(target)
            winningArcMotion = if (dist1 < dist2) arcMotion1 else arcMotion2
        }

        return winningArcMotion
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
                    val key = loop.getCurrent().name.lowercase()
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

                        arcMotion = calculateBestArcMotion()

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

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameCircle().setRadius(1.35f * ConstVals.PPM))
        body.addFixture(bodyFixture)
        bodyFixture.rawShape.color = Color.GRAY
        debugShapes.add { bodyFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(1.425f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        damagerFixture.rawShape.color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameCircle().setRadius(1.425f * ConstVals.PPM))
        body.addFixture(damageableFixture)
        damageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.fixtures.forEach {
                (it.second as Fixture).active = !loop.getCurrent().equalsAny(
                    MoonHeadState.DELAY, MoonHeadState.DARK, MoonHeadState.CRUMBLE
                )
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        sprite.setSize(3f * ConstVals.PPM, 2.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setCenter(body.getCenter())
            sprite.hidden = damageBlink || loop.getCurrent() == MoonHeadState.DELAY
            val alpha = if (defeated) 1f - defeatTimer.getRatio() else 1f
            sprite.setAlpha(alpha)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            if (defeated) "damaged"
            else when (loop.getCurrent()) {
                MoonHeadState.DELAY, MoonHeadState.DARK -> "dark"
                MoonHeadState.AWAKEN -> "awaken"
                MoonHeadState.SHOOT -> "shoot"
                MoonHeadState.MOVE -> "angry"
                MoonHeadState.CRUMBLE -> "crumble"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "dark" pairTo Animation(regions.get("dark")),
            "awaken" pairTo Animation(regions.get("awaken"), 5, 2, 0.1f, false),
            "shoot" pairTo Animation(regions.get("shoot")),
            "angry" pairTo Animation(regions.get("angry")),
            "crumble" pairTo Animation(regions.get("crumble"), 1, 3, 0.1f, false),
            "damaged" pairTo Animation(regions.get("damaged"), 3, 1, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
