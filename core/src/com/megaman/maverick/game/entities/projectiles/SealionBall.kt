package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameCircle
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.ISpritesEntity
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.enemies.Sealion
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class SealionBall(game: MegamanMaverickGame) : AbstractHealthEntity(game), IProjectileEntity, ISpritesEntity,
    IDamageable {

    companion object {
        const val TAG = "SealionBall"
        private const val VELOCITY_Y = 10f
        private const val GRAVITY = -0.15f
        private var region: TextureRegion? = null
    }

    override val damageNegotiations = ObjectMap<KClass<out IDamager>, DamageNegotiation>()
    override var owner: IGameEntity? = null

    override fun init() {
        if (region == null) region = TODO()
        super.init()
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        owner = spawnProps.get(ConstKeys.PARENT, Sealion::class)!!
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        onDamageInflictedTo(damageable)
        (owner as Sealion).onBallDamagedInflicted()
    }

    fun throwBall() {
        firstSprite!!.hidden = false
        body.physics.velocity.y = VELOCITY_Y * ConstVals.PPM
        body.physics.gravityOn = true
    }

    fun catchBall() {
        firstSprite!!.hidden = true
        body.physics.velocity.setZero()
        body.physics.gravityOn = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (owner != null) {
            (owner as Sealion).onBallDestroyed()
            owner = null
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.85f * ConstVals.PPM)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameCircle().setRadius(0.425f * ConstVals.PPM))
        body.addFixture(projectileFixture)
        projectileFixture.rawShape.color = Color.RED
        debugShapes.add { projectileFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.425f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameCircle().setRadius(0.425f * ConstVals.PPM))
        body.addFixture(damageableFixture)

        TODO()
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.875f * ConstVals.PPM, 1.171875f * ConstVals.PPM)
        TODO()
    }
}