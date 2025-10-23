package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.ProjectilesFactory
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import kotlin.math.abs

class Shotman(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.MEDIUM), IAnimatedEntity {

    companion object {
        const val TAG = "Shotman"
        private const val SHOOT_TIME = 0.6f
        private const val CROUCH_TIME = 0.2f
        private const val LAUNCH_IMPULSE_Y = 10f
        private const val LAUNCH_GRAVITY = -0.15f
        private const val SHOOT_SPEED_X = 10f
        private var shootRegion: TextureRegion? = null
        private var launchRegion: TextureRegion? = null
    }

    private val shootTimer = Timer(SHOOT_TIME)
    private val crouchTimer = Timer(CROUCH_TIME)
    private val shootLoop = Loop(gdxArrayOf(true, true, false), true)

    override fun init() {
        if (shootRegion == null || launchRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            shootRegion = atlas.findRegion("Shotman/Shoot")
            launchRegion = atlas.findRegion("Shotman/Launch")
        }
        super.init()
        addComponent(defineAnimationsComponent())
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)



        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
        shootLoop.reset()
        shootTimer.reset()
        crouchTimer.setToEnd()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            crouchTimer.update(delta)
            shootTimer.update(delta)
            if (shootTimer.isFinished()) {
                val shoot = shootLoop.next()
                if (shoot) shootBullet() else {
                    lauchBullet()
                    crouchTimer.reset()
                }
                shootTimer.reset()
            }
        }
    }

    private fun lauchBullet() {
        val spawn = body.getCenter()
        spawn.y += 0.25f * ConstVals.PPM

        val bullet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!
        val xFactor = 1f - ((abs(megaman.body.getY() - body.getY()) / ConstVals.PPM) / 10f) + 0.2f
        val impulseX = (megaman.body.getX() - body.getX()) * xFactor

        bullet.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.GRAVITY pairTo Vector2(0f, LAUNCH_GRAVITY * ConstVals.PPM),
                ConstKeys.TRAJECTORY pairTo Vector2(impulseX, LAUNCH_IMPULSE_Y * ConstVals.PPM)
            )
        )
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
    }

    private fun shootBullet() {
        val offsetX = if (megaman.body.getX() > body.getX()) 0.5f else -0.5f
        val spawn = body.getCenter().add(offsetX * ConstVals.PPM, 0.2f * ConstVals.PPM)
        val bullet = EntityFactories.fetch(EntityType.PROJECTILE, ProjectilesFactory.BULLET)!!
        val impulseX =
            if (megaman.body.getX() > body.getX()) SHOOT_SPEED_X * ConstVals.PPM else -SHOOT_SPEED_X * ConstVals.PPM
        bullet.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.TRAJECTORY pairTo Vector2(impulseX, 0f)
            )
        )
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.ENEMY_BULLET_SOUND, false)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.6875f * ConstVals.PPM, 0.75f * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().set(body))
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().set(body))
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().set(body))
        body.addFixture(damageableFixture)

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(1.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putPreProcess { _, _ ->
            sprite.hidden = damageBlink
            val bodyPosition = body.getPositionPoint(Position.BOTTOM_CENTER)
            sprite.setPosition(bodyPosition, Position.BOTTOM_CENTER)
            sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { if (crouchTimer.isFinished()) "shoot" else "launch" }
        val animations = objectMapOf<String, IAnimation>(
            "shoot" pairTo Animation(shootRegion!!, 1, 2, 0.1f, true),
            "launch" pairTo Animation(launchRegion!!)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
