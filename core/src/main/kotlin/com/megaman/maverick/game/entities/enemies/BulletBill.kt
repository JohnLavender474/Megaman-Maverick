package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.decorations.FloatingPoints.FloatingPointsType
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.utils.DrawableShapesComponentBuilder
import com.megaman.maverick.game.world.body.*

class BulletBill(game: MegamanMaverickGame) : AbstractEnemy(game), IProjectileEntity, IFaceable {

    companion object {
        const val TAG = "BulletBill"
        private const val SPEED = 8f
        private const val CULL_TIME = 0.5f
        private const val KNOCKABILITY_DELAY = 0.1f
        private var region: TextureRegion? = null
    }

    override lateinit var facing: Facing
    override var owner: IGameEntity? = null

    private var knockedToDeath = false
    private val collisionIdsToIgnore = ObjectSet<Int>()
    private val knockabilityDelayTimer = Timer(KNOCKABILITY_DELAY)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.SMB3_ENEMIES.source, "${TAG}/missile")
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_TIME, CULL_TIME)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val position = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(position)

        facing = spawnProps.get(ConstKeys.FACING, Facing::class)!!

        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)!!

        val collisionIdsToIgnore = spawnProps.get(ConstKeys.IGNORE) as Array<Int>
        this.collisionIdsToIgnore.addAll(collisionIdsToIgnore)

        knockedToDeath = false
        knockabilityDelayTimer.reset()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        collisionIdsToIgnore.clear()
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = explodeAndDie()

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (knockedToDeath) return

        val block = blockFixture.getEntity()
        if (!collisionIdsToIgnore.contains(block.id)) explodeAndDie()
    }

    override fun canDamage(damageable: IDamageable) =
        !knockedToDeath &&
            super<AbstractEnemy>.canDamage(damageable) &&
            super<IProjectileEntity>.canDamage(damageable)

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta -> knockabilityDelayTimer.update(delta) }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val drawableShapesComponentBuilder = DrawableShapesComponentBuilder()
        drawableShapesComponentBuilder.addDebug { body.getBounds() }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        headFixture.setHitByFeetReceiver(ProcessState.BEGIN) { feet, _ ->
            if (!knockedToDeath &&
                feet.getEntity() == megaman &&
                knockabilityDelayTimer.isFinished() &&
                megaman.body.physics.velocity.y <= 0f
            ) {
                knockedToDeath = true

                spawnWhackForOverlap(headFixture.getShape(), feet.getShape())
                spawnFloatingPoints(FloatingPointsType.POINTS100)

                megaman.body.physics.velocity.y = MegamanValues.JUMP_VEL * ConstVals.PPM / 2f
                playSoundNow(SoundAsset.SWIM_SOUND, false)
            }
        }
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.ORANGE
        drawableShapesComponentBuilder.addDebug { headFixture }

        body.preProcess.put(ConstKeys.DAMAGER) {
            val damager = body.fixtures.get(FixtureType.DAMAGER).first()
            damager.setActive(!knockedToDeath)
        }

        body.preProcess.put(ConstKeys.MOVEMENT) {
            val velocityX = if (knockedToDeath) 0f else SPEED * facing.value * ConstVals.PPM
            body.physics.velocity.x = velocityX

            val velocityY = if (knockedToDeath) -SPEED * ConstVals.PPM else 0f
            body.physics.velocity.y = velocityY
        }

        addComponent(drawableShapesComponentBuilder.build())

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.PROJECTILE, FixtureType.SHIELD)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(region!!).also { it.setSize(2f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            sprite.setCenter(body.getCenter())
            sprite.setFlip(isFacing(Facing.RIGHT), false)
        }
        .build()

    override fun getType() = EntityType.ENEMY
}
