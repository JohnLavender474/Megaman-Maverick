package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.audio.AudioComponent
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.*
import com.engine.common.objects.Loop
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.cullables.CullableOnEvent
import com.engine.cullables.CullablesComponent
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.contracts.*
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
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.utils.overlapsGameCamera
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.getOpposingPosition
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType

class FlameThrower(game: MegamanMaverickGame) : GameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    ICullableEntity, IAudioEntity, IDirectionRotatable, IDamager, IHazard {

    companion object {
        const val TAG = "FlameThrower"
        private const val COOL_DUR = 0.625f
        private const val BLINK_DUR = 0.625f
        private const val FLAME_THROW_DUR = 1.25f
        private const val DAMAGER_FIXTURE_VERT_OFFSET = 2f
        private const val DAMAGER_FIXTURE_HORIZ_OFFSET = 2f
        private const val FLAME_COLUMN_HORIZ_OFFSET = 1.85f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class FlameThrowerState {
        COOL, BLINK, HOT
    }

    override lateinit var directionRotation: Direction

    private val loop = Loop(FlameThrowerState.values().toGdxArray(), false)
    private val initDelayTimer = Timer(0f)
    private val coolTimer = Timer(COOL_DUR)
    private val blinkTimer = Timer(BLINK_DUR)
    private val flameThrowTimer = Timer(FLAME_THROW_DUR)

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
        addComponent(AudioComponent(this))
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        directionRotation =
            Direction.valueOf(spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class).uppercase())

        body.setSize(
            (if (directionRotation.isHorizontal()) Vector2(1f, 0.75f) else Vector2(0.75f, 1f))
                .scl(ConstVals.PPM.toFloat())
        )

        val position = directionRotation.getOpposingPosition()
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(position)
        body.positionOnPoint(spawn, position)

        val initDelay = spawnProps.getOrDefault(ConstKeys.DELAY, 0f, Float::class)
        initDelayTimer.resetDuration(initDelay)

        loop.reset()
        coolTimer.reset()
        blinkTimer.setToEnd()
        flameThrowTimer.setToEnd()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, { delta ->
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
        val cullEvents = objectSetOf<Any>(EventType.BEGIN_ROOM_TRANS)
        val cullOnEvent = CullableOnEvent({ cullEvents.contains(it) })
        return CullablesComponent(this, objectMapOf(ConstKeys.CULL_EVENTS to cullOnEvent))
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        val debugShapes = Array<() -> IDrawableShape?>()

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle())
        body.addFixture(damagerFixture)
        debugShapes.add { damagerFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val damagerBounds = damagerFixture.rawShape as GameRectangle
            damagerBounds.setSize(
                (if (directionRotation.isHorizontal()) Vector2(3f, 0.75f) else Vector2(0.75f, 3f))
                    .scl(ConstVals.PPM.toFloat())
            )

            damagerFixture.active = loop.getCurrent() == FlameThrowerState.HOT
            damagerBounds.color = if (damagerFixture.active) Color.RED else Color.YELLOW

            damagerFixture.offsetFromBodyCenter = (when (directionRotation) {
                Direction.UP -> Vector2(0f, DAMAGER_FIXTURE_VERT_OFFSET)
                Direction.DOWN -> Vector2(0f, -DAMAGER_FIXTURE_VERT_OFFSET)
                Direction.LEFT -> Vector2(-DAMAGER_FIXTURE_HORIZ_OFFSET, 0.1f)
                Direction.RIGHT -> Vector2(DAMAGER_FIXTURE_HORIZ_OFFSET, 0.1f)
            }).scl(ConstVals.PPM.toFloat())
        }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprites = OrderedMap<String, GameSprite>()

        val throwerSprite = GameSprite()
        throwerSprite.setSize(1.15f * ConstVals.PPM)
        sprites.put("thrower", throwerSprite)

        val flameColumnSprite = GameSprite()
        flameColumnSprite.setSize(0.75f * ConstVals.PPM, 3.5f * ConstVals.PPM)
        sprites.put("flameColumn", flameColumnSprite)

        val spritesComponent = SpritesComponent(this, sprites)
        spritesComponent.putUpdateFunction("thrower") { _, _sprite ->
            _sprite.setOriginCenter()
            _sprite.rotation = directionRotation.rotation
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
        }
        spritesComponent.putUpdateFunction("flameColumn") { _, _sprite ->
            _sprite.setOriginCenter()
            _sprite.rotation = directionRotation.rotation
            val position = directionRotation.getOpposingPosition()
            val offset = (when (directionRotation) {
                Direction.UP -> Vector2(0f, 1.15f)
                Direction.DOWN -> Vector2(0f, -0.85f)
                Direction.LEFT -> Vector2(-FLAME_COLUMN_HORIZ_OFFSET, 0.2f)
                Direction.RIGHT -> Vector2(FLAME_COLUMN_HORIZ_OFFSET, 0.2f)
            }).scl(ConstVals.PPM.toFloat())
            _sprite.setPosition(body.getPositionPoint(position), position, offset)
            _sprite.hidden = loop.getCurrent() != FlameThrowerState.HOT
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val flameColumnAnim = Animation(regions.get("flame_column"), 2, 3, 0.05f, true)
        val flameColumnAnimator = Animator(flameColumnAnim)

        val throwerKeySupplier: () -> String? = {
            loop.getCurrent().name
        }
        val throwerAnims = objectMapOf<String, IAnimation>(
            FlameThrowerState.COOL.name to Animation(regions.get("cool_thrower")),
            FlameThrowerState.BLINK.name to Animation(regions.get("blink_thrower"), 1, 2, 0.1f, true),
            FlameThrowerState.HOT.name to Animation(regions.get("hot_thrower"))
        )
        val throwerAnimator = Animator(throwerKeySupplier, throwerAnims)

        return AnimationsComponent(this, gdxArrayOf(
            { sprites.get("flameColumn") } to flameColumnAnimator,
            { sprites.get("thrower") } to throwerAnimator
        ))
    }
}