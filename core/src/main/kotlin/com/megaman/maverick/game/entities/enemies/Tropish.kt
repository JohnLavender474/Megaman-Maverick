package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toGameRectangle
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class Tropish(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "Tropish"
        private const val SWIM_SPEED = 6f
        private const val GRAVITY = -0.1f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class TropishState { WAIT, SWIM, BENT }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(15),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShotExplosion::class pairTo dmgNeg(10)
    )
    override lateinit var facing: Facing

    private lateinit var tropishState: TropishState
    private lateinit var triggerBox: GameRectangle
    private lateinit var startPosition: Vector2

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            regions.put("swim", atlas.findRegion("$TAG/swim"))
            regions.put("bent", atlas.findRegion("$TAG/bent"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        triggerBox = spawnProps.get(ConstKeys.TRIGGER, RectangleMapObject::class)!!.rectangle.toGameRectangle(false)
        startPosition = spawnProps.get(ConstKeys.START, RectangleMapObject::class)!!.rectangle.getCenter(false)

        tropishState = TropishState.WAIT
        body.physics.gravityOn = false
        body.forEachFixture { it.setActive(false) }

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
    }

    override fun canDamage(damageable: IDamageable) = tropishState != TropishState.WAIT

    private fun startSwim() {
        tropishState = TropishState.SWIM

        body.setCenter(startPosition)

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

        val speed = SWIM_SPEED * ConstVals.PPM * facing.value
        body.physics.velocity.x = speed

        body.forEachFixture { it.setActive(true) }
    }

    private fun hitNose() {
        tropishState = TropishState.BENT
        body.physics.velocity.setZero()
        body.physics.gravityOn = true
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.MARIO_FIREBALL_SOUND, false)
    }

    private fun explodeAndDie() {
        explode()
        destroy()
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.EXPLOSION_2_SOUND, false)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            when {
                tropishState == TropishState.WAIT && megaman.body.getBounds().overlaps(triggerBox) -> startSwim()
                tropishState == TropishState.BENT && body.isSensing(BodySense.FEET_ON_GROUND) -> explodeAndDie()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(1.5f * ConstVals.PPM, ConstVals.PPM.toFloat())
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle(body))
        body.addFixture(damageableFixture)

        val noseFixture = Fixture(body, FixtureType.CONSUMER, GameRectangle().setSize(0.1f * ConstVals.PPM))
        noseFixture.setConsumer { processState, fixture ->
            if (tropishState == TropishState.SWIM &&
                processState == ProcessState.BEGIN &&
                fixture.getType() == FixtureType.BLOCK
            ) hitNose()
        }
        body.addFixture(noseFixture)
        noseFixture.drawingColor = Color.BLUE
        debugShapes.add { noseFixture }

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        debugShapes.add { feetFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            noseFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f * facing.value
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(3f * ConstVals.PPM, 2.4f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setFlip(isFacing(Facing.RIGHT), false)
            val position = if (isFacing(Facing.LEFT)) Position.CENTER_LEFT else Position.CENTER_RIGHT
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.hidden = damageBlink || tropishState == TropishState.WAIT
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? =
            { if (tropishState == TropishState.WAIT) null else tropishState.name.lowercase() }
        val animations = objectMapOf<String, IAnimation>(
            "swim" pairTo Animation(regions["swim"], 2, 1, 0.1f, true),
            "bent" pairTo Animation(regions["bent"], 2, 1, 0.25f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}