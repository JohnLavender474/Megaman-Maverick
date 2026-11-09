package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.entities.utils.TickFrames
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.D1000Frames
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.hazards.D1000Pillar
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

class D1000Bomb(services: MegaServices) : AbstractProjectile(services, size = Size.MEDIUM), IAnimatedEntity {

    companion object {
        const val TAG = "D1000Bomb"
        private const val SIZE = 0.5f * ConstVals.PPM
        private const val GRAVITY = 28f * ConstVals.PPM
        private const val FLIGHT_DUR = 0.53f
    }

    private val animations = ObjectMap<String, IAnimation>()
    private val arena = GameRectangle()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        val atlas = services.assets.getTextureAtlas(TextureAsset.BOSSES_3.source)
        TickFrames.animations(atlas, TAG, D1000Frames.BOMB, animations)
        super.init()
        addComponent(AnimationsComponent(this, Animator(animations["bomb"])))
        addComponent(UpdatablesComponent({ land() }))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        val target = spawnProps.get(ConstKeys.TARGET, Float::class)!!
        arena.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)
        body.setCenter(spawn)
        body.physics.velocity.set((target - spawn.x) / FLIGHT_DUR, 0f)
        body.physics.gravity.set(0f, -GRAVITY)
        body.physics.gravityOn = true
    }

    private fun land() {
        val center = body.getCenter()
        if (center.x < arena.getX() || center.x > arena.getMaxX()) {
            body.physics.velocity.x = 0f
            body.setCenter(center.x.coerceIn(arena.getX(), arena.getMaxX()), center.y)
        }
        if (body.getY() > arena.getY()) return

        val pillar = MegaEntityFactory.fetch(D1000Pillar::class)!!
        pillar.spawn(props(ConstKeys.POSITION pairTo Vector2(body.getCenter().x, arena.getY())))
        requestToPlaySound(SoundAsset.BLAST_1_SOUND, false)
        destroy()
    }

    override fun explodeAndDie(vararg params: Any?) {
        destroy()
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(SIZE)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))
        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val center = body.getCenter()
            TickFrames.place(sprite, D1000Frames.BOMB["bomb"], center.x, center.y, false)
        }
        return component
    }
}
