package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.Vector2
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.CAUSE_OF_DEATH_MESSAGE
import com.engine.common.enums.Position
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.mask
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamageable
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.defineProjectileComponents
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.getEntity

class Fireball(game: MegamanMaverickGame) : GameEntity(game), IProjectileEntity {

    companion object {
        private var fireballAtlas: TextureAtlas? = null
        private var flameAtlas: TextureAtlas? = null
        private const val ROTATION = 1000f
        private const val GRAVITY = -.25f
        private const val CULL_DUR = 1f
        private const val Y_BOUNCE = 7.5f
        private const val X_VEL = 10f
    }

    override var owner: IGameEntity? = null

    private val cullTimer = Timer(CULL_DUR)

    private var xVel = X_VEL
    private var burst = false

    override fun init() {
        if (fireballAtlas == null)
            fireballAtlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
        if (flameAtlas == null) flameAtlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
        defineProjectileComponents().forEach { addComponent(it) }
        addComponent(defineBodyComponent())
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        burst = false
        cullTimer.reset()
        body.physics.gravityOn = true
        xVel = X_VEL * ConstVals.PPM
        if (spawnProps.containsKey(ConstKeys.LEFT) && spawnProps.get(ConstKeys.LEFT, Boolean::class)!!)
            xVel *= -1f
        body.physics.velocity.x = xVel
        body.physics.velocity.y = Y_BOUNCE * ConstVals.PPM
    }

    override fun canDamage(damageable: IDamageable): Boolean {
        TODO("Not yet implemented")
    }

    override fun onDamageInflictedTo(damageable: IDamageable) {
        super.onDamageInflictedTo(damageable)
        explodeAndDie()
    }

    override fun hitBody(bodyFixture: Fixture) {
        super.hitBody(bodyFixture)
        if (mask(owner, bodyFixture.getEntity(), { it is Megaman }, { it is AbstractEnemy })) explodeAndDie()
    }

    override fun hitBlock(blockFixture: Fixture) {
        super.hitBlock(blockFixture)
        explodeAndDie()
    }

    override fun hitShield(shieldFixture: Fixture) {
        super.hitShield(shieldFixture)
        xVel *= -1f
        requestToPlaySound(SoundAsset.DINK_SOUND, false)
    }

    override fun hitWater(waterFixture: Fixture) {
        super.hitWater(waterFixture)
        kill(props(CAUSE_OF_DEATH_MESSAGE to "Hit water"))
        val smokePuff = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.SMOKE_PUFF)!!
        val waterBounds = waterFixture.shape.getBoundingRectangle()
        val spawn = Vector2(body.getCenter().x, waterBounds.getMaxY())
        game.gameEngine.spawn(smokePuff, props(ConstKeys.POSITION to spawn))
        getMegamanMaverickGame().audioMan.playSound(SoundAsset.WHOOSH_SOUND, false)
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        body.setSize(0.9f * ConstVals.PPM)

        // projectile fixture
        val projectileFixture =
            Fixture(GameRectangle().setSize(0.9f * ConstVals.PPM), FixtureType.PROJECTILE)
        body.addFixture(projectileFixture)

        // damager fixture
        val damagerFixture = Fixture(GameRectangle().setSize(0.9f * ConstVals.PPM), FixtureType.DAMAGER)
        body.addFixture(damagerFixture)

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM)
        val SpritesComponent = SpritesComponent(this, "fireball" to sprite)
        SpritesComponent.putUpdateFunction("fireball") { delta, _sprite ->
            _sprite as GameSprite
            val position = body.getBottomCenterPoint()
            _sprite.setPosition(position, Position.BOTTOM_CENTER)
            sprite.rotation = if (burst) 0f else ROTATION * delta
        }
        return SpritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = { if (burst) "burst" else "fireball" }
        val animations =
            objectMapOf<String, IAnimation>(
                "burst" to Animation(flameAtlas!!.findRegion("Flame"), 1, 4, 0.1f, true),
                "fireball" to Animation(fireballAtlas!!.findRegion("Fire/Fireball"))
            )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun defineUpdatablesComponent(): UpdatablesComponent {
        val updatablesComponent = UpdatablesComponent(this)
        updatablesComponent.add {
            if (burst) {
                body.physics.velocity.x = 0f
                cullTimer.update(it)
            } else body.physics.velocity.x = xVel
            if (cullTimer.isFinished()) kill(props(CAUSE_OF_DEATH_MESSAGE to "Cull timer finished"))
        }
        return updatablesComponent
    }

    override fun explodeAndDie() {
        burst = true
        requestToPlaySound(SoundAsset.ATOMIC_FIRE_SOUND, false)
    }
}
