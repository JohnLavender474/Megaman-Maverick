package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.explosions.Disintegration
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.world.body.*

class Rock(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        const val TAG = "Rock"

        private const val BIG_SIZE = 0.5f
        private const val SMALL_SIZE = 0.25f

        private const val GRAVITY = -0.15f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class RockSize { BIG, SMALL }

    private lateinit var rockSize: RockSize

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            val keys = RockSize.entries.map { it.name.lowercase() }
            AnimationUtils.loadRegions(TAG, atlas, keys, regions)
        }
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        rockSize = spawnProps.get(ConstKeys.SIZE, RockSize::class)!!

        val bodySize = if (rockSize == RockSize.SMALL) SMALL_SIZE else BIG_SIZE
        body.setSize(bodySize * ConstVals.PPM)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val impulse = spawnProps.get(ConstKeys.IMPULSE, Vector2::class)!!
        body.physics.velocity.set(impulse)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun hitBlock(
        blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D
    ) = explodeAndDie()

    override fun hitShield(
        shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D
    ) {
        val shield = shieldFixture.getEntity()
        if (shield is Rock ||
            shield == owner ||
            shieldFixture.isProperty("${ConstKeys.ROCK}_${ConstKeys.IGNORE}", true)
        ) return

        explodeAndDie()

        playSoundNow(SoundAsset.DINK_SOUND, false)
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        GameLogger.debug(GroundPebble.Companion.TAG, "explodeAndDie()")

        destroy()

        val disintegration = MegaEntityFactory.fetch(Disintegration::class)!!
        disintegration.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.physics.gravity.y = GRAVITY * ConstVals.PPM

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.forEachFixture { fixture ->
                val bounds = (fixture as Fixture).rawShape as GameRectangle
                bounds.set(body)
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.DAMAGER, FixtureType.PROJECTILE, FixtureType.SHIELD)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(ConstVals.PPM.toFloat()) })
        .updatable { _, sprite ->
            val region = regions[rockSize.name.lowercase()]
            if (region == null) throw IllegalStateException("Region is null: $region")
            sprite.setRegion(region)
            sprite.setCenter(body.getCenter())
        }
        .build()
}
