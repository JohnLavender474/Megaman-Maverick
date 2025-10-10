package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.updatables.UpdatablesComponent
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

class PreciousShard(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        const val TAG = "PreciousShard"

        private const val BIG_SIZE = 0.5f
        private const val SMALL_SIZE = 0.25f

        private const val GRAVITY = -0.15f

        private const val CULL_TIME = 0.5f

        private const val SPAWN_NO_COLLISION_DUR = 0.05f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class PreciousShardSize { LARGE, SMALL }
    enum class PreciousShardColor { GREEN, PURPLE, PINK, BLUE }

    private lateinit var shardSize: PreciousShardSize
    private lateinit var shardColor: PreciousShardColor

    private val spawnNoCollisionTimer = Timer(SPAWN_NO_COLLISION_DUR)
    private var doNoCollisionOnSpawn = false

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)

            val keys = Array<String>()
            PreciousShardSize.entries.forEach { shardSize ->
                PreciousShardColor.entries.forEach { shardColor ->
                    keys.add("${shardSize.name.lowercase()}_${shardColor.name.lowercase()}")
                }
            }

            AnimationUtils.loadRegions(TAG, atlas, keys, regions)
        }
        super.init()
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        spawnProps.put(ConstKeys.CULL_TIME, CULL_TIME)
        super.onSpawn(spawnProps)

        shardSize = spawnProps.get(ConstKeys.SIZE, PreciousShardSize::class)!!
        shardColor = spawnProps.get(ConstKeys.COLOR, PreciousShardColor::class)!!

        val bodySize = if (shardSize == PreciousShardSize.SMALL) SMALL_SIZE else BIG_SIZE
        body.setSize(bodySize * ConstVals.PPM)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val impulse = spawnProps.get(ConstKeys.IMPULSE, Vector2::class)!!
        body.physics.velocity.set(impulse)

        val gravityOn = spawnProps.getOrDefault(ConstKeys.GRAVITY_ON, true, Boolean::class)
        body.physics.gravityOn = gravityOn

        spawnNoCollisionTimer.reset()
        doNoCollisionOnSpawn = spawnProps.getOrDefault(
            "${ConstKeys.COLLIDE}_${ConstKeys.DELAY}", true, Boolean::class
        )
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun hitProjectile(projectileFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val projectile = projectileFixture.getEntity()
        if (projectile is SlashWave) explodeAndDie()
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (!doNoCollisionOnSpawn || spawnNoCollisionTimer.isFinished()) explodeAndDie()
    }

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val shield = shieldFixture.getEntity()
        if (shield == owner || shield is PreciousShard) return

        explodeAndDie()

        playSoundNow(SoundAsset.DINK_SOUND, false)
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        GameLogger.debug(TAG, "explodeAndDie()")

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
        .sprite(TAG, GameSprite().also { it.setSize(0.5f * ConstVals.PPM) })
        .updatable { _, sprite ->
            val key = "${shardSize.name.lowercase()}_${shardColor.name.lowercase()}"

            val region = regions[key]
            if (region == null) throw IllegalStateException("Region is null: $region")

            sprite.setRegion(region)

            sprite.setCenter(body.getCenter())
        }
        .build()

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        spawnNoCollisionTimer.update(delta)
    })
}
