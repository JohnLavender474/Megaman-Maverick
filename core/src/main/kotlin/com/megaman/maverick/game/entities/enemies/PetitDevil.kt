package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.cullables.ICullable
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.GameEntity
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.*
import com.maverick.game.engine.entities.direction
import com.maverick.game.engine.motion.RotatingLine
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.contracts.targetPlayer
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds

class PetitDevil(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity,
    IParentEntity<PetitDevilChild>, IDrawableShapesEntity, IFaceable {

    companion object {
        const val TAG = "PetitDevil"
        private const val SPEED = 4f
        private const val CULL_TIME = 1f
        private val CHILDREN = gdxArrayOf(0f, 90f, 180f, 270f)
        private var orangeRegion: TextureRegion? = null
        private var greenRegion: TextureRegion? = null
    }

    override lateinit var facing: Facing
    override var children = Array<PetitDevilChild>()

    private lateinit var type: String

    private val startDelay = Timer()
    private var started = false

    override fun init(vararg params: Any) {
        if (orangeRegion == null || greenRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            orangeRegion = atlas.findRegion("PetitDevil/LargeOrange")
            greenRegion = atlas.findRegion("PetitDevil/LargeGreen")
        }

        addComponent(DrawableShapesComponent())

        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        putProperty(ConstKeys.ENTITY_KILLED_BY_DEATH_FIXTURE, false)
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        type = spawnProps.getOrDefault(ConstKeys.TYPE, ConstKeys.GREEN, String::class)

        facing = when (targetPlayer.direction) {
            Direction.UP -> if (targetPlayer.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
            Direction.DOWN -> if (targetPlayer.body.getX() > body.getX()) Facing.LEFT else Facing.RIGHT
            Direction.LEFT -> if (targetPlayer.body.getY() < body.getY()) Facing.LEFT else Facing.RIGHT
            Direction.RIGHT -> if (targetPlayer.body.getY() > body.getY()) Facing.LEFT else Facing.RIGHT
        }

        val delay = spawnProps.getOrDefault(ConstKeys.DELAY, 0f, Float::class)
        startDelay.resetDuration(delay)
        started = false
        body.physics.velocity.setZero()
        if (delay <= 0f) start()

        putCullable(ConstKeys.CULL_OUT_OF_BOUNDS, object : ICullable {

            private var time = 0f

            override fun shouldBeCulled(delta: Float): Boolean {
                if (overlapsGameCamera()) {
                    time = 0f
                    return false
                }
                for (child in children) {
                    if ((child as PetitDevilChild).overlapsGameCamera()) {
                        time = 0f
                        return false
                    }
                }
                time += delta
                return time >= CULL_TIME
            }

            override fun reset() {
                super.reset()
                time = 0f
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isHealthDepleted()) explode(
            props(
                ConstKeys.POSITION pairTo body.getCenter(), ConstKeys.SOUND pairTo SoundAsset.EXPLOSION_2_SOUND
            )
        )
        children.forEach {
            if (isHealthDepleted()) it.disintegrateAndDie()
            else it.destroy()
        }
        children.clear()
    }

    private fun start() {
        started = true

        CHILDREN.forEach { angle ->
            val child = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.PETIT_DEVIL_CHILD)!!
            child.spawn(
                props(
                    ConstKeys.PARENT pairTo this, ConstKeys.ANGLE pairTo angle, ConstKeys.TYPE pairTo type
                )
            )
        }

        val trajectory = GameObjectPools.fetch(Vector2::class).set(targetPlayer.body.getCenter()).sub(body.getCenter()).nor()
            .scl(SPEED * ConstVals.PPM)
        body.physics.velocity.set(trajectory)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!started) {
                startDelay.update(delta)
                if (startDelay.isFinished()) start()
            }

            val iter = children.iterator()
            while (iter.hasNext()) {
                val child = iter.next()
                if (!child.existenceState.liveAfterUpdate) iter.remove()
            }

            facing = when (targetPlayer.direction) {
                Direction.UP -> if (targetPlayer.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
                Direction.DOWN -> if (targetPlayer.body.getX() > body.getX()) Facing.LEFT else Facing.RIGHT
                Direction.LEFT -> if (targetPlayer.body.getY() < body.getY()) Facing.LEFT else Facing.RIGHT
                Direction.RIGHT -> if (targetPlayer.body.getY() > body.getY()) Facing.LEFT else Facing.RIGHT
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        addDebugShapeSupplier { body.getBounds() }
        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putPreProcess { _, _ ->
            sprite.hidden = damageBlink
            sprite.setCenter(body.getCenter())
            sprite.setOriginCenter()
            val direction = targetPlayer.direction
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.rotation = direction.rotation
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { if (type == ConstKeys.GREEN) "LargeGreen" else "LargeOrange" }
        val animations = objectMapOf<String, IAnimation>(
            "LargeGreen" pairTo Animation(greenRegion!!, 1, 4, gdxArrayOf(1f, 0.1f, 0.1f, 0.1f), true),
            "LargeOrange" pairTo Animation(orangeRegion!!, 1, 4, gdxArrayOf(1f, 0.1f, 0.1f, 0.1f), true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}

class PetitDevilChild(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity,
    IDrawableShapesEntity, IChildEntity, IFaceable {

    companion object {
        const val TAG = "PetitDevilChild"
        private const val SPINNING_SPEED = 3f
        private const val OUT_SPEED = 0.1f
        private const val START_SCALAR = 0.5f
        private var greenRegion: TextureRegion? = null
        private var orangeRegion: TextureRegion? = null
    }

    override var parent: IGameEntity? = null
    override lateinit var facing: Facing

    private lateinit var rotatingLine: RotatingLine
    private lateinit var type: String
    private var scalar = START_SCALAR

    override fun init(vararg params: Any) {
        if (greenRegion == null || orangeRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            greenRegion = atlas.findRegion("PetitDevil/SmallGreen")
            orangeRegion = atlas.findRegion("PetitDevil/SmallOrange")
        }

        addComponent(DrawableShapesComponent())

        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        putProperty(ConstKeys.ENTITY_KILLED_BY_DEATH_FIXTURE, false)
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.onSpawn(spawnProps)

        parent = spawnProps.get(ConstKeys.PARENT, GameEntity::class)!!

        val origin = (parent as IBodyEntity).body.getCenter()
        val angle = spawnProps.get(ConstKeys.ANGLE, Float::class)!!
        rotatingLine = RotatingLine(origin, ConstVals.PPM.toFloat(), SPINNING_SPEED * ConstVals.PPM, angle)

        type = spawnProps.get(ConstKeys.TYPE, String::class)!!
        scalar = START_SCALAR

        facing = when (targetPlayer.direction) {
            Direction.UP -> if (targetPlayer.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
            Direction.DOWN -> if (targetPlayer.body.getX() > body.getX()) Facing.LEFT else Facing.RIGHT
            Direction.LEFT -> if (targetPlayer.body.getY() < body.getY()) Facing.LEFT else Facing.RIGHT
            Direction.RIGHT -> if (targetPlayer.body.getY() > body.getY()) Facing.LEFT else Facing.RIGHT
        }

        putCullable(ConstKeys.CULL_OUT_OF_BOUNDS, object : ICullable {

            override fun shouldBeCulled(delta: Float): Boolean {
                if ((parent as PetitDevil).overlapsGameCamera()) return false
                return !overlapsGameCamera()
            }
        })
    }

    internal fun disintegrateAndDie() {
        disintegrate()
        destroy()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (parent == null || !parent!!.existenceState.liveAfterUpdate) {
                destroy()
                return@add
            }

            val origin = (parent as IBodyEntity).body.getCenter()
            rotatingLine.setOrigin(origin)
            rotatingLine.update(delta)

            scalar += OUT_SPEED * delta
            body.setCenter(rotatingLine.getScaledPosition(scalar, GameObjectPools.fetch(Vector2::class)))

            facing = when (targetPlayer.direction) {
                Direction.UP -> if (targetPlayer.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
                Direction.DOWN -> if (targetPlayer.body.getX() > body.getX()) Facing.LEFT else Facing.RIGHT
                Direction.LEFT -> if (targetPlayer.body.getY() < body.getY()) Facing.LEFT else Facing.RIGHT
                Direction.RIGHT -> if (targetPlayer.body.getY() > body.getY()) Facing.LEFT else Facing.RIGHT
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        addDebugShapeSupplier { body.getBounds() }
        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putPreProcess { _, _ ->
            sprite.setOriginCenter()
            sprite.rotation = targetPlayer.direction.rotation
            sprite.hidden = damageBlink
            sprite.setCenter(body.getCenter())
            sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { if (type == ConstKeys.GREEN) "SmallGreen" else "SmallOrange" }
        val animations = objectMapOf<String, IAnimation>(
            "SmallGreen" pairTo Animation(greenRegion!!, 1, 4, gdxArrayOf(1f, 0.1f, 0.1f, 0.1f), true),
            "SmallOrange" pairTo Animation(orangeRegion!!, 1, 4, gdxArrayOf(1f, 0.1f, 0.1f, 0.1f), true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
