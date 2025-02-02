package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.TimeMarkedRunnable
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
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
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class TotemPolem(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.MEDIUM), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "TotemPolem"
        private const val SHOOT_OPTIONS = 4
        private const val EYES_OPEN_DUR = 0.5f
        private const val EYES_CLOSE_DUR = 1f
        private const val EYES_OPENING_DUR = 0.35f
        private const val EYES_CLOSING_DUR = 0.35f
        private const val SHOOT_DUR = 0.5f
        private const val SHOOT_TIME = 0.25f
        private const val BULLET_SPEED = 8f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class TotemPolemState {
        EYES_CLOSED, EYES_OPENING, EYES_OPEN, EYES_OPEN_SHOOTING, EYES_CLOSING
    }

    override lateinit var facing: Facing

    val eyesOpen: Boolean
        get() = loop.getCurrent() != TotemPolemState.EYES_CLOSED

    private val loop = Loop(TotemPolemState.entries.toTypedArray().toGdxArray())
    private val timers = objectMapOf(
        TotemPolemState.EYES_CLOSED pairTo Timer(EYES_CLOSE_DUR),
        TotemPolemState.EYES_OPENING pairTo Timer(EYES_OPENING_DUR),
        TotemPolemState.EYES_OPEN pairTo Timer(EYES_OPEN_DUR),
        TotemPolemState.EYES_CLOSING pairTo Timer(EYES_CLOSING_DUR),
        TotemPolemState.EYES_OPEN_SHOOTING pairTo Timer(
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
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        loop.reset()
        timers.values().forEach { it.reset() }

        shootPositionIndex = 0

        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isHealthDepleted()) explode()
    }

    private fun getShootPositionY(index: Int) = when (index) {
        0 -> -0.75f
        1 -> -0.01f
        2 -> 0.85f
        else -> 1.65f
    }

    private fun shoot() {
        val bullet = MegaEntityFactory.fetch(Bullet::class)!!
        bullet.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo body.getCenter().add(
                    0.1f * facing.value * ConstVals.PPM,
                    (getShootPositionY(shootPositionIndex) - 0.3f) * ConstVals.PPM
                ),
                ConstKeys.TRAJECTORY pairTo Vector2(BULLET_SPEED * facing.value * ConstVals.PPM, 0f)
            )
        )

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT

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
        body.setSize(ConstVals.PPM.toFloat(), 3.5f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val damageables = Array<Fixture>()
        for (i in 0 until SHOOT_OPTIONS) {
            val damageableFixture = Fixture(
                body,
                FixtureType.DAMAGEABLE,
                GameRectangle().setSize(0.5f * ConstVals.PPM, 0.25f * ConstVals.PPM)
            )
            damageableFixture.offsetFromBodyAttachment.y = getShootPositionY(i) * ConstVals.PPM
            body.addFixture(damageableFixture)
            damageables.add(damageableFixture)
            debugShapes.add { damageableFixture }
        }

        body.preProcess.put(ConstKeys.DEFAULT) {
            damageables.forEach { damageable ->
                damageable.setActive(eyesOpen)
                damageable.offsetFromBodyAttachment.x = 0.25f * ConstVals.PPM * facing.value
                damageable.drawingColor = if (damageable.isActive()) Color.PURPLE else Color.GRAY
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.SHIELD)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(5f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.hidden = damageBlink
            sprite.setFlip(isFacing(Facing.RIGHT), false)
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
            "eyes_closed" pairTo Animation(regions.get("eyes_closed")),
            "eyes_open" pairTo Animation(regions.get("eyes_open")),
            "eyes_closing" pairTo Animation(regions.get("eyes_closing"), 3, 1, 0.1f, false),
            "eyes_opening" pairTo Animation(regions.get("eyes_closing"), 3, 1, 0.1f, false).reversed(),
            "shoot_1" pairTo Animation(regions.get("shoot_1")),
            "shoot_2" pairTo Animation(regions.get("shoot_2")),
            "shoot_3" pairTo Animation(regions.get("shoot_3")),
            "shoot_4" pairTo Animation(regions.get("shoot_4"))
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
