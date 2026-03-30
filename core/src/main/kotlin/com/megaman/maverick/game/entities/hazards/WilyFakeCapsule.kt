package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IActivatable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.bosses.WilyFinalBoss
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.projectiles.HomingMissile
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.setHitByProjectileReceiver

class WilyFakeCapsule(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    IOwnable<WilyFinalBoss>, IDamager, IHazard, IActivatable, Updatable {

    companion object {
        const val TAG = "WilyFakeCapsule"
        private const val BOUNCE_DUR = 1f
        private const val SPRITE_SIZE = 8f
        private const val CANNON_OFFSET_X = 3f
        private const val CANNON_OFFSET_Y = -0.75f
        private const val CANNON_BOTTOM_OFFSET_Y = 2f
        private const val MISSILE_ANGLE_LEFT = 225
        private const val MISSILE_ANGLE_DOWN = 180
        private const val MISSILE_ANGLE_RIGHT = 135
        private const val MISSILE_INIT_DELAY = 0.75f
        private val animDefs = objectMapOf(
            "hover" pairTo AnimationDef(3, 1, 0.1f, true),
            "bounce" pairTo AnimationDef(2, 2, 0.1f, true)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var owner: WilyFinalBoss? = null
    override var on = false

    var spriteAlpha = 0f

    var hasFiredMissiles = false

    private val bounceTimer = Timer(BOUNCE_DUR)
    private val bouncing: Boolean
        get() = !bounceTimer.isFinished()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.WILY_FINAL_BOSS.source)
            animDefs.keys().forEach { key ->
                val region = atlas.findRegion("phase_3/fake/$key")
                regions.put(key, region)
            }
            regions.put("missiles", atlas.findRegion("phase_3/fake/missiles"))
        }
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(UpdatablesComponent({ delta -> update(delta) }))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER, WilyFinalBoss::class)!!

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        on = spawnProps.getOrDefault(ConstKeys.ON, false, Boolean::class)

        spriteAlpha = 0f
        hasFiredMissiles = false
        bounceTimer.setToEnd()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        on = false
        owner = null
        spriteAlpha = 0f
    }

    override fun update(delta: Float) {
        if (!on) bounceTimer.setToEnd() else bounceTimer.update(delta)
    }

    fun shootMissiles() {
        GameLogger.debug(TAG, "shootMissiles()")

        val center = body.getCenter()

        for (i in 0 until 3) {
            val angle = when (i) {
                0 -> MISSILE_ANGLE_LEFT
                1 -> MISSILE_ANGLE_DOWN
                else -> MISSILE_ANGLE_RIGHT
            }

            val position = GameObjectPools.fetch(Vector2::class)
            when (i) {
                0 -> position.set(
                    center.x - CANNON_OFFSET_X * ConstVals.PPM,
                    center.y + CANNON_OFFSET_Y * ConstVals.PPM
                )

                1 -> position.set(center.x, center.y - CANNON_BOTTOM_OFFSET_Y * ConstVals.PPM)

                else -> position.set(
                    center.x + CANNON_OFFSET_X * ConstVals.PPM,
                    center.y + CANNON_OFFSET_Y * ConstVals.PPM
                )
            }

            val missile = MegaEntityFactory.fetch(HomingMissile::class)!!
            missile.spawn(
                props(
                    ConstKeys.OWNER pairTo owner,
                    ConstKeys.POSITION pairTo position,
                    ConstKeys.ANGLE pairTo angle,
                    "${ConstKeys.INIT}_${ConstKeys.DELAY}" pairTo MISSILE_INIT_DELAY
                )
            )
        }

        game.audioMan.playSound(SoundAsset.BLAST_2_SOUND, false)
    }

    fun explodeAndDestroy() {
        GameLogger.debug(TAG, "explodeAndDestroy()")

        val explosion = MegaEntityFactory.fetch(Explosion::class)!!
        explosion.spawn(props(ConstKeys.POSITION pairTo body.getCenter(), ConstKeys.DAMAGER pairTo false))

        game.audioMan.playSound(SoundAsset.EXPLOSION_2_SOUND, false)

        destroy()
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(3f * ConstVals.PPM, 4f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(
            body, FixtureType.BODY,
            GameRectangle().setSize(3f * ConstVals.PPM, 2.5f * ConstVals.PPM)
        )
        bodyFixture.setHitByProjectileReceiver { projectile ->
            if (projectile.owner == megaman) bounceTimer.reset()
        }
        bodyFixture.offsetFromBodyAttachment.y = -0.25f * ConstVals.PPM
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        val topDamagerFixture = Fixture(
            body,
            FixtureType.DAMAGER,
            GameRectangle().setSize(6f * ConstVals.PPM, 2f * ConstVals.PPM)
        )
        topDamagerFixture.offsetFromBodyAttachment.y = 1.5f * ConstVals.PPM
        body.addFixture(topDamagerFixture)
        debugShapes.add { topDamagerFixture }

        val topBodyFixture = Fixture(
            body,
            FixtureType.BODY,
            GameRectangle().setSize(6f * ConstVals.PPM, 2f * ConstVals.PPM)
        )
        topBodyFixture.setHitByProjectileReceiver { projectile ->
            if (projectile.owner == megaman) bounceTimer.reset()
        }
        topBodyFixture.offsetFromBodyAttachment.y = 1.5f * ConstVals.PPM
        body.addFixture(topBodyFixture)
        debugShapes.add { topBodyFixture }

        val leftCannonDamager = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(1f * ConstVals.PPM))
        leftCannonDamager.offsetFromBodyAttachment.set(-2f, 0.5f).scl(ConstVals.PPM.toFloat())
        body.addFixture(leftCannonDamager)
        debugShapes.add { leftCannonDamager }

        val leftCannonBody = Fixture(body, FixtureType.BODY, GameCircle().setRadius(1f * ConstVals.PPM))
        leftCannonBody.setHitByProjectileReceiver { projectile ->
            if (projectile.owner == megaman) bounceTimer.reset()
        }
        leftCannonBody.offsetFromBodyAttachment.set(-2f, 0.5f).scl(ConstVals.PPM.toFloat())
        body.addFixture(leftCannonBody)
        debugShapes.add { leftCannonBody }

        val rightCannonBody = Fixture(body, FixtureType.BODY, GameCircle().setRadius(1f * ConstVals.PPM))
        rightCannonBody.setHitByProjectileReceiver { projectile ->
            if (projectile.owner == megaman) bounceTimer.reset()
        }
        rightCannonBody.offsetFromBodyAttachment.set(2f, 0.5f).scl(ConstVals.PPM.toFloat())
        body.addFixture(rightCannonBody)
        debugShapes.add { rightCannonBody }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.forEachFixture { it.setActive(on) }
        }

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, -2))
                .also { it.setSize(SPRITE_SIZE * ConstVals.PPM) }
        )
        .preProcess { _, sprite ->
            sprite.setCenter(body.getCenter())
            sprite.setAlpha(spriteAlpha)
        }
        .sprite(
            "missiles", GameSprite(regions["missiles"], DrawingPriority(DrawingSection.PLAYGROUND, -1))
                .also { it.setSize(SPRITE_SIZE * ConstVals.PPM) }
        )
        .preProcess { _, sprite ->
            sprite.setCenter(body.getCenter())
            sprite.setAlpha(if (hasFiredMissiles) 0f else spriteAlpha)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { if (bouncing) "bounce" else "hover" }
                .applyToAnimations { animations ->
                    AnimationUtils.loadAnimationDefs(animDefs, animations, regions)
                }
                .build()
        )
        .build()

    override fun getType() = EntityType.HAZARD
}
