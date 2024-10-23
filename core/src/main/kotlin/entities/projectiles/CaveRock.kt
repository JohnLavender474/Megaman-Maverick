package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType

class CaveRock(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        const val TAG = "CaveRock"
        private const val STANDARD_GRAVITY = -0.1f
        private var rockRegion: TextureRegion? = null
    }

    var trajectory: Vector2? = null
    var passThroughBlocks = false

    override fun init() {
        if (rockRegion == null)
            rockRegion = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "$TAG/Rock")
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "Spawn with props = $spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)
        passThroughBlocks = spawnProps.getOrDefault(ConstKeys.PASS_THROUGH, false, Boolean::class)

        if (spawnProps.containsKey(ConstKeys.IMPULSE)) {
            val impulse = spawnProps.get(ConstKeys.IMPULSE, Vector2::class)!!
            body.physics.velocity = impulse
        }

        trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)
        body.physics.gravity.y =
            spawnProps.getOrDefault(ConstKeys.GRAVITY, STANDARD_GRAVITY * ConstVals.PPM, Float::class)
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun hitBlock(blockFixture: IFixture) {
        if (passThroughBlocks) return
        explodeAndDie()
    }

    override fun hitWater(waterFixture: IFixture) {
        // set x vel pairTo zero and sink slowly
    }

    override fun hitShield(shieldFixture: IFixture) {
        // bounce
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(0.75f * ConstVals.PPM)
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body }), debug = true))
        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.PROJECTILE, FixtureType.DAMAGER, FixtureType.SHIELD)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2.75f * ConstVals.PPM)
        sprite.setRegion(rockRegion!!)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite -> _sprite.setCenter(body.getCenter()) }
        return spritesComponent
    }

    override fun explodeAndDie(vararg params: Any?) {
        GameLogger.debug(TAG, "explodeAndDie()")
        destroy()
        val caveRockExplosion =
            EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.CAVE_ROCK_EXPLOSION)!!
        caveRockExplosion.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
    }
}
