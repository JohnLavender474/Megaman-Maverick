package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Direction
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
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.projectiles.SmallGreenMissile
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class ColtonJoe(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IDrawableShapesEntity, IFaceable {

    companion object {
        const val TAG = "ColtonJoe"
        private const val SHOOT_DUR = 1.5f
        private const val SHOOT_DELAY = 0.25f
        private const val BULLET_SPEED = 8f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var facing: Facing

    private val shootTimer = Timer(SHOOT_DUR)
    private val shootDelayTimer = Timer(SHOOT_DELAY)
    private lateinit var scanner: GameRectangle

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            gdxArrayOf("stand", "shoot").forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
        addDebugShapeSupplier { scanner }
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        scanner = GameRectangle().setSize(10f * ConstVals.PPM, ConstVals.PPM.toFloat())

        shootTimer.setToEnd()
        shootDelayTimer.setToEnd()

        facing = if (body.getX() < megaman.body.getX()) Facing.RIGHT else Facing.LEFT
    }

    private fun shoot() {
        val position = body.getCenter().add(0.75f * ConstVals.PPM * facing.value, 0.1f * ConstVals.PPM)

        val trajectory = GameObjectPools.fetch(Vector2::class).set(BULLET_SPEED * facing.value * ConstVals.PPM, 0f)

        val missile = MegaEntityFactory.fetch(SmallGreenMissile::class)!!
        missile.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.GRAVITY_ON pairTo false,
                ConstKeys.POSITION pairTo position,
                ConstKeys.TRAJECTORY pairTo trajectory,
                ConstKeys.DIRECTION pairTo if (isFacing(Facing.LEFT)) Direction.LEFT else Direction.RIGHT
            )
        )

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.BLAST_2_SOUND, false)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!shootTimer.isFinished()) {
                shootTimer.update(delta)
                return@add
            }

            val position = if (isFacing(Facing.LEFT)) Position.CENTER_RIGHT else Position.CENTER_LEFT
            scanner.positionOnPoint(body.getCenter(), position)

            if (shootDelayTimer.isFinished() && scanner.overlaps(megaman.body.getBounds())) shootDelayTimer.reset()

            if (!shootDelayTimer.isFinished()) {
                shootDelayTimer.update(delta)

                if (shootDelayTimer.isJustFinished()) {
                    shoot()
                    shootTimer.reset()
                }
            }

            facing = if (body.getX() < megaman.body.getX()) Facing.RIGHT else Facing.LEFT
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat(), 2f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(4f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.hidden = damageBlink
            sprite.setFlip(isFacing(Facing.RIGHT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { if (shootTimer.isFinished()) "stand" else "shoot" }
        val animations = objectMapOf<String, IAnimation>(
            "stand" pairTo Animation(regions.get("stand"), 2, 1, gdxArrayOf(0.75f, 0.15f), true),
            "shoot" pairTo Animation(regions.get("shoot"), 5, 1, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    override fun getTag() = TAG
}
