package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.*
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getOpposingPosition
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getPositionPoint

class FlameThrower(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    ICullableEntity, IAudioEntity, IDirectional, IDamager, IHazard {

    companion object {
        const val TAG = "FlameThrower"
        private const val COOL_DUR = 0.625f
        private const val BLINK_DUR = 0.625f
        private const val FLAME_THROW_DUR = 1.25f
        private const val DAMAGER_FIXTURE_VERT_OFFSET = 2f
        private const val DAMAGER_FIXTURE_HORIZ_OFFSET = 2f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class FlameThrowerState { COOL, BLINK, HOT }

    override var direction = Direction.UP

    private val loop = Loop(FlameThrowerState.entries.toTypedArray().toGdxArray(), false)
    private val initDelayTimer = Timer(0f)
    private val coolTimer = Timer(COOL_DUR)
    private val blinkTimer = Timer(BLINK_DUR)
    private val flameThrowTimer = Timer(FLAME_THROW_DUR)

    override fun getType() = EntityType.HAZARD

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_1.source)
            regions.put("flame_column", atlas.findRegion("FlameColumn"))
            regions.put("cool_thrower", atlas.findRegion("$TAG/Cool"))
            regions.put("blink_thrower", atlas.findRegion("$TAG/Blink"))
            regions.put("hot_thrower", atlas.findRegion("$TAG/Hot"))
        }
        addComponent(defineUpdatablesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(AudioComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        direction =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class).uppercase())

        val size = GameObjectPools.fetch(Vector2::class)
        body.setSize(
            (if (direction.isHorizontal()) size.set(1f, 0.75f)
            else size.set(0.75f, 1f)).scl(ConstVals.PPM.toFloat())
        )

        val position = direction.getOpposingPosition()
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(position)
        body.positionOnPoint(spawn, position)

        val initDelay = spawnProps.getOrDefault(ConstKeys.DELAY, 0f, Float::class)
        initDelayTimer.resetDuration(initDelay)

        loop.reset()

        coolTimer.reset()
        blinkTimer.reset()
        flameThrowTimer.reset()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        initDelayTimer.update(delta)
        if (!initDelayTimer.isFinished()) return@UpdatablesComponent

        when (loop.getCurrent()) {
            FlameThrowerState.COOL -> {
                coolTimer.update(delta)
                if (coolTimer.isFinished()) {
                    coolTimer.reset()
                    loop.next()
                }
            }

            FlameThrowerState.BLINK -> {
                blinkTimer.update(delta)
                if (blinkTimer.isFinished()) {
                    blinkTimer.reset()
                    loop.next()

                    if (overlapsGameCamera()) requestToPlaySound(SoundAsset.FLAMETHROWER_SOUND, false)
                }
            }

            FlameThrowerState.HOT -> {
                flameThrowTimer.update(delta)
                if (flameThrowTimer.isFinished()) {
                    flameThrowTimer.reset()
                    loop.next()
                }
            }
        }
    })

    private fun defineCullablesComponent(): CullablesComponent {
        val cullOnEvents = getStandardEventCullingLogic(this, objectSetOf(EventType.BEGIN_ROOM_TRANS))
        return CullablesComponent(objectMapOf(ConstKeys.CULL_EVENTS pairTo cullOnEvents))
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        val debugShapes = Array<() -> IDrawableShape?>()

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val size = GameObjectPools.fetch(Vector2::class)

            val damagerBounds = damagerFixture.rawShape as GameRectangle
            damagerBounds.setSize(
                (if (direction.isHorizontal()) size.set(3f, 0.75f)
                else size.set(0.75f, 3f)).scl(ConstVals.PPM.toFloat())
            )

            damagerFixture.setActive(loop.getCurrent() == FlameThrowerState.HOT)
            damagerFixture.drawingColor = if (damagerFixture.isActive()) Color.RED else Color.YELLOW

            damagerFixture.offsetFromBodyAttachment = (when (direction) {
                Direction.UP -> Vector2(0f, DAMAGER_FIXTURE_VERT_OFFSET)
                Direction.DOWN -> Vector2(0f, -DAMAGER_FIXTURE_VERT_OFFSET)
                Direction.LEFT -> Vector2(-DAMAGER_FIXTURE_HORIZ_OFFSET, 0.1f)
                Direction.RIGHT -> Vector2(DAMAGER_FIXTURE_HORIZ_OFFSET, 0.1f)
            }).scl(ConstVals.PPM.toFloat())
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprites = OrderedMap<Any, GameSprite>()

        val throwerSprite = GameSprite()
        throwerSprite.setSize(1.25f * ConstVals.PPM)
        sprites.put("thrower", throwerSprite)

        val flameColumnSprite = GameSprite()
        flameColumnSprite.setSize(ConstVals.PPM.toFloat(), 3.75f * ConstVals.PPM)
        sprites.put("flameColumn", flameColumnSprite)

        val spritesComponent = SpritesComponent(sprites)
        spritesComponent.putUpdateFunction("thrower") { _, sprite ->
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
            val position = DirectionPositionMapper.getInvertedPosition(direction)
            sprite.setPosition(body.getPositionPoint(position), position)
        }
        spritesComponent.putUpdateFunction("flameColumn") { _, sprite ->
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
            var position = DirectionPositionMapper.getInvertedPosition(direction)
            val offset = (when (direction) {
                Direction.UP -> Vector2(0f, 1.15f)
                Direction.DOWN -> Vector2(0f, -1.15f)
                Direction.LEFT -> Vector2(-1.65f, 0f)
                Direction.RIGHT -> Vector2(1.65f, 0f)
            }).scl(ConstVals.PPM.toFloat())
            val bodyPosition =
                body.getPositionPoint(if (direction.isVertical()) position else position.opposite())
            sprite.setPosition(bodyPosition, position, offset)
            sprite.hidden = loop.getCurrent() != FlameThrowerState.HOT
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val flameColumnAnim = Animation(regions.get("flame_column"), 2, 3, 0.05f, true)
        val flameColumnAnimator = Animator(flameColumnAnim)

        val throwerKeySupplier: () -> String? = { loop.getCurrent().name }
        val throwerAnims = objectMapOf<String, IAnimation>(
            FlameThrowerState.COOL.name pairTo Animation(regions.get("cool_thrower")),
            FlameThrowerState.BLINK.name pairTo Animation(regions.get("blink_thrower"), 1, 2, 0.1f, true),
            FlameThrowerState.HOT.name pairTo Animation(regions.get("hot_thrower"))
        )
        val throwerAnimator = Animator(throwerKeySupplier, throwerAnims)

        return AnimationsComponent(
            gdxArrayOf(
                { sprites.get("flameColumn") } pairTo flameColumnAnimator,
                { sprites.get("thrower") } pairTo throwerAnimator
            )
        )
    }
}
