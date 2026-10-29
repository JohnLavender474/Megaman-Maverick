package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponentBuilder
import com.maverick.game.engine.animations.AnimatorBuilder
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.shapes.GameCircle
import com.maverick.game.engine.common.shapes.IGameShape2D
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.world.body.*

class GreenPelletBlast(game: MegaServices) : AbstractProjectile(game, size = Size.SMALL), IAnimatedEntity {

    companion object {
        const val TAG = "GreenPelletBlast"
        private const val BURST_DUR = 0.15f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private val burstTimer = Timer(BURST_DUR)
    private var burst = false

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            gdxArrayOf("blast", "burst").forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
        body.physics.velocity.set(trajectory)

        burstTimer.reset()
        burst = false
    }

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = burst()

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = burst()

    override fun hitBody(bodyFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val entity = bodyFixture.getEntity()
        if (entity.asPlayer != null) burst()
    }

    override fun onDamageInflictedTo(damageable: IDamageableEntity) = burst()

    private fun burst() {
        if (!burst) requestToPlaySound(SoundAsset.ASTEROID_EXPLODE_SOUND, false)
        burst = true
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (burst) {
            burstTimer.update(delta)
            if (burstTimer.isFinished()) destroy()
        }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM, 0.25f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val projectileFixture = Fixture(body, FixtureType.PROJECTILE, GameCircle().setRadius(0.125f * ConstVals.PPM))
        body.addFixture(projectileFixture)
        projectileFixture.drawingColor = Color.RED
        debugShapes.add { projectileFixture }

        val blastDamagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(0.125f * ConstVals.PPM))
        body.addFixture(blastDamagerFixture)

        val burstDamagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(ConstVals.PPM.toFloat()))
        body.addFixture(burstDamagerFixture)
        burstDamagerFixture.drawingColor = Color.RED
        debugShapes.add { if (burstDamagerFixture.isActive()) burstDamagerFixture else null }

        body.preProcess.put(ConstKeys.BURST) {
            burstDamagerFixture.setActive(burst)
            if (burst) body.physics.velocity.setZero()
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
                .also { sprite -> sprite.setSize(2f * ConstVals.PPM) }
        )
        .preProcess { _, sprite ->
            sprite.setCenter(body.getCenter())

            sprite.setOriginCenter()
            sprite.rotation = body.physics.velocity.angleDeg() + 180f
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { if (burst) "burst" else "blast" }
                .addAnimations(
                    "blast" pairTo Animation(regions["blast"], 2, 1, 0.1f, true),
                    "burst" pairTo Animation(regions["burst"], 3, 1, 0.05f, false)
                )
                .build()
        )
        .build()
}
