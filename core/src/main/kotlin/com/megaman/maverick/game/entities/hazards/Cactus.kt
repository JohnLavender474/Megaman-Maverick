package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.putAll
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractHealthEntity
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.enemies.Spiky
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.explosions.SpreadExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.projectiles.*
import com.megaman.maverick.game.entities.utils.getGameCameraCullingLogic
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class Cactus(game: MegamanMaverickGame) : AbstractHealthEntity(game), IBodyEntity, ISpritesEntity, ICullableEntity,
    IAudioEntity, IHazard, IDamager {

    companion object {
        const val TAG = "Cactus"
        private const val NEEDLES = 5
        private const val NEEDLE_GRAV = -0.1f
        private const val NEEDLE_IMPULSE = 10f
        private const val NEEDLE_Y_OFFSET = 0.1f
        private val angles = gdxArrayOf(90f, 45f, 0f, 315f, 270f)
        private val xOffsets = gdxArrayOf(-0.2f, -0.1f, 0f, 0.1f, 0.2f)
        private val regions = ObjectMap<String, TextureRegion>()
    }

    // use damage overrides to handle damage negotiation
    override val damageNegotiator = null

    private var big = false

    override fun init() {
        damageOverrides.putAll(
            Bullet::class pairTo dmgNeg(10),
            ArigockBall::class pairTo dmgNeg(10),
            CactusMissile::class pairTo dmgNeg(10),
            ChargedShot::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            ChargedShotExplosion::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            SmallGreenMissile::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            Explosion::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            Spiky::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            SpreadExplosion::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            MoonScythe::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
            Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH)
        )

        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            regions.put("small", atlas.findRegion("$TAG/small"))
            regions.put("big", atlas.findRegion("$TAG/big"))
        }

        super.init()

        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineCullablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        big = spawnProps.getOrDefault(ConstKeys.BIG, true, Boolean::class)
        body.setHeight((if (big) 2.5f else 1.5f) * ConstVals.PPM)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
    }

    override fun takeDamageFrom(damager: IDamager): Boolean {
        val damaged = super.takeDamageFrom(damager)
        if (damaged && overlapsGameCamera()) requestToPlaySound(SoundAsset.ENEMY_DAMAGE_SOUND, false)
        return damaged
    }

    override fun onHealthDepleted() {
        spawnNeedles()
        playSoundNow(SoundAsset.THUMP_SOUND, false)
        super.onHealthDepleted()
    }

    private fun spawnNeedles() {
        val indexStep = if (big) 1 else 2
        for (i in 0 until NEEDLES step indexStep) {
            val xOffset = xOffsets[i]
            val position = body.getCenter().add(xOffset * ConstVals.PPM, NEEDLE_Y_OFFSET * ConstVals.PPM)
            val angle = angles[i]
            val impulse = Vector2(0f, NEEDLE_IMPULSE * ConstVals.PPM).rotateDeg(angle)

            val needle = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.NEEDLE)!!
            needle.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.POSITION pairTo position,
                    ConstKeys.IMPULSE pairTo impulse,
                    ConstKeys.GRAVITY pairTo NEEDLE_GRAV * ConstVals.PPM
                )
            )
        }
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setWidth(ConstVals.PPM.toFloat())
        body.preProcess.put(ConstKeys.DEFAULT) {
            body.forEachFixture { ((it as Fixture).rawShape as GameRectangle).set(body) }
        }

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }
        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(
                FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE
            )
        )
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        sprite.setSize(1.75f * ConstVals.PPM, 3f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setRegion(regions[if (big) "big" else "small"])
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineCullablesComponent() =
        CullablesComponent(objectMapOf(ConstKeys.CULL_OUT_OF_BOUNDS pairTo getGameCameraCullingLogic(this)))

    override fun getTag() = TAG

    override fun getType() = EntityType.HAZARD
}
