package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.toGdxArray
import com.engine.common.getRandom
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.isFacing
import com.engine.common.objects.Loop
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.TimeMarkedRunnable
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.entities.overlapsGameCamera
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class TotemPolem(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "TotemPolem"
        private const val SHOOT_OPTIONS = 4
        private const val EYES_OPEN_DUR = 0.5f
        private const val EYES_CLOSE_DUR = 1f
        private const val EYES_OPENING_DUR = 0.35f
        private const val EYES_CLOSING_DUR = 0.35f
        private const val SHOOT_DUR = 0.5f
        private const val SHOOT_TIME = 0.25f
        private const val BULLET_SPEED = 6f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class TotemPolemState {
        EYES_CLOSED, EYES_OPENING, EYES_OPEN, EYES_OPEN_SHOOTING, EYES_CLOSING
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(1),
        Fireball::class to dmgNeg(20),
        ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 5 else 3
        },
        ChargedShotExplosion::class to dmgNeg {
            it as ChargedShotExplosion
            if (it.fullyCharged) 3 else 2
        }
    )
    override lateinit var facing: Facing

    val eyesOpen: Boolean
        get() = loop.getCurrent() != TotemPolemState.EYES_CLOSED

    private val loop = Loop(TotemPolemState.values().toGdxArray())
    private val timers = objectMapOf(
        TotemPolemState.EYES_CLOSED to Timer(EYES_CLOSE_DUR),
        TotemPolemState.EYES_OPENING to Timer(EYES_OPENING_DUR),
        TotemPolemState.EYES_OPEN to Timer(EYES_OPEN_DUR),
        TotemPolemState.EYES_CLOSING to Timer(EYES_CLOSING_DUR),
        TotemPolemState.EYES_OPEN_SHOOTING to Timer(
            SHOOT_DUR, gdxArrayOf(TimeMarkedRunnable(SHOOT_TIME) { shoot() })
        )
    )
    private var shootPositionIndex = 0

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            regions.put("eyes_closed", atlas.findRegion("$TAG/eyes_closed"))
            regions.put("eyes_closing", atlas.findRegion("$TAG/eyes_closing"))
            regions.put("eyes_open", atlas.findRegion("$TAG/eyes_open"))
            for (i in 1..SHOOT_OPTIONS) regions.put("shoot_$i", atlas.findRegion("$TAG/shoot$i"))
        }
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        loop.reset()
        timers.values().forEach { it.reset() }
        shootPositionIndex = 0
        facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
    }

    override fun onDestroy() {
        super<AbstractEnemy>.onDestroy()
        if (hasDepletedHealth()) explode()
    }

    private fun getShootPositionY(index: Int) = when (index) {
        0 -> -0.75f
        1 -> -0.01f
        2 -> 0.75f
        else -> 1.5f
    }

    private fun shoot() {
        val bullet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!
        game.engine.spawn(
            bullet, props(
                ConstKeys.OWNER to this,
                ConstKeys.POSITION to body.getCenter().add(
                    0.1f * facing.value * ConstVals.PPM,
                    (getShootPositionY(shootPositionIndex) - 0.3f) * ConstVals.PPM
                ),
                ConstKeys.TRAJECTORY to Vector2(BULLET_SPEED * facing.value * ConstVals.PPM, 0f)
            )
        )
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT

            val timer = timers.get(loop.getCurrent())
            timer.update(delta)
            if (timer.isFinished()) {
                loop.next()
                if (loop.getCurrent() == TotemPolemState.EYES_OPENING)
                    shootPositionIndex = getRandom(0, SHOOT_OPTIONS - 1)
                timer.reset()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.85f * ConstVals.PPM, 3.25f * ConstVals.PPM)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(
            body, FixtureType.BODY, GameRectangle().setSize(
                0.65f * ConstVals.PPM, 3.25f * ConstVals.PPM
            )
        )
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(
            body, FixtureType.DAMAGER, GameRectangle().setSize(
                0.65f * ConstVals.PPM, 3.25f * ConstVals.PPM
            )
        )
        body.addFixture(damagerFixture)

        val shieldFixture = Fixture(
            body, FixtureType.SHIELD, GameRectangle().setSize(
                0.65f * ConstVals.PPM, 3.25f * ConstVals.PPM
            )
        )
        body.addFixture(shieldFixture)

        val damageableFixtures = Array<Fixture>()
        for (i in 0 until SHOOT_OPTIONS) {
            val damageableFixture = Fixture(
                body,
                FixtureType.DAMAGEABLE,
                GameRectangle().setSize(0.5f * ConstVals.PPM, 0.25f * ConstVals.PPM)
            )
            damageableFixture.offsetFromBodyCenter.y = getShootPositionY(i) * ConstVals.PPM
            body.addFixture(damageableFixture)

            damageableFixtures.add(damageableFixture)

            debugShapes.add {
                damageableFixture.rawShape.color = if (damageableFixture.active) Color.PURPLE else Color.GRAY
                damageableFixture.getShape()
            }
        }

        body.preProcess.put(ConstKeys.DEFAULT) {
            for (i in 0 until damageableFixtures.size) {
                val d = damageableFixtures[i]
                d.offsetFromBodyCenter.x = 0.25f * ConstVals.PPM * facing.value
                d.active = eyesOpen
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(4.5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.hidden = damageBlink
            _sprite.setFlip(isFacing(Facing.RIGHT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (loop.getCurrent()) {
                TotemPolemState.EYES_CLOSED -> "eyes_closed"
                TotemPolemState.EYES_OPENING -> "eyes_opening"
                TotemPolemState.EYES_OPEN -> "eyes_open"
                TotemPolemState.EYES_CLOSING -> "eyes_closing"
                TotemPolemState.EYES_OPEN_SHOOTING -> "shoot_${shootPositionIndex + 1}"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "eyes_closed" to Animation(regions.get("eyes_closed")),
            "eyes_open" to Animation(regions.get("eyes_open")),
            "eyes_closing" to Animation(regions.get("eyes_closing"), 3, 1, 0.1f, false),
            "eyes_opening" to Animation(regions.get("eyes_closing"), 3, 1, 0.1f, false).reversed(),
            "shoot_1" to Animation(regions.get("shoot_1")),
            "shoot_2" to Animation(regions.get("shoot_2")),
            "shoot_3" to Animation(regions.get("shoot_3")),
            "shoot_4" to Animation(regions.get("shoot_4"))
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}