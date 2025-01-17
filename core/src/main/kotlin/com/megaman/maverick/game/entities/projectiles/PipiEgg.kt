package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.DecorationsFactory
import com.megaman.maverick.game.entities.factories.impl.EnemiesFactory
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

class PipiEgg(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        const val TAG = "PipiEgg"
        private const val GRAVITY = -0.15f
        private const val BABY_BIRDIE_SPAWN_MAX_OFFSET = 0.25f
        private const val BABY_BIRDIE_MIN_ANGLE = 250f
        private const val BABY_BIRDIE_MAX_ANGLE = 290f
        private const val BABY_BIRDIES_TO_SPAWN = 6
        private const val BABY_BIRDIE_SPEED = 6f
        private val eggShatterImpulses = Array<Vector2>()
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            regions.put("egg", atlas.findRegion("Pipi/Egg"))

            // TODO: shatter region never used
            regions.put("shatter", atlas.findRegion("Pipi/EggShatter"))
        }
        if (eggShatterImpulses.isEmpty) {
            eggShatterImpulses.addAll(
                Vector2(-5f, 5f), Vector2(-2.5f, 2.5f), Vector2(2.5f, 2.5f), Vector2(5f, 5f)
            )
        }
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        body.physics.gravityOn = true
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        if (overlapsGameCamera()) playSoundNow(SoundAsset.THUMP_SOUND, false)
        spawnBabyBirdies()
        spawnEggShatters()
        destroy()
    }

    private fun spawnBabyBirdies() {
        for (i in 0 until BABY_BIRDIES_TO_SPAWN) {
            val random = GameObjectPools.fetch(Vector2::class)
            val randomValue = getRandom(-BABY_BIRDIE_SPAWN_MAX_OFFSET, BABY_BIRDIE_SPAWN_MAX_OFFSET)
            random.set(randomValue, randomValue)

            val randomSpawnPosition =
                Vector2(body.getCenter()).add(random.scl(ConstVals.PPM.toFloat()))

            val randomAngle = getRandom(BABY_BIRDIE_MIN_ANGLE, BABY_BIRDIE_MAX_ANGLE)
            val trajectory = Vector2(0f, BABY_BIRDIE_SPEED * ConstVals.PPM).rotateDeg(randomAngle)

            if (megaman.body.getX() < body.getMaxX()) trajectory.x *= -1f

            val babyBirdie = EntityFactories.fetch(EntityType.ENEMY, EnemiesFactory.COPIPI)!!
            babyBirdie.spawn(
                props(
                    ConstKeys.POSITION pairTo randomSpawnPosition,
                    ConstKeys.TRAJECTORY pairTo trajectory
                )
            )
        }
    }

    private fun spawnEggShatters() {
        for (i in 0 until eggShatterImpulses.size) {
            val eggShatter = EntityFactories.fetch(EntityType.DECORATION, DecorationsFactory.PIPI_EGG_SHATTER)!!
            eggShatter.spawn(
                props(
                    ConstKeys.POSITION pairTo body.getCenter(),
                    ConstKeys.IMPULSE pairTo eggShatterImpulses[i].cpy().scl(ConstVals.PPM.toFloat()),
                    ConstKeys.TYPE pairTo i
                )
            )
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.75f * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameCircle().setRadius(0.375f * ConstVals.PPM))
        body.addFixture(projectileFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.375f * ConstVals.PPM))
        body.addFixture(damagerFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(regions.get("egg"))
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ -> sprite.setCenter(body.getCenter()) }
        return spritesComponent
    }
}

