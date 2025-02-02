package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.ICullable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.*
import com.mega.game.engine.motion.RotatingLine
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

class PetitDevil(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity, IParentEntity,
    IDrawableShapesEntity, IFaceable {

    companion object {
        const val TAG = "PetitDevil"
        private const val SPEED = 4f
        private const val CULL_TIME = 1f
        private val CHILDREN = gdxArrayOf(0f, 90f, 180f, 270f)
        private var orangeRegion: TextureRegion? = null
        private var greenRegion: TextureRegion? = null
    }

    override lateinit var facing: Facing
    override var children = Array<IGameEntity>()

    private lateinit var type: String

    override fun init() {
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

        CHILDREN.forEach { angle ->
            val child = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.PETIT_DEVIL_CHILD)!!
            child.spawn(
                props(
                    ConstKeys.PARENT pairTo this, ConstKeys.ANGLE pairTo angle, ConstKeys.TYPE pairTo type
                )
            )
        }

        facing = when (megaman.direction) {
            Direction.UP -> if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
            Direction.DOWN -> if (megaman.body.getX() > body.getX()) Facing.LEFT else Facing.RIGHT
            Direction.LEFT -> if (megaman.body.getY() < body.getY()) Facing.LEFT else Facing.RIGHT
            Direction.RIGHT -> if (megaman.body.getY() > body.getY()) Facing.LEFT else Facing.RIGHT
        }

        val trajectory = GameObjectPools.fetch(Vector2::class)
            .set(megaman.body.getCenter())
            .sub(body.getCenter())
            .nor()
            .scl(SPEED * ConstVals.PPM)
        body.physics.velocity.set(trajectory)

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
            if (isHealthDepleted()) (it as PetitDevilChild).disintegrateAndDie()
            else (it as GameEntity).destroy()
        }
        children.clear()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            val iter = children.iterator()
            while (iter.hasNext()) {
                val child = iter.next() as MegaGameEntity
                if (child.dead) iter.remove()
            }

            facing = when (megaman.direction) {
                Direction.UP -> if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
                Direction.DOWN -> if (megaman.body.getX() > body.getX()) Facing.LEFT else Facing.RIGHT
                Direction.LEFT -> if (megaman.body.getY() < body.getY()) Facing.LEFT else Facing.RIGHT
                Direction.RIGHT -> if (megaman.body.getY() > body.getY()) Facing.LEFT else Facing.RIGHT
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
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            sprite.setCenter(body.getCenter())
            sprite.setOriginCenter()
            val direction = megaman.direction
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.rotation = direction.rotation
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (type == ConstKeys.GREEN) "LargeGreen" else "LargeOrange" }
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

    override fun init() {
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

        facing = when (megaman.direction) {
            Direction.UP -> if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
            Direction.DOWN -> if (megaman.body.getX() > body.getX()) Facing.LEFT else Facing.RIGHT
            Direction.LEFT -> if (megaman.body.getY() < body.getY()) Facing.LEFT else Facing.RIGHT
            Direction.RIGHT -> if (megaman.body.getY() > body.getY()) Facing.LEFT else Facing.RIGHT
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
            if (parent == null || (parent as MegaGameEntity).dead) {
                destroy()
                return@add
            }

            val origin = (parent as IBodyEntity).body.getCenter()
            rotatingLine.setOrigin(origin)
            rotatingLine.update(delta)

            scalar += OUT_SPEED * delta
            body.setCenter(rotatingLine.getScaledPosition(scalar, GameObjectPools.fetch(Vector2::class)))

            facing = when (megaman.direction) {
                Direction.UP -> if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
                Direction.DOWN -> if (megaman.body.getX() > body.getX()) Facing.LEFT else Facing.RIGHT
                Direction.LEFT -> if (megaman.body.getY() < body.getY()) Facing.LEFT else Facing.RIGHT
                Direction.RIGHT -> if (megaman.body.getY() > body.getY()) Facing.LEFT else Facing.RIGHT
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
        sprite.setSize(ConstVals.PPM.toFloat())
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setOriginCenter()
            sprite.rotation = megaman.direction.rotation
            sprite.hidden = damageBlink
            sprite.setCenter(body.getCenter())
            sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (type == ConstKeys.GREEN) "SmallGreen" else "SmallOrange" }
        val animations = objectMapOf<String, IAnimation>(
            "SmallGreen" pairTo Animation(greenRegion!!, 1, 4, gdxArrayOf(1f, 0.1f, 0.1f, 0.1f), true),
            "SmallOrange" pairTo Animation(orangeRegion!!, 1, 4, gdxArrayOf(1f, 0.1f, 0.1f, 0.1f), true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
