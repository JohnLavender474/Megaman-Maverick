package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.*
import com.megaman.maverick.game.entities.decorations.WoodCratePiece
import com.megaman.maverick.game.entities.hazards.MagmaFlame
import com.megaman.maverick.game.entities.hazards.Saw
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.extensions.getRandomPositionInBounds
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class WoodCrate(game: MegamanMaverickGame) : Block(game), IFireableEntity, ISpritesEntity, IAudioEntity {

    companion object {
        const val TAG = "WoodCrate"

        private const val HIT_BY_FEET_IMPULSE_Y_ABOVE_WATER = -5f
        private const val HIT_BY_FEET_IMPULSE_Y_IN_WATER = -1f

        private const val FLOAT_UP_IMPULSE_Y = 2f
        private const val FLOAT_UP_MAX_VEL_Y = 3f

        private const val I_DUR = 0.05f

        private const val BURN_DUR = 1f
        private const val SPAWN_FLAME_DELAY = 0.2f

        private const val PIECES_ON_DESTROY = 20
        private const val PIECE_ON_DESTROY_MIN_IMPULSE_X = -8f
        private const val PIECE_ON_DESTROY_MAX_IMPULSE_X = 8f
        private const val PIECE_ON_DESTROY_MIN_IMPULSE_Y = 4f
        private const val PIECE_ON_DESTROY_MAX_IMPULSE_Y = 10f

        private const val FLAME_MIN_IMPULSE_X = -5f
        private const val FLAME_MAX_IMPULSE_X = 5f
        private const val FLAME_MIN_IMPULSE_Y = 3f
        private const val FLAME_MAX_IMPULSE_Y = 5f

        private val DAMAGERS = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
            Bullet::class pairTo dmgNeg(5),
            ChargedShot::class pairTo dmgNeg {
                it as ChargedShot
                if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
            },
            MoonScythe::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            Saw::class pairTo dmgNeg(ConstVals.MAX_HEALTH)
        )

        private val animDefs = orderedMapOf(
            "idle" pairTo AnimationDef(),
            "burning" pairTo AnimationDef(3, 1, 0.05f, true)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var burning: Boolean
        get() = !burnTimer.isFinished()
        set(value) {
            if (value) burnTimer.reset() else burnTimer.setToEnd()
        }

    var health = 0
        private set

    private val burnTimer = Timer(BURN_DUR)
    private val spawnFlameDelay = Timer(SPAWN_FLAME_DELAY)

    private val iTimer = Timer(I_DUR)
    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            AnimationUtils.loadRegions(TAG, atlas, animDefs.keys(), regions)
        }
        super.init()
        addComponent(AudioComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        val copyProps = spawnProps.copy()
        copyProps.remove(ConstKeys.BOUNDS)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps, copyProps=$copyProps")
        super.onSpawn(copyProps)

        val position = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(position)

        spawnFlameDelay.reset()

        burnTimer.setToEnd()
        iTimer.setToEnd()

        health = ConstVals.MAX_HEALTH
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy(): health=$health")
        super.onDestroy()

        if (health <= 0) {
            spawnPieces()
            if (overlapsGameCamera()) playSoundNow(SoundAsset.ENEMY_DAMAGE_SOUND, false)
        }
    }

    private fun spawnPieces() {
        GameLogger.debug(TAG, "spawnPieces()")

        (0 until PIECES_ON_DESTROY).forEach {
            val position = body.getBounds().getRandomPositionInBounds()

            val impulse = GameObjectPools.fetch(Vector2::class)
                .set(
                    UtilMethods.getRandom(
                        PIECE_ON_DESTROY_MIN_IMPULSE_X,
                        PIECE_ON_DESTROY_MAX_IMPULSE_X
                    ),
                    UtilMethods.getRandom(
                        PIECE_ON_DESTROY_MIN_IMPULSE_Y,
                        PIECE_ON_DESTROY_MAX_IMPULSE_Y
                    )
                )
                .scl(ConstVals.PPM.toFloat())

            val index = UtilMethods.getRandom(0, WoodCratePiece.MAX_INDEX)

            val piece = MegaEntityFactory.fetch(WoodCratePiece::class)!!
            piece.spawn(
                props(
                    ConstKeys.POSITION pairTo position,
                    ConstKeys.IMPULSE pairTo impulse,
                    ConstKeys.INDEX pairTo index
                )
            )
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val component = super.defineBodyComponent()

        val body = component.body
        body.setSize(2f * ConstVals.PPM)

        val waterListener = Fixture(body, FixtureType.WATER_LISTENER, GameCircle().setRadius(0.1f * ConstVals.PPM))
        waterListener.setEntity(this)
        body.addFixture(waterListener)
        waterListener.drawingColor = Color.BLUE
        debugShapeSuppliers.add { waterListener }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(1.5f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.setEntity(this)
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapeSuppliers.add { feetFixture }

        body.preProcess.put(ConstKeys.FALL) {
            if (body.isSensing(BodySense.FEET_ON_GROUND) && body.physics.velocity.y < 0f)
                body.physics.velocity.y = 0f
        }

        return component
    }

    override fun hitByFeet(processState: ProcessState, feetFixture: IFixture) {
        if (processState != ProcessState.BEGIN) return

        GameLogger.debug(TAG, "hitByFeet(): feetFixture=$feetFixture")

        val entity = feetFixture.getEntity()
        if (entity != megaman) return

        if (burning) {
            GameLogger.debug(TAG, "hitByFeet(): burning: set health to 0")
            health = 0
            return
        }

        if (!body.isSensing(BodySense.FEET_ON_GROUND)) {
            GameLogger.debug(TAG, "hitByFeet(): not on ground: apply negative Y impulse")

            val impulseY = when {
                body.isSensing(BodySense.IN_WATER) -> HIT_BY_FEET_IMPULSE_Y_IN_WATER
                else -> HIT_BY_FEET_IMPULSE_Y_ABOVE_WATER
            }
            body.physics.velocity.y = impulseY * ConstVals.PPM
        }
    }

    override fun hitByProjectile(projectileFixture: IFixture) {
        val projectile = projectileFixture.getEntity() as IProjectileEntity
        GameLogger.debug(TAG, "hitByProjectile(): projectile=$projectile, projectileFixture=$projectileFixture")
        if (projectile.owner == this) return

        if (projectile is IFireEntity) {
            GameLogger.debug(TAG, "hitByProjectile(): set to burning")
            burning = true
        } else if (projectile.owner == megaman) {
            val damage = DAMAGERS[projectile::class]?.get(projectile) ?: 0
            if (damage > 0) {
                GameLogger.debug(TAG, "hitByProjectile(): apply damage=$damage")

                health -= damage
                iTimer.reset()

                if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
            }
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (body.isSensing(BodySense.IN_WATER)) {
            if (body.physics.velocity.y > FLOAT_UP_MAX_VEL_Y * ConstVals.PPM)
                body.physics.velocity.y = FLOAT_UP_MAX_VEL_Y * ConstVals.PPM
            else body.physics.velocity.y += FLOAT_UP_IMPULSE_Y * ConstVals.PPM * delta
        } else if (body.physics.velocity.y > 0f) body.physics.velocity.y = 0f

        iTimer.update(delta)

        burnTimer.update(delta)
        if (burnTimer.isJustFinished()) {
            GameLogger.debug(TAG, "update(): burn timer just finished")
            health = 0
        }
        if (burning) {
            spawnFlameDelay.update(delta)

            if (spawnFlameDelay.isFinished()) {
                GameLogger.debug(TAG, "update(): spawn flame")

                val impulse = GameObjectPools.fetch(Vector2::class)
                    .setX(UtilMethods.getRandom(FLAME_MIN_IMPULSE_X, FLAME_MAX_IMPULSE_X))
                    .setY(UtilMethods.getRandom(FLAME_MIN_IMPULSE_Y, FLAME_MAX_IMPULSE_Y))
                    .scl(ConstVals.PPM.toFloat())

                val position = body.getBounds().getRandomPositionInBounds()

                val flame = MegaEntityFactory.fetch(MagmaFlame::class)!!
                flame.spawn(
                    props(
                        ConstKeys.OWNER pairTo this,
                        ConstKeys.IMPULSE pairTo impulse,
                        ConstKeys.POSITION pairTo position,
                        "${ConstKeys.BODY}_${ConstKeys.TYPE}" pairTo BodyType.ABSTRACT
                    )
                )

                spawnFlameDelay.reset()

                // if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
            }
        }

        if (health <= 0) destroy()
    })

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.setCenter(body.getCenter())
            sprite.hidden = !iTimer.isFinished()
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { if (burning) "burning" else "idle" }
                .applyToAnimations { animations ->
                    AnimationUtils.loadAnimationDefs(animDefs, animations, regions)
                }
                .build()
        )
        .build()
}
