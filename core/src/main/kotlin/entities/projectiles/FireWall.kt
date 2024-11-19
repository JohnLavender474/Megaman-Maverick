package com.megaman.maverick.game.entities.projectiles


import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType

class FireWall(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "FireWall"
        private const val DISINTEGRATE_DUR = 0.3f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var facing: Facing

    private val disintegrateTimer = Timer(DISINTEGRATE_DUR)
    private var disintegrating = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            regions.put("fire", atlas.findRegion("$TAG/fire"))
            regions.put("disintegrate", atlas.findRegion("$TAG/disintegrate"))
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        val position =
            spawnProps.getOrDefault("${ConstKeys.BODY}_${ConstKeys.POSITION}", Position.CENTER, Position::class)
        body.positionOnPoint(spawn, position)

        val trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
        body.physics.velocity.set(trajectory)

        facing = if (trajectory.x < 0f) Facing.LEFT else Facing.RIGHT

        disintegrateTimer.reset()
        disintegrating = false
    }

    override fun hitBlock(
        blockFixture: IFixture,
        thisShape: IGameShape2D,
        otherShape: IGameShape2D
    ) = explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        body.physics.velocity.setZero()
        disintegrating = true
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (disintegrating) {
            disintegrateTimer.update(delta)
            if (disintegrateTimer.isFinished()) destroy()
        }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(0.75f * ConstVals.PPM, ConstVals.PPM.toFloat())
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBodyBounds() }), debug = true))
        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
        sprite.setSize(1.75f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setCenter(body.getCenter())
            sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String = { if (disintegrating) "disintegrate" else "fire" }
        val animations = objectMapOf<String, IAnimation>(
            "fire" pairTo Animation(regions["fire"], 3, 1, 0.1f, true),
            "disintegrate" pairTo Animation(regions["disintegrate"], 3, 1, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
