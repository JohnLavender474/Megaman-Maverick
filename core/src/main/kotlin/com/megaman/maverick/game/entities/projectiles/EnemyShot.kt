package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
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
import com.maverick.game.engine.drawables.sprites.setCenter
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.enemies.Junkroid
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.explosions.Disintegration
import com.megaman.maverick.game.entities.utils.TickFrameDef
import com.megaman.maverick.game.entities.utils.TickFrames
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

class EnemyShot(services: MegaServices) : AbstractProjectile(services, size = Size.SMALL), IAnimatedEntity,
    IDamageableEntity {

    sealed class Art(val atlas: TextureAsset, val region: String) {

        class Ticks(atlas: TextureAsset, region: String, val def: TickFrameDef) : Art(atlas, region)

        class Grid(
            atlas: TextureAsset,
            region: String,
            val rows: Int,
            val columns: Int,
            val duration: Float,
            val width: Float,
            val height: Float
        ) : Art(atlas, region)
    }

    enum class Turn { FLIP_MOVING_RIGHT, FLIP_MOVING_LEFT, ROTATE }

    enum class Death { DISINTEGRATION, CHARGED_SHOT_EXPLOSION }

    enum class Kind(
        val art: Art,
        val width: Float,
        val height: Float,
        val damage: Int,
        val turn: Turn,
        val section: DrawingSection,
        val priority: Int,
        val damageable: Boolean,
        val shootable: Boolean,
        val diesOnHit: Boolean,
        val diesOnShield: Boolean,
        val death: Death,
        val deathSound: SoundAsset?,
        val gravity: Float = 0f
    ) {
        CORE(
            art = Art.Ticks(
                TextureAsset.ENEMIES_2, "JunkShot/core",
                TickFrameDef(15, 14, 7, 7, 3, intArrayOf(0, 1, 2, 1), intArrayOf(2, 2, 3, 2), 0)
            ),
            width = 0.5f, height = 0.5f, damage = 2, turn = Turn.FLIP_MOVING_RIGHT,
            section = DrawingSection.FOREGROUND, priority = 0, damageable = true, shootable = true,
            diesOnHit = true, diesOnShield = false, death = Death.DISINTEGRATION, deathSound = null
        ),
        JUNKROID(
            art = Art.Ticks(
                TextureAsset.ENEMIES_2, "JunkShot/junkroid",
                TickFrameDef(10, 10, 5, 5, 1, intArrayOf(0), intArrayOf(1), 0)
            ),
            width = 0.3125f, height = 0.3125f, damage = 1, turn = Turn.FLIP_MOVING_RIGHT,
            section = DrawingSection.FOREGROUND, priority = 0, damageable = true, shootable = true,
            diesOnHit = true, diesOnShield = false, death = Death.DISINTEGRATION, deathSound = null,
            gravity = Junkroid.HURL_GRAVITY
        ),
        JUNKROID_HARD(
            art = Art.Ticks(
                TextureAsset.ENEMIES_2, "JunkShot/junkroid_hard",
                TickFrameDef(10, 10, 5, 5, 3, intArrayOf(0, 1, 2), intArrayOf(2, 2, 2), 0)
            ),
            width = 0.3125f, height = 0.3125f, damage = 1, turn = Turn.FLIP_MOVING_RIGHT,
            section = DrawingSection.FOREGROUND, priority = 0, damageable = true, shootable = false,
            diesOnHit = true, diesOnShield = false, death = Death.DISINTEGRATION, deathSound = null,
            gravity = Junkroid.HURL_GRAVITY
        ),
        HOVER_GUNNER(
            art = Art.Ticks(
                TextureAsset.ENEMIES_2, "JunkShot/hover_gunner",
                TickFrameDef(8, 8, 4, 4, 4, intArrayOf(0, 1, 2, 3), intArrayOf(2, 2, 2, 2), 0)
            ),
            width = 0.3125f, height = 0.3125f, damage = 3, turn = Turn.FLIP_MOVING_RIGHT,
            section = DrawingSection.FOREGROUND, priority = 0, damageable = true, shootable = true,
            diesOnHit = true, diesOnShield = false, death = Death.DISINTEGRATION, deathSound = null
        ),
        ROLLING_BOT(
            art = Art.Grid(
                TextureAsset.PROJECTILES_1, "RollingBotShot", rows = 1, columns = 2, duration = 0.1f, width = 1f,
                height = 1f
            ),
            width = 0.85f, height = 0.45f, damage = 3, turn = Turn.FLIP_MOVING_LEFT,
            section = DrawingSection.PLAYGROUND, priority = 1, damageable = false, shootable = false,
            diesOnHit = true, diesOnShield = true, death = Death.CHARGED_SHOT_EXPLOSION,
            deathSound = SoundAsset.ENEMY_DAMAGE_SOUND
        ),
        FIRE_PELLET(
            art = Art.Grid(
                TextureAsset.PROJECTILES_1, "FirePellet", rows = 1, columns = 1, duration = 1f, width = 1f, height = 1f
            ),
            width = 0.5f, height = 0.5f, damage = 3, turn = Turn.ROTATE,
            section = DrawingSection.FOREGROUND, priority = 0, damageable = false, shootable = false,
            diesOnHit = false, diesOnShield = true, death = Death.DISINTEGRATION,
            deathSound = SoundAsset.ENEMY_DAMAGE_SOUND
        )
    }

    companion object {
        const val TAG = "EnemyShot"
        private const val CHARGED_SHOT_EXPLOSION_DURATION = 0.1f
    }

    override val invincible = false

    var kind = Kind.CORE
        private set

    private val animations = ObjectMap<String, IAnimation>()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        Kind.entries.forEach { loadAnimation(it) }
        super.init()
        addComponent(AnimationsComponent(this, Animator({ kind.name }, animations)))
    }

    private fun loadAnimation(kind: Kind) {
        val atlas = services.assets.getTextureAtlas(kind.art.atlas.source)
        when (val art = kind.art) {
            is Art.Ticks -> TickFrames.animation(atlas, art.region, kind.name, art.def, animations)
            is Art.Grid -> {
                val region = atlas.findRegion(art.region) ?: throw IllegalStateException("Missing region ${art.region}")
                animations.put(kind.name, Animation(region, art.rows, art.columns, art.duration, loop = true))
            }
        }
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        kind = spawnProps.get(ConstKeys.TYPE, Kind::class)!!
        val width = kind.width * ConstVals.PPM
        val height = kind.height * ConstVals.PPM
        body.setSize(width, height)
        body.forEachFixture {
            ((it as Fixture).rawShape as GameRectangle).setSize(width, height)
            if (it.getType() == FixtureType.DAMAGEABLE) it.setActive(kind.damageable)
        }
        body.setCenter(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)
        body.physics.velocity.set(spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!)
        body.physics.gravity.set(0f, kind.gravity * ConstVals.PPM)
        defaultSprite.priority.section = kind.section
        defaultSprite.priority.value = kind.priority
    }

    override fun canBeDamagedBy(damager: IDamagerEntity) =
        kind.shootable && damager is IOwnable<*> && damager.owner.asPlayer != null

    override fun takeDamageFrom(damager: IDamagerEntity): Boolean {
        if (!canBeDamagedBy(damager)) return false
        explodeAndDie()
        return true
    }

    override fun onDamageInflictedTo(damageable: IDamageableEntity) {
        super.onDamageInflictedTo(damageable)
        if (kind.diesOnHit) explodeAndDie()
    }

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (kind.diesOnShield) explodeAndDie()
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) = explodeAndDie()

    override fun explodeAndDie(vararg params: Any?) {
        destroy()
        val sound = kind.deathSound
        if (sound != null && overlapsGameCamera()) services.audio.playSound(sound, loop = false)
        when (kind.death) {
            Death.DISINTEGRATION -> MegaEntityFactory.fetch(Disintegration::class)!!
                .spawn(props(ConstKeys.POSITION pairTo body.getCenter()))

            Death.CHARGED_SHOT_EXPLOSION -> MegaEntityFactory.fetch(ChargedShotExplosion::class)!!.spawn(
                props(
                    ConstKeys.OWNER pairTo owner,
                    ConstKeys.BOOLEAN pairTo false,
                    ConstKeys.POSITION pairTo body.getCenter(),
                    ConstKeys.DURATION pairTo CHARGED_SHOT_EXPLOSION_DURATION,
                    ConstKeys.ROTATION pairTo body.physics.velocity.angleDeg()
                )
            )
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(Kind.CORE.width * ConstVals.PPM, Kind.CORE.height * ConstVals.PPM)
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
            val velocity = body.physics.velocity
            val flip = when (kind.turn) {
                Turn.FLIP_MOVING_RIGHT -> velocity.x > 0f
                Turn.FLIP_MOVING_LEFT -> velocity.x < 0f
                Turn.ROTATE -> false
            }
            when (val art = kind.art) {
                is Art.Ticks -> TickFrames.place(sprite, art.def, center.x, center.y, flip)
                is Art.Grid -> {
                    sprite.setSize(art.width * ConstVals.PPM, art.height * ConstVals.PPM)
                    sprite.setCenter(center)
                    sprite.setFlip(flip, false)
                }
            }
            sprite.setOriginCenter()
            sprite.rotation = if (kind.turn == Turn.ROTATE) velocity.angleDeg() - 90f else 0f
        }
        return component
    }
}
