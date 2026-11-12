package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.entities.utils.TickFrames
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.decorations.D1000BombMarker
import com.megaman.maverick.game.entities.hazards.D1000Pillar
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import com.megaman.maverick.game.entities.bosses.d1000.D1000Frames

class D1000Bomb(services: MegaServices) : AbstractProjectile(services, size = Size.MEDIUM), IAnimatedEntity {

    companion object {
        const val TAG = "D1000Bomb"
        private const val SIZE = 0.5f * ConstVals.PPM
        private const val GRAVITY = 28f * ConstVals.PPM

        private const val WALL_SPEED = 14f * ConstVals.PPM
        private const val WALL_MIN_FLIGHT = 0.3f

        fun flightTime(spawnY: Float, floorY: Float) = sqrt(2f * max(spawnY - SIZE / 2f - floorY, SIZE) / GRAVITY)

        fun wallFlightTime(spawnX: Float, wallX: Float) = max(abs(wallX - spawnX) / WALL_SPEED, WALL_MIN_FLIGHT)
    }

    private val animations = ObjectMap<String, IAnimation>()
    private val arena = GameRectangle()
    private var marker: D1000BombMarker? = null

    var targetX = 0f
        private set
    var targetY = 0f
        private set
    var horizontal = false
        private set

    fun landsIn(): Float {
        if (horizontal) {
            val vx = body.physics.velocity.x
            return if (vx == 0f) 0f else max((targetX - body.getCenter().x) / vx, 0f)
        }
        val height = body.getY() - arena.getY()
        if (height <= 0f) return 0f
        val down = -body.physics.velocity.y
        return (-down + sqrt(down * down + 2f * GRAVITY * height)) / GRAVITY
    }

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        val atlas = services.assets.getTextureAtlas(TextureAsset.BOSSES_3.source)
        TickFrames.animations(atlas, TAG, D1000Frames.BOMB, animations)
        super.init()
        addComponent(AnimationsComponent(this, Animator(animations["bomb"])))
        addComponent(UpdatablesComponent({ land() }))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        val target = spawnProps.get(ConstKeys.TARGET, Float::class)!!
        targetX = target
        arena.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)
        body.setCenter(spawn)
        val wallY = spawnProps.get(ConstKeys.Y, Float::class)
        horizontal = wallY != null
        targetY = wallY ?: arena.getY()
        if (horizontal) {
            val time = wallFlightTime(spawn.x, target)
            body.physics.velocity.set((target - spawn.x) / time, (targetY - spawn.y + GRAVITY * time * time / 2f) / time)
        } else {
            val drop = max(spawn.y - SIZE / 2f - arena.getY(), SIZE)
            body.physics.velocity.set((target - spawn.x) / sqrt(2f * drop / GRAVITY), 0f)
        }
        body.physics.gravity.set(0f, -GRAVITY)
        body.physics.gravityOn = true
        val rotation = if (!horizontal) 0f else if (target < (arena.getX() + arena.getWidth() / 2f)) -90f else 90f
        val given = spawnProps.get(ConstKeys.CHILD, D1000BombMarker::class)
        if (given != null && given.existenceState.liveAfterUpdate) {
            given.position.set(target, targetY)
            given.rotation = rotation
            marker = given
        } else {
            val spawned = MegaEntityFactory.fetch(D1000BombMarker::class)!!
            spawned.spawn(props(ConstKeys.POSITION pairTo Vector2(target, targetY), ConstKeys.ROTATION pairTo rotation))
            marker = spawned
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeMarker()
    }

    private fun removeMarker() {
        marker?.let { if (it.existenceState.liveAfterUpdate) it.destroy() }
        marker = null
    }

    private fun land() {
        if (horizontal) {
            landOnWall()
            return
        }
        val center = body.getCenter()
        if (center.x < arena.getX() || center.x > arena.getMaxX()) {
            body.physics.velocity.x = 0f
            body.setCenter(center.x.coerceIn(arena.getX(), arena.getMaxX()), center.y)
        }
        if (body.getY() > arena.getY()) return

        val pillar = MegaEntityFactory.fetch(D1000Pillar::class)!!
        pillar.spawn(
            props(
                ConstKeys.POSITION pairTo Vector2(body.getCenter().x, arena.getY()),
                ConstKeys.MAX_Y pairTo arena.getMaxY()
            )
        )
        requestToPlaySound(SoundAsset.BLAST_1_SOUND, false)
        removeMarker()
        destroy()
    }

    private fun landOnWall() {
        val x = body.getCenter().x
        val left = targetX < (arena.getX() + arena.getWidth() / 2f)
        if (if (left) x > targetX else x < targetX) return
        val pillar = MegaEntityFactory.fetch(D1000Pillar::class)!!
        pillar.spawn(
            props(
                ConstKeys.POSITION pairTo Vector2(targetX, targetY),
                ConstKeys.VERTICAL pairTo false,
                ConstKeys.TARGET pairTo if (left) arena.getMaxX() else arena.getX()
            )
        )
        requestToPlaySound(SoundAsset.BLAST_1_SOUND, false)
        removeMarker()
        destroy()
    }

    override fun explodeAndDie(vararg params: Any?) {
        destroy()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(SIZE)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))
        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val center = body.getCenter()
            TickFrames.place(sprite, D1000Frames.BOMB["bomb"], center.x, center.y, false)
        }
        return component
    }
}
