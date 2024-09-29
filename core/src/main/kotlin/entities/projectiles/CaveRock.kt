package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType

class CaveRock(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        const val TAG = "CaveRock"
        private var rockRegion: TextureRegion? = null
        private const val STANDARD_GRAVITY = -0.5f
    }

    var trajectory: Vector2? = null
    var passThroughBlocks = false
    var gravity = STANDARD_GRAVITY * ConstVals.PPM

    override fun init() {
        if (rockRegion == null)
            rockRegion = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, "CaveRock/Rock")
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
        gravity =
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
        body.physics.takeFrictionFromOthers = false
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

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2.5f * ConstVals.PPM)
        sprite.setRegion(rockRegion!!)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setCenter(body.getCenter())
        }
        return spritesComponent
    }

    override fun explodeAndDie(vararg params: Any?) {
        GameLogger.debug(TAG, "burst()")
        destroy()
        val caveRockExplosion =
            EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.CAVE_ROCK_EXPLOSION)!!
        caveRockExplosion.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
    }
}
