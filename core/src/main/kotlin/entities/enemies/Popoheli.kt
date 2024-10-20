package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimator
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.interfaces.UpdateFunction
import com.mega.game.engine.common.objects.GamePair

import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.getCenter
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.*
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import java.util.*
import kotlin.reflect.KClass

class Popoheli(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "Popoheli"
        private const val SPEED = 10f
        private const val ATTACK_DELAY = 0.25f
        private const val ATTACK_DUR = 1f
        private const val FLAMES = 4
        private const val FLAME_PADDING = 0.35f
        private var heliRegion: TextureRegion? = null
        private var flameRegion: TextureRegion? = null
    }

    enum class PopoheliState { APPROACHING, ATTACKING, FLEEING }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class pairTo dmgNeg(15),
        Fireball::class pairTo dmgNeg(ConstVals.MAX_HEALTH),
        ChargedShot::class pairTo dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        },
        ChargedShotExplosion::class pairTo dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        }
    )
    override lateinit var facing: Facing

    private val attackDelayTimer = Timer(ATTACK_DELAY)
    private val attackTimer = Timer(ATTACK_DUR)
    private val attacking: Boolean
        get() = popoheliState == PopoheliState.ATTACKING && attackDelayTimer.isFinished()

    private lateinit var popoheliState: PopoheliState
    private lateinit var target: Vector2
    private lateinit var faceOnEnd: Facing

    override fun init() {
        if (heliRegion == null || flameRegion == null) {
            heliRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_1.source, "Popoheli")
            flameRegion = game.assMan.getTextureRegion(TextureAsset.HAZARDS_1.source, "Flame2")
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        val megamanCenter = getMegaman().body.getCenter()
        val targets = PriorityQueue<Vector2> { target1, target2 ->
            target1.dst2(megamanCenter).compareTo(target2.dst2(megamanCenter))
        }
        spawnProps.getAllMatching { it.toString().startsWith(ConstKeys.TARGET) }
            .forEach { targets.add((it.second as RectangleMapObject).rectangle.getCenter()) }
        target = targets.poll()

        popoheliState = PopoheliState.APPROACHING
        facing = if (target.x < body.x) Facing.LEFT else Facing.RIGHT
        faceOnEnd = if (spawnProps.containsKey("${ConstKeys.FACE}_${ConstKeys.ON}_${ConstKeys.END}")) Facing.valueOf(
            spawnProps.get(
                "${ConstKeys.FACE}_${ConstKeys.ON}_${ConstKeys.END}",
                String::class
            )!!.uppercase()
        ) else facing

        attackTimer.reset()
        attackDelayTimer.reset()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (popoheliState) {
                PopoheliState.APPROACHING -> {
                    val trajectory = target.cpy().sub(body.getCenter()).nor().scl(SPEED * ConstVals.PPM)
                    body.physics.velocity = trajectory

                    if (body.getCenter().epsilonEquals(target, 0.1f * ConstVals.PPM)) {
                        body.physics.velocity.setZero()
                        popoheliState = PopoheliState.ATTACKING
                        facing = faceOnEnd
                    }
                }

                PopoheliState.ATTACKING -> {
                    attackDelayTimer.update(delta)
                    if (!attackDelayTimer.isFinished()) return@add
                    if (attackDelayTimer.isJustFinished() && overlapsGameCamera()) requestToPlaySound(
                        SoundAsset.ATOMIC_FIRE_SOUND, false
                    )

                    attackTimer.update(delta)
                    if (attackTimer.isFinished()) popoheliState = PopoheliState.FLEEING
                }

                PopoheliState.FLEEING -> {
                    body.physics.velocity.y = SPEED * ConstVals.PPM
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.65f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle(body))
        body.addFixture(bodyFixture)
        bodyFixture.rawShape.color = Color.GRAY
        debugShapes.add { bodyFixture.getShape() }

        val damagerFixture1 = Fixture(body, FixtureType.DAMAGER, GameRectangle(body))
        body.addFixture(damagerFixture1)
        damagerFixture1.rawShape.color = Color.RED
        debugShapes.add { damagerFixture1.getShape() }

        val damagerFixture2 = Fixture(
            body, FixtureType.DAMAGER, GameRectangle().setSize(
                FLAMES * FLAME_PADDING * ConstVals.PPM, 0.2f * ConstVals.PPM
            )
        )
        body.addFixture(damagerFixture2)
        debugShapes.add { damagerFixture2.getShape() }

        val damageableFixture = Fixture(
            body, FixtureType.DAMAGEABLE, GameRectangle().setSize(
                0.25f * ConstVals.PPM, 0.5f * ConstVals.PPM
            )
        )
        body.addFixture(damageableFixture)
        damageableFixture.rawShape.color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        val shieldFixture = Fixture(
            body, FixtureType.SHIELD, GameRectangle().setSize(
                0.35f * ConstVals.PPM, 0.65f * ConstVals.PPM
            )
        )
        body.addFixture(shieldFixture)
        shieldFixture.rawShape.color = Color.GREEN
        debugShapes.add { shieldFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            damagerFixture2.offsetFromBodyCenter = Vector2(
                FLAMES * FLAME_PADDING * facing.value * ConstVals.PPM / 2f, -0.5f * ConstVals.PPM
            )
            damagerFixture2.active = attacking
            damagerFixture2.rawShape.color = if (damagerFixture2.active) Color.GRAY else Color.RED

            damageableFixture.offsetFromBodyCenter.x = 0.3f * ConstVals.PPM * -facing.value
            shieldFixture.offsetFromBodyCenter.x = 0.2f * ConstVals.PPM * facing.value
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprites = OrderedMap<String, GameSprite>()
        val updateFunctions = ObjectMap<String, UpdateFunction<GameSprite>>()

        val heliSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
        heliSprite.setSize(1.25f * ConstVals.PPM)
        sprites.put("heli", heliSprite)
        updateFunctions.put("heli") { _, _sprite ->
            _sprite.setFlip(isFacing(Facing.RIGHT), false)
            _sprite.setCenter(body.getCenter())
            _sprite.hidden = damageBlink
        }

        for (i in 0 until FLAMES) {
            val flameSprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 1))
            flameSprite.setSize(0.5f * ConstVals.PPM)
            sprites.put("flame_$i", flameSprite)
            updateFunctions.put("flame_$i") { _, _sprite ->
                _sprite.hidden = !attacking
                _sprite.setPosition(
                    body.getBottomCenterPoint().add(
                        i * FLAME_PADDING * ConstVals.PPM * facing.value, -0.25f * ConstVals.PPM
                    ), if (isFacing(Facing.LEFT)) Position.BOTTOM_RIGHT else Position.BOTTOM_LEFT
                )
            }
        }

        return SpritesComponent(sprites, updateFunctions)
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animators = Array<GamePair<() -> GameSprite, IAnimator>>()

        val heliAnimation = Animation(heliRegion!!, 1, 2, 0.1f, true)
        val heliAnimator = Animator(heliAnimation)
        animators.add({ sprites.get("heli") } pairTo heliAnimator)

        for (i in 0 until FLAMES) {
            val flameAnimation = Animation(flameRegion!!, 1, 3, 0.1f, true)
            val flameAnimator = Animator(flameAnimation)
            animators.add({ sprites.get("flame_$i") } pairTo flameAnimator)
        }

        return AnimationsComponent(animators)
    }
}
