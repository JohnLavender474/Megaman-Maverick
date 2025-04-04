package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
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
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.projectiles.CaveRock
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class CaveRocker(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "CaveRocker"
        private var standingRegion: TextureRegion? = null
        private var throwingRegion: TextureRegion? = null
        private const val WAIT_DURATION = 1.25f
        private const val ROCK_IMPULSE_X = 11f
        private const val ROCK_IMPULSE_Y = 9f
    }

    override lateinit var facing: Facing

    private val waitTimer = Timer(WAIT_DURATION)
    private var throwing = false
    private var newRock: CaveRock? = null
    private var newRockOffsetY = 0f

    override fun init() {
        super.init()
        if (standingRegion == null || throwingRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            standingRegion = atlas.findRegion("$TAG/Stand")
            throwingRegion = atlas.findRegion("$TAG/Throw")
        }
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)
        newRockOffsetY = spawnProps.get(ConstKeys.OFFSET_Y, Float::class)!!
        waitTimer.reset()
        throwing = false
        facing = if (megaman.body.getX() < body.getX()) Facing.LEFT else Facing.RIGHT
    }

    override fun onDestroy() {
        super.onDestroy()
        newRock?.destroy()
        newRock = null
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            facing = if (megaman.body.getX() >= body.getX()) Facing.RIGHT else Facing.LEFT
            waitTimer.update(it)
            if (waitTimer.isJustFinished()) {
                throwRock()
                spawnNewRock()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat(), 1.5f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(
            body, FixtureType.BODY, GameRectangle().setSize(1.25f * ConstVals.PPM, 1.5f * ConstVals.PPM)
        )
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture}

        val damagerFixture = Fixture(
            body, FixtureType.DAMAGER, GameRectangle().setSize(1.25f * ConstVals.PPM, 1.5f * ConstVals.PPM)
        )
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture}

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE, GameRectangle().setSize(1.25f * ConstVals.PPM))
        body.addFixture(damageableFixture)
        debugShapes.add { damageableFixture}

        val headFixture = Fixture(
            body, FixtureType.HEAD, GameRectangle().setSize(0.5f * ConstVals.PPM, 0.1f * ConstVals.PPM)
        )
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        body.addFixture(headFixture)
        debugShapes.add { headFixture}

        body.preProcess.put(ConstKeys.DEFAULT) {
            newRock?.let {
                if (throwing && it.dead) {
                    GameLogger.debug(TAG, "New rock died before reaching cave rocker, so spawning a new one")
                    spawnNewRock()
                } else if (it.body.getBounds().overlaps(headFixture.getShape()) ||
                    it.body.getY() < headFixture.getShape().getY()
                ) {
                    GameLogger.debug(
                        TAG,
                        "New rock landed on cave rocker's head. Setting [throwing] to false and resetting wait timer"
                    )
                    it.destroy()
                    throwing = false
                    newRock = null
                    waitTimer.reset()
                }
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(3f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.hidden = damageBlink
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { if (throwing) "throw" else "stand" }
        val animations = objectMapOf<String, IAnimation>(
            "stand" pairTo Animation(standingRegion!!, 1, 2, gdxArrayOf(0.5f, 0.15f), true),
            "throw" pairTo Animation(throwingRegion!!, 1, 3, 0.05f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun throwRock() {
        throwing = true

        val impulse = MegaUtilMethods.calculateJumpImpulse(
            body.getPositionPoint(Position.TOP_CENTER),
            megaman.body.getCenter(),
            ROCK_IMPULSE_Y * ConstVals.PPM
        )
        impulse.x = impulse.x.coerceIn(-ROCK_IMPULSE_X * ConstVals.PPM, ROCK_IMPULSE_X * ConstVals.PPM)

        val caveRockToThrow = MegaEntityFactory.fetch(CaveRock::class)!!
        caveRockToThrow.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo body.getPositionPoint(Position.TOP_CENTER),
                ConstKeys.IMPULSE pairTo impulse,
                ConstKeys.PASS_THROUGH pairTo false
            )
        )
    }

    private fun spawnNewRock() {
        val spawn = body.getCenter().add(0f, newRockOffsetY * ConstVals.PPM)
        val impulse = GameObjectPools.fetch(Vector2::class).set(0f, -ROCK_IMPULSE_Y * ConstVals.PPM)

        newRock = MegaEntityFactory.fetch(CaveRock::class)!!
        newRock!!.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.POSITION pairTo spawn,
                ConstKeys.IMPULSE pairTo impulse,
                ConstKeys.PASS_THROUGH pairTo true
            )
        )
    }
}
