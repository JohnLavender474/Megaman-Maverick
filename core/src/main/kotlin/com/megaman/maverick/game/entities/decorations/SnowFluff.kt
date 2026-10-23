package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.extensions.getTextureRegion
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.*

class SnowFluff(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity {

    companion object {
        const val TAG = "SnowFluff"
        private var region: TextureRegion? = null
    }

    override fun init(vararg params: Any) {
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.DECORATIONS_1.source, Snow.TAG)
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val impulse = spawnProps.get(ConstKeys.IMPULSE, Vector2::class)!!
        body.physics.velocity.set(impulse)

        val gravity = spawnProps.get(ConstKeys.GRAVITY, Float::class)!!
        body.physics.gravity.y = gravity
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.1f * ConstVals.PPM)
        body.physics.applyFrictionY = false

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        bodyFixture.setReceiver(Receivers.HIT_BY_BLOCK, ProcessState.BEGIN) { _, _ ->
            GameLogger.debug(TAG, "hitByBlock: BEGIN: velocity=${body.physics.velocity}, destroy()")
            if (body.physics.velocity.y <= 0f) destroy()
        }
        bodyFixture.setReceiver(Receivers.HIT_BY_BLOCK, ProcessState.CONTINUE) { _, _ ->
            if (body.physics.velocity.y <= 0f) {
                GameLogger.debug(TAG, "hitByBlock: CONTINUE: velocity=${body.physics.velocity}, destroy()")
                destroy()
            }
        }
        bodyFixture.setReceiver(Receivers.HIT_BY_PROJECTILE) { destroy() }
        body.addFixture(bodyFixture)

        val waterListenerFixture = Fixture(body, FixtureType.WATER_LISTENER, GameRectangle(body))
        waterListenerFixture.setReceiver(Receivers.HIT_BY_WATER) { destroy() }
        waterListenerFixture.putProperty(ConstKeys.SPLASH, false)
        body.addFixture(waterListenerFixture)

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(region!!, DrawingPriority(DrawingSection.FOREGROUND, 1))
        sprite.setSize(0.1f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ -> sprite.setCenter(body.getCenter()) }
        return component
    }

    override fun getType() = EntityType.DECORATION
}
