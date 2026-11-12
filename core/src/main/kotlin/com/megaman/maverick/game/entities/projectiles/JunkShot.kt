package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Size
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.shapes.IGameShape2D
import com.maverick.game.engine.damage.IDamageableEntity
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.enemies.Junkroid
import com.megaman.maverick.game.entities.explosions.Disintegration
import com.megaman.maverick.game.entities.utils.TickFrameDef
import com.megaman.maverick.game.entities.utils.TickFrames
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

// TODO: JunkShot is one of many one-off enemy shots; move it into a shared simple enemy shot (item 139)
class JunkShot(services: MegaServices) : AbstractProjectile(services, size = Size.SMALL), IAnimatedEntity,
    IDamageableEntity {

    enum class Type(
        val key: String, val size: Float, val damage: Int, val shootable: Boolean, val def: TickFrameDef,
        val gravity: Float = 0f
    ) {
        CORE("core", 0.5f * ConstVals.PPM, 2, true, TickFrameDef(15, 14, 7, 7, 3, intArrayOf(0, 1, 2, 1), intArrayOf(2, 2, 3, 2), 0)),
        JUNKROID("junkroid", 0.3125f * ConstVals.PPM, 1, true, TickFrameDef(10, 10, 5, 5, 1, intArrayOf(0), intArrayOf(1), 0),
            Junkroid.HURL_GRAVITY
        ),
        JUNKROID_HARD(
            "junkroid_hard", 0.3125f * ConstVals.PPM, 1, false, TickFrameDef(10, 10, 5, 5, 3, intArrayOf(0, 1, 2), intArrayOf(2, 2, 2), 0),
            Junkroid.HURL_GRAVITY
        ),
        HOVER_GUNNER(
            "hover_gunner", 0.3125f * ConstVals.PPM, 3, true, TickFrameDef(8, 8, 4, 4, 4, intArrayOf(0, 1, 2, 3), intArrayOf(2, 2, 2, 2), 0)
        )
    }

    companion object {
        const val TAG = "JunkShot"
    }

    override val invincible = false

    var type = Type.CORE
        private set

    private val animations = ObjectMap<String, IAnimation>()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        val atlas = services.assets.getTextureAtlas(TextureAsset.ENEMIES_2.source)
        Type.entries.forEach { TickFrames.animation(atlas, "$TAG/${it.key}", it.key, it.def, animations) }
        super.init()
        addComponent(AnimationsComponent(this, Animator({ type.key }, animations)))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        type = spawnProps.get(ConstKeys.TYPE, Type::class)!!
        body.setSize(type.size)
        body.forEachFixture { ((it as Fixture).rawShape as GameRectangle).setSize(type.size) }
        body.setCenter(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)
        body.physics.velocity.set(spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!)
        body.physics.gravity.set(0f, type.gravity * ConstVals.PPM)
    }

    override fun canBeDamagedBy(damager: IDamagerEntity) =
        type.shootable && damager is IOwnable<*> && damager.owner.asPlayer != null

    override fun takeDamageFrom(damager: IDamagerEntity): Boolean {
        if (!canBeDamagedBy(damager)) return false
        explodeAndDie()
        return true
    }

    override fun onDamageInflictedTo(damageable: IDamageableEntity) {
        super.onDamageInflictedTo(damageable)
        explodeAndDie()
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        destroy()
        val disintegration = MegaEntityFactory.fetch(Disintegration::class)!!
        disintegration.spawn(props(ConstKeys.POSITION pairTo body.getCenter()))
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(Type.CORE.size)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))
        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0))
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val center = body.getCenter()
            TickFrames.place(sprite, type.def, center.x, center.y, body.physics.velocity.x > 0f)
        }
        return component
    }
}
