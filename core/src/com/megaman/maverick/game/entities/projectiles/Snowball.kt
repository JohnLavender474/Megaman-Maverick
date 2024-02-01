package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.GameLogger
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectSetOf
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.damage.IDamageable
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.defineProjectileComponents
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.getEntity
import kotlin.reflect.KClass

class Snowball(game: MegamanMaverickGame) : GameEntity(game), IProjectileEntity {

    companion object {
        const val TAG = "Snowball"
        private const val CLAMP = 10f
        private var region: TextureRegion? = null
    }

    override var owner: IGameEntity? = null

    // TODO: private var formFirst = false

    override fun init() {
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "Snowball")
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        defineProjectileComponents().forEach { addComponent(it) }
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        // TODO: formFirst = spawnProps.getOrDefault(ConstKeys.FORM, false, Boolean::class)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)

        body.physics.velocity = spawnProps.getOrDefault(ConstKeys.TRAJECTORY, Vector2(), Vector2::class)
        body.physics.gravityOn = spawnProps.getOrDefault(ConstKeys.GRAVITY_ON, false, Boolean::class)
        body.physics.gravity = spawnProps.getOrDefault(ConstKeys.GRAVITY, Vector2(), Vector2::class)
    }

    override fun hitBody(bodyFixture: Fixture) {
        if (bodyFixture.getEntity() !is AbstractEnemy && bodyFixture.getEntity() !is IProjectileEntity)
            explode("Hit body: $bodyFixture")
    }

    override fun hitBlock(blockFixture: Fixture) = explode("Hit block: $blockFixture")

    override fun hitShield(shieldFixture: Fixture) {
        if (shieldFixture.getEntity() != owner) explode("Hit shield: $shieldFixture")
    }

    override fun hitWater(waterFixture: Fixture) = explode("Hit water: $waterFixture")

    fun explode(causeOfDeath: String = "Explode") {
        GameLogger.debug(TAG, "Exploding: $this. Cause of death: $causeOfDeath")

        kill(props(CAUSE_OF_DEATH_MESSAGE to causeOfDeath))
        val explosion =
            EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.SNOWBALL_EXPLOSION)!!
        game.gameEngine.spawn(
            explosion,
            props(
                ConstKeys.POSITION to body.getCenter(),
                ConstKeys.MASK to
                        objectSetOf<KClass<out IDamageable>>(
                            if (owner is Megaman) AbstractEnemy::class else Megaman::class
                        )
            )
        )
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.15f * ConstVals.PPM)
        body.physics.velocityClamp.set(CLAMP * ConstVals.PPM.toFloat(), CLAMP * ConstVals.PPM.toFloat())

        // body fixture
        val bodyFixture = Fixture(GameRectangle(body), FixtureType.BODY)
        body.addFixture(bodyFixture)

        // projectile fixture
        val projectileFixture =
            Fixture(GameRectangle().setSize(0.2f * ConstVals.PPM), FixtureType.PROJECTILE)
        body.addFixture(projectileFixture)

        // damager fixture
        val damagerFixture = Fixture(GameRectangle().setSize(0.2f * ConstVals.PPM), FixtureType.DAMAGER)
        body.addFixture(damagerFixture)

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(0.85f * ConstVals.PPM)
        sprite.setRegion(region!!)

        val spritesComponent = SpritesComponent(this, "snowball" to sprite)
        spritesComponent.putUpdateFunction("snowball") { _, _sprite ->
            _sprite as GameSprite
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
        }

        return spritesComponent
    }
}
