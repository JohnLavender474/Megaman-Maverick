package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Facing
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.isFacing
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.cullables.ICullable
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.*
import com.engine.motion.RotatingLine
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
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class PetitDevil(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IParentEntity,
    IDrawableShapesEntity, IFaceable {

    companion object {
        const val TAG = "PetitDevil"
        private const val SPEED = 4f
        private val CHILDREN = gdxArrayOf(0f, 90f, 180f, 270f)
        private var orangeRegion: TextureRegion? = null
        private var greenRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        }, ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 15 else 10
        }
    )
    override lateinit var facing: Facing
    override var children = Array<IGameEntity>()

    private lateinit var type: String

    override fun init() {
        if (orangeRegion == null || greenRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            orangeRegion = atlas.findRegion("PetitDevil/LargeOrange")
            greenRegion = atlas.findRegion("PetitDevil/LargeGreen")
        }
        addComponent(DrawableShapesComponent(this))
        isDebugShapes = true
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.DEATH_FIXTURE, false)
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)

        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        type = spawnProps.getOrDefault(ConstKeys.TYPE, ConstKeys.GREEN, String::class)

        CHILDREN.forEach { angle ->
            val child = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.PETIT_DEVIL_CHILD)!!
            game.engine.spawn(
                child, props(
                    ConstKeys.PARENT to this, ConstKeys.ANGLE to angle, ConstKeys.TYPE to type
                )
            )
        }

        facing = if (megaman.body.x < body.x) Facing.LEFT else Facing.RIGHT

        val trajectory = megaman.body.getCenter().sub(body.getCenter()).nor().scl(SPEED * ConstVals.PPM)
        body.physics.velocity = trajectory

        putCullable(ConstKeys.CULL_OUT_OF_BOUNDS, object : ICullable {
            override fun shouldBeCulled(delta: Float): Boolean {
                if (isInGameCamBounds()) return false
                for (child in children) if ((child as AbstractEnemy).isInGameCamBounds()) return false
                return true
            }
        })
    }

    override fun onDestroy() {
        super<AbstractEnemy>.onDestroy()
        if (hasDepletedHealth()) explode(
            props(
                ConstKeys.POSITION to body.getCenter(), ConstKeys.SOUND to SoundAsset.EXPLOSION_2_SOUND
            )
        )
        children.forEach {
            if (hasDepletedHealth()) (it as PetitDevilChild).disintegrateAndDie()
            else it.kill()
        }
        children.clear()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            val iter = children.iterator()
            while (iter.hasNext()) {
                val child = iter.next()
                if (child.dead) iter.remove()
            }
            facing = if (megaman.body.x < body.x) Facing.LEFT else Facing.RIGHT
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        addDebugShapeSupplier { body }
        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)
        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)
        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)
        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            _sprite.setCenter(body.getCenter())
            _sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (type == ConstKeys.GREEN) "LargeGreen" else "LargeOrange" }
        val animations = objectMapOf<String, IAnimation>(
            "LargeGreen" to Animation(greenRegion!!, 1, 4, gdxArrayOf(1f, 0.1f, 0.1f, 0.1f), true),
            "LargeOrange" to Animation(orangeRegion!!, 1, 4, gdxArrayOf(1f, 0.1f, 0.1f, 0.1f), true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}

class PetitDevilChild(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IDrawableShapesEntity,
    IChildEntity, IFaceable {

    companion object {
        const val TAG = "PetitDevilChild"
        private const val SPINNING_SPEED = 3f
        private const val OUT_SPEED = 0.1f
        private const val START_SCALAR = 0.5f
        private var greenRegion: TextureRegion? = null
        private var orangeRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10),
        Fireball::class to dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 10 else 5
        }
    )
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
        addComponent(DrawableShapesComponent(this))
        addDebugShapeSupplier { rotatingLine.line }
        isDebugShapes = true
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.spawn(spawnProps)
        parent = spawnProps.get(ConstKeys.PARENT, IGameEntity::class)!!
        val origin = (parent as IBodyEntity).body.getCenter()
        val angle = spawnProps.get(ConstKeys.ANGLE, Float::class)!!
        rotatingLine = RotatingLine(origin, ConstVals.PPM.toFloat(), SPINNING_SPEED * ConstVals.PPM, angle)
        type = spawnProps.get(ConstKeys.TYPE, String::class)!!
        scalar = START_SCALAR
    }

    internal fun disintegrateAndDie() {
        disintegrate()
        kill()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (parent == null || parent?.dead == true) {
                kill()
                return@add
            }
            val origin = (parent as IBodyEntity).body.getCenter()
            rotatingLine.setOrigin(origin)
            rotatingLine.update(delta)
            scalar += OUT_SPEED * delta
            body.setCenter(rotatingLine.getScaledPosition(scalar))
            facing = if (megaman.body.x < body.x) Facing.LEFT else Facing.RIGHT
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM)

        addDebugShapeSupplier { body }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(ConstVals.PPM.toFloat())
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.hidden = damageBlink
            _sprite.setCenter(body.getCenter())
            _sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (type == ConstKeys.GREEN) "SmallGreen" else "SmallOrange" }
        val animations = objectMapOf<String, IAnimation>(
            "SmallGreen" to Animation(greenRegion!!, 1, 4, gdxArrayOf(1f, 0.1f, 0.1f, 0.1f), true),
            "SmallOrange" to Animation(orangeRegion!!, 1, 4, gdxArrayOf(1f, 0.1f, 0.1f, 0.1f), true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}