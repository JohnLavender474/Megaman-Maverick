package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Facing
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
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
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class UpNDown(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable, IAnimatedEntity {

    companion object {
        const val TAG = "UpNDown"
        const val RED_TYPE = "red"
        const val BLUE_TYPE = "blue"
        private const val VEL_X = 4f
        private const val VEL_Y = 3f
        private const val DEVIATION = 2f
        private var redRegion: TextureRegion? = null
        private var blueRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10), Fireball::class to dmgNeg(ConstVals.MAX_HEALTH), ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        }, ChargedShotExplosion::class to dmgNeg(ConstVals.MAX_HEALTH)
    )

    override lateinit var facing: Facing

    private lateinit var type: String

    private var minX = 0f
    private var maxX = 0f
    private var up = false
    private var left = true

    override fun init() {
        if (redRegion == null || blueRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            redRegion = atlas.findRegion("UpNDown/Red")
            blueRegion = atlas.findRegion("UpNDown/Blue")
        }
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn = if (spawnProps.containsKey(ConstKeys.BOUNDS))
            spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        else spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        minX = spawn.x - (DEVIATION * ConstVals.PPM)
        maxX = spawn.x + (DEVIATION * ConstVals.PPM)

        up = spawnProps.getOrDefault(ConstKeys.UP, false, Boolean::class)
        left = spawnProps.getOrDefault(ConstKeys.LEFT, true, Boolean::class)

        type = spawnProps.getOrDefault(ConstKeys.TYPE, RED_TYPE, String::class)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            facing = if (megaman.body.x < body.x) Facing.LEFT else Facing.RIGHT
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM)

        val bodyFixture = Fixture(GameRectangle().set(body), FixtureType.BODY)
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(GameRectangle().set(body), FixtureType.DAMAGER)
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(GameRectangle().set(body), FixtureType.DAMAGEABLE)
        body.addFixture(damageableFixture)

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (left && body.x <= minX) left = false
            else if (!left && body.x >= maxX) left = true

            body.physics.velocity.x = VEL_X * ConstVals.PPM * if (left) -1 else 1
            body.physics.velocity.y = VEL_Y * ConstVals.PPM * if (up) 1 else -1
        }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = gdxArrayOf({ body }), debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(0.5f * ConstVals.PPM)

        val spritesComponent = SpritesComponent(this, "upNDown" to sprite)
        spritesComponent.putUpdateFunction("upNDown") { _, _sprite ->
            _sprite as GameSprite
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
            _sprite.setFlip(facing == Facing.LEFT, false)
        }

        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier = { type }
        val animations = objectMapOf<String, IAnimation>(
            RED_TYPE to Animation(redRegion!!, 1, 2, 0.1f, true),
            BLUE_TYPE to Animation(blueRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}