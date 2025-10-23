package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

class ReactorMonkeyBall(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        const val TAG = "ReactorMonkeyBall"
        private const val GRAVITY = -0.15f
        private const val MAX_BOUNCES = 3
        private const val BOUNCE_VEL_SCALAR = 0.75f
        private const val EXPLODING_ALPHA = 0.5f
        private val INIT_IMPULSE = Vector2(0f, -3f * ConstVals.PPM)
        private val ANIM_DURS = gdxArrayOf(0.1f, 0.075f, 0.05f)
        private val regions = ObjectMap<Int, TextureRegion>()
    }

    override var owner: IGameEntity? = null

    var hidden = false
    var collide = false

    private var exploding = false
    private var bounces = 0

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            for (i in 0..MAX_BOUNCES) {
                val region = atlas.findRegion("$TAG/$i")
                regions.put(i, region)
            }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER, GameEntity::class)
        collide = spawnProps.getOrDefault(ConstKeys.COLLIDE, false, Boolean::class)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)
        body.physics.gravityOn = true
        body.physics.velocity.set(INIT_IMPULSE)

        exploding = false
        hidden = false
        bounces = 0
    }

    override fun onDestroy() {
        super.onDestroy()
        exploding = false
    }

    override fun explodeAndDie(vararg params: Any?) {
        val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION_FIELD)!!
        val props = props(
            ConstKeys.BOUNDS pairTo GameRectangle(body),
            ConstKeys.OWNER pairTo this,
            ConstKeys.END pairTo { if (exploding) destroy() } as () -> Unit
        )
        explosion.spawn(props)

        exploding = true

        body.physics.gravityOn = false
        body.physics.velocity.setZero()
    }

    private fun bounce(direction: Direction, add: Boolean = true) {
        if (!collide) return

        if (add) bounces++

        when {
            bounces >= MAX_BOUNCES -> explodeAndDie()
            else -> {
                when (direction) {
                    Direction.UP -> body.physics.velocity.y = abs(body.physics.velocity.y) * BOUNCE_VEL_SCALAR
                    Direction.DOWN -> body.physics.velocity.y = -abs(body.physics.velocity.y) * BOUNCE_VEL_SCALAR
                    Direction.LEFT -> body.physics.velocity.x = -abs(body.physics.velocity.x) * BOUNCE_VEL_SCALAR
                    Direction.RIGHT -> body.physics.velocity.x = abs(body.physics.velocity.x) * BOUNCE_VEL_SCALAR
                }
                requestToPlaySound(SoundAsset.BLOOPITY_SOUND, false)
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(3.5f * ConstVals.PPM)
        body.physics.gravity.y = GRAVITY * ConstVals.PPM
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        headFixture.setHitByBlockReceiver(ProcessState.BEGIN) { _, _ -> bounce(Direction.DOWN) }
        body.addFixture(headFixture)
        debugShapes.add { headFixture }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.25f * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        feetFixture.setHitByBlockReceiver(ProcessState.BEGIN) { _, _ -> bounce(Direction.UP) }
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture }

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 1.5f * ConstVals.PPM))
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.setHitByBlockReceiver(ProcessState.BEGIN) { _, _ -> bounce(Direction.RIGHT) }
        body.addFixture(leftFixture)
        debugShapes.add { leftFixture }

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 1.5f * ConstVals.PPM))
        rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        rightFixture.setHitByBlockReceiver(ProcessState.BEGIN) { _, _ -> bounce(Direction.LEFT) }
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        debugShapes.add { rightFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        val circle = GameCircle().setRadius(body.getWidth() / 2f)

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(
                FixtureType.SHIELD pairTo circle.copy(),
                FixtureType.DAMAGER pairTo circle.copy(),
                FixtureType.PROJECTILE pairTo circle.copy(),
                FixtureType.WATER_LISTENER pairTo circle.copy()
            )
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
        sprite.setSize(4f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            sprite.hidden = hidden
            sprite.setCenter(body.getCenter())
            sprite.setAlpha(if (exploding) EXPLODING_ALPHA else 1f)
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { (bounces.coerceIn(0, MAX_BOUNCES)).toString() }

        val animations = ObjectMap<String, IAnimation>()
        for (i in 0..MAX_BOUNCES) {
            val region = regions[i]!!
            val animation = if (i == 0) Animation(region) else Animation(region, 2, 1, ANIM_DURS[i - 1], true)
            animations.put(i.toString(), animation)
        }

        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun getTag() = TAG
}
