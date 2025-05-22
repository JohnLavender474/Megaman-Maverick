package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.ICullable
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
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
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.projectiles.DeathBomb
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class Jetto(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable, ICullable {

    companion object {
        const val TAG = "Jetto"
        private const val SPEED = 12f
        private const val CULL_TIME = 0.5f
        private const val BOMB_DROP_Y = -8f
        private const val DROP_BOMB_DELAY = 0.25f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class JettoColor { GRAY, BRONZE }

    override lateinit var facing: Facing

    private var passes = 0
    private lateinit var color: JettoColor
    private val cullTimer = Timer(CULL_TIME)
    private val dropBombDelay = Timer(DROP_BOMB_DELAY)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            JettoColor.entries.map { it.name.lowercase() }.forEach { regions.put(it, atlas.findRegion("$TAG/$it")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val center = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(center)

        FacingUtils.setFacingOf(this)

        color = JettoColor.valueOf(spawnProps.get(ConstKeys.COLOR, String::class)!!.uppercase())
        requestToPlaySound(SoundAsset.JET_SOUND, false)
        dropBombDelay.setToEnd()
        cullTimer.reset()
        passes = 1
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun shouldBeCulled(delta: Float) = passes >= 2 && cullTimer.isFinished()

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            body.physics.velocity.x = SPEED * ConstVals.PPM * facing.value

            if (!body.getBounds().overlaps(game.getGameCamera().getRotatedBounds())) cullTimer.update(delta)
            else cullTimer.reset()

            if (passes == 1) {
                if (cullTimer.isFinished()) {
                    val centerX: Float
                    val facing: Facing
                    if (body.getBounds().getMaxX() < game.getGameCamera().getRotatedBounds().getX()) {
                        centerX = game.getGameCamera().getRotatedBounds().getX()
                        facing = Facing.RIGHT
                    } else {
                        centerX = game.getGameCamera().getRotatedBounds().getMaxX()
                        facing = Facing.LEFT
                    }
                    body.setCenterX(centerX)
                    this.facing = facing

                    passes++
                    cullTimer.reset()
                    requestToPlaySound(SoundAsset.JET_SOUND, false)
                }
            } else {
                dropBombDelay.update(delta)
                if (dropBombDelay.isFinished()) {
                    dropBomb()
                    dropBombDelay.reset()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(3f * ConstVals.PPM, ConstVals.PPM.toFloat())
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val leftFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.75f * ConstVals.PPM))
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftFixture }

        val rightFixture =
            Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM, 0.75f * ConstVals.PPM))
        rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.YELLOW
        debugShapes.add { rightFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (FacingUtils.isFacingBlock(this)) {
                explode()
                destroy()
            }
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.DAMAGER, FixtureType.SHIELD))
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(4f * ConstVals.PPM, 2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            val position = if (isFacing(Facing.LEFT)) Position.CENTER_LEFT else Position.CENTER_RIGHT
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.translateX(0.5f * ConstVals.PPM * facing.value)
            sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { color.name.lowercase() }
                .applyToAnimations { animations ->
                    AnimationUtils.loadAnimationDef(
                        AnimationDef(3, 1, 0.1f, true),
                        animations,
                        regions,
                        JettoColor.entries.map { it.name.lowercase() },
                    )
                }
                .build()
        )
        .build()

    private fun dropBomb() {
        GameLogger.debug(TAG, "dropBomb()")

        val position = body.getCenter()
            .add(0.75f * ConstVals.PPM * facing.value, -0.75f * ConstVals.PPM)

        val impulse = GameObjectPools.fetch(Vector2::class)
            .set(0f, BOMB_DROP_Y)
            .scl(ConstVals.PPM.toFloat())

        val bomb = MegaEntityFactory.fetch(DeathBomb::class)!!
        bomb.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.IMPULSE pairTo impulse,
                ConstKeys.POSITION pairTo position
            )
        )
    }
}
