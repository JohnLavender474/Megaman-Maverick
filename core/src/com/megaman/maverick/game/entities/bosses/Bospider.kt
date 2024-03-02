package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.PolylineMapObject
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Queue
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.extensions.equalsAny
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.toGdxArray
import com.engine.common.objects.Loop
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.toGameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IParentEntity
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
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.toProps
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class Bospider(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IParentEntity {

    companion object {
        const val TAG = "Bospider"
        private const val SPAWN_DELAY = 2.5f
        private const val MAX_CHILDREN = 4
        private const val MIN_SPEED = 5f
        private const val MAX_SPEED = 15f
        private const val START_POINT_OFFSET = 2f
        private const val OPEN_EYE_MAX_DURATION = 2f
        private const val OPEN_EYE_MIN_DURATION = 0.5f
        private const val CLOSE_EYE_DURATION = 0.35f
        private const val DEBUG_TIMER = 1f
        private var climbRegion: TextureRegion? = null
        private var stillRegion: TextureRegion? = null
        private var openEyeRegion: TextureRegion? = null
    }

    private enum class BospiderState {
        SPAWN, CLIMB, OPEN_EYE, CLOSE_EYE, RETREAT
    }

    override var children = Array<IGameEntity>()
    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(1), Fireball::class to dmgNeg(2), ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 2 else 1
        }, ChargedShotExplosion::class to dmgNeg(1)
    )

    private val paths = Array<Array<Vector2>>()
    private val currentPath = Queue<Vector2>()
    private val stateLoop = Loop(BospiderState.values().toGdxArray())

    private val childrenSpawnPoints = Array<RectangleMapObject>()

    private val spawnDelayTimer = Timer(SPAWN_DELAY)
    private val closeEyeTimer = Timer(CLOSE_EYE_DURATION)
    private val debugTimer = Timer(DEBUG_TIMER)

    private lateinit var openEyeTimer: Timer
    private lateinit var spawn: Vector2

    override fun init() {
        if (climbRegion == null || stillRegion == null || openEyeRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            climbRegion = atlas.findRegion("Bospider/Climb")
            stillRegion = atlas.findRegion("Bospider/Still")
            openEyeRegion = atlas.findRegion("Bospider/OpenEye")
        }
        super<AbstractBoss>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn.x, spawn.y + START_POINT_OFFSET * ConstVals.PPM)

        paths.clear()
        childrenSpawnPoints.clear()
        spawnProps.forEach { _, value ->
            if (value is PolylineMapObject) {
                val rawPath = value.polyline.transformedVertices
                val path = Array<Vector2>()
                for (i in rawPath.indices step 2) path.add(Vector2(rawPath[i], rawPath[i + 1]))
                paths.add(path)
            } else if (value is RectangleMapObject) childrenSpawnPoints.add(value)
        }

        stateLoop.reset()
        spawnDelayTimer.reset()
    }

    override fun onDestroy() {
        super<AbstractBoss>.onDestroy()
        children.forEach { it.kill() }
        children.clear()
        paths.clear()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!ready) return@add

            debugTimer.update(delta)
            if (debugTimer.isFinished()) {
                GameLogger.debug(TAG, "defineUpdatablesComponent(): state = ${stateLoop.getCurrent()}")
                GameLogger.debug(TAG, "defineUpdatablesComponent(): position = ${body.getCenter()}")
                GameLogger.debug(TAG, "defineUpdatablesComponent(): velocity = ${body.physics.velocity}")
                GameLogger.debug(TAG, "defineUpdatablesComponent(): health = ${getCurrentHealth()}")
                GameLogger.debug(TAG, "defineUpdatablesComponent(): health ratio = ${getHealthRatio()}")
                GameLogger.debug(TAG, "defineUpdatablesComponent(): speed = ${getCurrentSpeed()}")
                GameLogger.debug(TAG, "defineUpdatablesComponent(): current path = $currentPath")
                debugTimer.reset()
            }

            val childIter = children.iterator()
            while (childIter.hasNext()) {
                val child = childIter.next()
                if (child.dead) childIter.remove()
            }

            when (stateLoop.getCurrent()) {
                BospiderState.SPAWN -> {
                    if (!body.getCenter().epsilonEquals(spawn, 0.1f * ConstVals.PPM)) {
                        moveToSpawn()
                        return@add
                    }

                    body.physics.velocity.setZero()

                    spawnDelayTimer.update(delta)
                    if (spawnDelayTimer.isFinished()) {
                        val numChildrenToSpawn = MAX_CHILDREN - children.size
                        for (i in 0 until numChildrenToSpawn) {
                            val spawnRectObject = childrenSpawnPoints.get(i)
                            val spawnProps = spawnRectObject.properties.toProps()
                            spawnProps.put(ConstKeys.BOUNDS, spawnRectObject.rectangle.toGameRectangle())
                            val babySpider = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.BABY_SPIDER)!!
                            game.gameEngine.spawn(babySpider, spawnProps)
                            children.add(babySpider)
                        }
                        val path = paths.random()
                        currentPath.clear()
                        path.forEach { currentPath.addLast(it) }
                        stateLoop.next()
                    }
                }

                BospiderState.CLIMB -> {
                    if (currentPath.isEmpty) {
                        body.physics.velocity.setZero()
                        val openEyeDuration =
                            OPEN_EYE_MIN_DURATION + (OPEN_EYE_MAX_DURATION - OPEN_EYE_MIN_DURATION) * (1f - getHealthRatio())
                        openEyeTimer = Timer(openEyeDuration)
                        stateLoop.next()
                        return@add
                    }

                    moveToNextTarget()
                    if (body.getCenter()
                            .epsilonEquals(currentPath.first(), 0.1f * ConstVals.PPM)
                    ) currentPath.removeFirst()
                }

                BospiderState.OPEN_EYE -> {
                    openEyeTimer.update(delta)
                    if (openEyeTimer.isFinished()) {
                        stateLoop.next()
                        closeEyeTimer.reset()
                    }
                }

                BospiderState.CLOSE_EYE -> {
                    closeEyeTimer.update(delta)
                    if (closeEyeTimer.isFinished()) stateLoop.next()
                }

                BospiderState.RETREAT -> {
                    body.physics.velocity.set(0f, getCurrentSpeed())
                    if (body.y >= spawn.y + START_POINT_OFFSET * ConstVals.PPM) {
                        body.physics.velocity.setZero()
                        body.setCenter(spawn.x, spawn.y + START_POINT_OFFSET * ConstVals.PPM)
                        spawnDelayTimer.reset()
                        stateLoop.next()
                    }
                }
            }
        }
    }

    private fun moveToSpawn() {
        val target = spawn.cpy()
        val center = body.getCenter()
        val directionToSpawn = target.sub(center).nor()
        val velocity = directionToSpawn.scl(MIN_SPEED).scl(ConstVals.PPM.toFloat())
        body.physics.velocity.set(velocity)
    }

    private fun moveToNextTarget() {
        val target = currentPath.first().cpy()
        val center = body.getCenter()
        val directionToTarget = target.sub(center).nor()
        val velocity = directionToTarget.scl(getCurrentSpeed())
        body.physics.velocity.set(velocity)
    }

    private fun getCurrentSpeed() = ConstVals.PPM * (MIN_SPEED + (MAX_SPEED - MIN_SPEED) * (1 - getHealthRatio()))

    private fun getHealthRatio() =
        (getCurrentHealth() - getMinHealth()).toFloat() / (getMaxHealth() - getMinHealth()).toFloat()

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM)
        val bodyFixture = Fixture(GameRectangle().setSize(2f * ConstVals.PPM), FixtureType.BODY)
        body.addFixture(bodyFixture)
        val damagerFixture = Fixture(GameRectangle().setSize(2f * ConstVals.PPM), FixtureType.DAMAGER)
        body.addFixture(damagerFixture)
        val damageableFixture = Fixture(GameRectangle().setSize(2f * ConstVals.PPM), FixtureType.DAMAGEABLE)
        body.addFixture(damageableFixture)
        val shieldFixture = Fixture(GameRectangle().setSize(2f * ConstVals.PPM), FixtureType.SHIELD)
        body.addFixture(shieldFixture)
        body.preProcess.put(ConstKeys.DEFAULT) {
            val shielded =
                stateLoop.getCurrent().equalsAny(BospiderState.SPAWN, BospiderState.CLIMB, BospiderState.RETREAT)
            shieldFixture.active = shielded
            damageableFixture.active = !shielded
        }
        return BodyComponentCreator.create(this, body)
    }


    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        sprite.setSize(4f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, TAG to sprite)
        spritesComponent.putUpdateFunction(TAG) { _, _sprite ->
            _sprite as GameSprite
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
            _sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (stateLoop.getCurrent()) {
                BospiderState.SPAWN -> "still"
                BospiderState.CLIMB -> "climb"
                BospiderState.OPEN_EYE -> "open_eye"
                BospiderState.CLOSE_EYE -> "close_eye"
                BospiderState.RETREAT -> "climb"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "still" to Animation(stillRegion!!),
            "climb" to Animation(climbRegion!!, 1, 5, 0.1f, true),
            "open_eye" to Animation(openEyeRegion!!, 1, 4, 0.1f, false),
            "close_eye" to Animation(openEyeRegion!!, 1, 4, 0.1f, false).reversed()
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}