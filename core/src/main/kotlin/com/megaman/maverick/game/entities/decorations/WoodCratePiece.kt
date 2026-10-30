package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.getCenter

class WoodCratePiece(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity {

    companion object {
        const val TAG = "WoodCratePiece"
        const val MAX_INDEX = 9
        private const val GRAVITY = 37.5f
        private const val CULL_TIME = 2f
        private const val ROTATE_DELAY = 0.05f
        private var regions = ObjectMap<Int, TextureRegion>()
    }

    private val rotateTimer = Timer(ROTATE_DELAY)
    private val cullTimer = Timer(CULL_TIME)
    private var index = 0

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.DECORATIONS_1.source)
            for (i in 0 until 10) regions.put(i, atlas.findRegion("$TAG/$i"))
        }
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(position)

        val impulse = spawnProps.get(ConstKeys.IMPULSE, Vector2::class)!!
        body.physics.velocity.set(impulse)

        index = spawnProps.get(ConstKeys.INDEX, Int::class)!!
        rotateTimer.reset()
        cullTimer.reset()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        cullTimer.update(delta)
        if (cullTimer.isFinished()) destroy()
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.25f * ConstVals.PPM)
        body.physics.collisionOn = false
        body.physics.applyFrictionY = false
        body.physics.gravity.y = -GRAVITY * ConstVals.PPM
        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
                .also { sprite -> sprite.setSize(0.1875f * ConstVals.PPM) }
        )
        .preProcess { delta, sprite ->
            sprite.setCenter(body.getCenter())

            val region = regions[index]
            sprite.setRegion(region)

            sprite.setOriginCenter()

            rotateTimer.update(delta)
            if (rotateTimer.isFinished()) {
                sprite.rotation += 90f
                rotateTimer.reset()
            }
        }
        .build()

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}
