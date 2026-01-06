package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.enemies.BulletBill
import com.megaman.maverick.game.entities.utils.DrawableShapesComponentBuilder
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class BulletBillLauncher(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, IAudioEntity, IHazard {

    companion object {
        const val TAG = "BulletBillLauncher"

        private const val LAUNCH_DELAY = 1.5f

        private var region: TextureRegion? = null
    }

    private val launchTimer = Timer(LAUNCH_DELAY)

    private val collisionIdsToIgnore = Array<Int>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SMB3_ENEMIES.source)
            region = atlas.findRegion("${BulletBill.TAG}/launcher")
        }
        super.init()
        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val position =
            spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(position)

        launchTimer.reset()

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.IGNORE)) {
                val id = (value as RectangleMapObject).properties.get(ConstKeys.ID, Int::class.java)
                collisionIdsToIgnore.add(id)
            }
        }
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        collisionIdsToIgnore.clear()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        launchTimer.update(delta)
        if (launchTimer.isFinished()) {
            launchBulletBill()
            launchTimer.reset()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1f * ConstVals.PPM, 2f * ConstVals.PPM)

        val drawableShapesComponentBuilder = DrawableShapesComponentBuilder()
        drawableShapesComponentBuilder.addDebug { body.getBounds() }
        addComponent(drawableShapesComponentBuilder.build())

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY))
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(region!!).also { it.setSize(2f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
        }
        .build()

    private fun launchBulletBill() {
        val facing = if (body.getX() < megaman.body.getX()) Facing.RIGHT else Facing.LEFT

        val position = GameObjectPools.fetch(Vector2::class)
            .set(
                body.getCenter().x + 0.75f * ConstVals.PPM * facing.value,
                body.getY() + 1.5f * ConstVals.PPM
            )

        val bulletBill = MegaEntityFactory.fetch(BulletBill::class)!!
        bulletBill.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.FACING pairTo facing,
                ConstKeys.POSITION pairTo position,
                ConstKeys.IGNORE pairTo collisionIdsToIgnore
            )
        )

        requestToPlaySound(SoundAsset.SMB3_THWOMP_SOUND, false)
    }

    override fun getType() = EntityType.HAZARD

    override fun getTag() = TAG
}
