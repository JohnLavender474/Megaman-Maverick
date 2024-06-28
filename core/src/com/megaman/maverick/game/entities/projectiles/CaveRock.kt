package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.GameLogger
import com.engine.common.extensions.getTextureRegion
import com.engine.common.interfaces.Updatable
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamageable
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setCenter
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.engine.world.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.defineProjectileComponents
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class CaveRock(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        const val TAG = "CaveRock"
        private var rockRegion: TextureRegion? = null
        private const val STANDARD_GRAVITY = -0.5f
    }

    override var owner: IGameEntity? = null

    var trajectory: Vector2? = null
    var passThroughBlocks = false
    var gravity = STANDARD_GRAVITY * ConstVals.PPM

    override fun init() {
        if (rockRegion == null)
            rockRegion =
                game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "CaveRock/Rock")
        addComponents(defineProjectileComponents())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "Spawn with props = $spawnProps")
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)
        passThroughBlocks = spawnProps.getOrDefault(ConstKeys.PASS_THROUGH, false, Boolean::class)

        if (spawnProps.containsKey(ConstKeys.IMPULSE)) {
            val impulse = spawnProps.get(ConstKeys.IMPULSE, Vector2::class)!!
            body.physics.velocity = impulse
        }

        trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)
        gravity =
            spawnProps.getOrDefault(ConstKeys.GRAVITY, STANDARD_GRAVITY * ConstVals.PPM, Float::class)
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun hitBlock(blockFixture: IFixture) {
        if (passThroughBlocks) return
        explodeAndDie()
    }

    override fun hitWater(waterFixture: IFixture) {
        // set x vel to zero and sink slowly
    }

    override fun hitShield(shieldFixture: IFixture) {
        // bounce
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.75f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val projectileFixture =
            Fixture(body, FixtureType.PROJECTILE, GameRectangle().setSize(0.75f * ConstVals.PPM))
        body.addFixture(projectileFixture)
        projectileFixture.rawShape.color = Color.YELLOW
        debugShapes.add { projectileFixture.getShape() }

        val damagerFixture =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.65f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        damagerFixture.getShape().color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val shieldFixture = Fixture(body, FixtureType.SHIELD, GameRectangle().setSize(0.75f * ConstVals.PPM))
        body.addFixture(shieldFixture)
        shieldFixture.getShape().color = Color.BLUE
        debugShapes.add { shieldFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT, Updatable {
            body.physics.gravity.y = gravity
            trajectory?.let { body.physics.velocity = it }
        })

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2.5f * ConstVals.PPM)
        sprite.setRegion(rockRegion!!)
        val spritesComponent = SpritesComponent(this, sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    override fun explodeAndDie(vararg params: Any) {
        GameLogger.debug(TAG, "burst()")
        kill(props(CAUSE_OF_DEATH_MESSAGE to "Burst"))

        val caveRockExplosion =
            EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.CAVE_ROCK_EXPLOSION)!!
        game.engine.spawn(caveRockExplosion, props(ConstKeys.POSITION to body.getCenter()))
    }
}
