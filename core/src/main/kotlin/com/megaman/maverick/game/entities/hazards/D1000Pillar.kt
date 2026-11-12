package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.AnimationsComponentBuilder
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.entities.utils.TickFrames
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import com.megaman.maverick.game.entities.bosses.d1000.D1000Frames

class D1000Pillar(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    IDamagerEntity, IHazard {

    companion object {
        const val TAG = "D1000Pillar"

        private const val BASE = "base"
        private const val SEGMENT_ROWS = 32
        private const val SEGMENT_HEIGHT = SEGMENT_ROWS * TickFrames.SCALE
        private const val MAX_SEGMENTS = 16
        private val SEGMENTS = Array(MAX_SEGMENTS) { "pillar_$it" }

        const val WIDTH = 1f * ConstVals.PPM
        private const val BASE_ABOVE_FLOOR = 0.25f * ConstVals.PPM
        const val ACTIVE_DUR = 0.85f
        val START_DUR = TickFrames.seconds(D1000Frames.PILLAR["pillar_start"]!!.ticks.sum())
        private const val DAMAGE = 2
    }

    private enum class D1000PillarState { START, ON, END }

    private val floor = Vector2()
    private var ceiling = 0f
    private var far = 0f

    var horizontal = false
        private set
    private lateinit var damager: Fixture
    private var state = D1000PillarState.START
    private var time = 0f

    private val animations = ObjectMap<String, IAnimation>()
    private lateinit var pillarAnimator: Animator

    override fun init(vararg params: Any) {
        super.init()
        GameLogger.debug(TAG, "init()")
        val atlas = services.assets.getTextureAtlas(TextureAsset.BOSSES_3.source)
        TickFrames.animations(atlas, TAG, D1000Frames.PILLAR, animations)
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        floor.set(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)
        horizontal = !spawnProps.getOrDefault(ConstKeys.VERTICAL, true, Boolean::class)
        if (horizontal) {
            far = spawnProps.get(ConstKeys.TARGET, Float::class)!!
            val length = abs(far - floor.x)
            if (length <= 0f || length > MAX_SEGMENTS * SEGMENT_HEIGHT) throw IllegalStateException(
                "$TAG: the far wall $far must be away from the wall ${floor.x} by at most ${MAX_SEGMENTS * SEGMENT_HEIGHT}"
            )
            body.setSize(length, WIDTH)
            (damager.rawShape as GameRectangle).setSize(length, WIDTH)
            body.setCenter(min(floor.x, far) + length / 2f, floor.y)
        } else {
            ceiling = spawnProps.get(ConstKeys.MAX_Y, Float::class)!!
            val height = ceiling - floor.y
            if (height <= 0f || height > MAX_SEGMENTS * SEGMENT_HEIGHT) throw IllegalStateException(
                "$TAG: the ceiling $ceiling must be above the floor ${floor.y} by at most ${MAX_SEGMENTS * SEGMENT_HEIGHT}"
            )
            body.setSize(WIDTH, height)
            (damager.rawShape as GameRectangle).setSize(WIDTH, height)
            body.setBottomCenterToPoint(floor)
        }
        state = D1000PillarState.START
        time = 0f
        services.audio.playSound(SoundAsset.ELECTRIC_2_SOUND, false)
    }

    val baseX: Float
        get() = floor.x

    val baseY: Float
        get() = floor.y

    val dangerIn: Float
        get() = when (state) {
            D1000PillarState.START -> max(START_DUR - time, 0f)
            D1000PillarState.ON -> 0f
            D1000PillarState.END -> Float.POSITIVE_INFINITY
        }

    val dangerUntil: Float
        get() = when (state) {
            D1000PillarState.START -> max(START_DUR - time, 0f) + ACTIVE_DUR
            D1000PillarState.ON -> ACTIVE_DUR - time
            D1000PillarState.END -> -1f
        }

    override fun getDamageToMegaman() = DAMAGE

    private fun animationDone(key: String) =
        pillarAnimator.currentKey == key && pillarAnimator.currentAnimation?.isFinished() == true

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        time += delta
        when (state) {
            D1000PillarState.START -> if (animationDone("pillar_start")) {
                state = D1000PillarState.ON
                time = 0f
            }

            D1000PillarState.ON -> if (time >= ACTIVE_DUR) {
                state = D1000PillarState.END
                time = 0f
            }

            D1000PillarState.END -> if (animationDone("pillar_end")) destroy()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(WIDTH, WIDTH)

        damager = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(WIDTH, WIDTH))
        body.addFixture(damager)

        body.preProcess.put(ConstKeys.DEFAULT) { damager.setActive(state == D1000PillarState.ON) }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun placeSegment(sprite: GameSprite, index: Int) {
        if (horizontal) {
            placeSideSegment(sprite, index)
            return
        }
        sprite.rotation = 0f
        val region = pillarAnimator.currentAnimation?.getCurrentRegion()
        val bottom = floor.y + index * SEGMENT_HEIGHT
        val height = min(SEGMENT_HEIGHT, ceiling - bottom)
        if (region == null || height <= 0f) {
            sprite.hidden = true
            return
        }
        sprite.hidden = false
        val def = D1000Frames.PILLAR[pillarAnimator.currentKey ?: "pillar_start"]
        val rows = (height / TickFrames.SCALE).roundToInt().coerceIn(1, SEGMENT_ROWS)
        sprite.setRegion(region, 0, 0, def.width, rows)
        sprite.setFlip(false, false)
        sprite.setBounds(floor.x - def.originX * TickFrames.SCALE, bottom, def.width * TickFrames.SCALE, height)
    }

    private fun placeSideSegment(sprite: GameSprite, index: Int) {
        val region = pillarAnimator.currentAnimation?.getCurrentRegion()
        val length = min(SEGMENT_HEIGHT, abs(far - floor.x) - index * SEGMENT_HEIGHT)
        if (region == null || length <= 0f) {
            sprite.hidden = true
            return
        }
        sprite.hidden = false
        val def = D1000Frames.PILLAR[pillarAnimator.currentKey ?: "pillar_start"]
        val rows = (length / TickFrames.SCALE).roundToInt().coerceIn(1, SEGMENT_ROWS)
        val width = def.width * TickFrames.SCALE
        val dir = if (far > floor.x) 1f else -1f
        sprite.setRegion(region, 0, 0, def.width, rows)
        sprite.setFlip(false, false)
        sprite.setBounds(floor.x + dir * index * SEGMENT_HEIGHT - width / 2f, floor.y, width, length)
        sprite.setOrigin(width / 2f, 0f)
        sprite.rotation = -90f * dir
    }

    private fun placeBase(sprite: GameSprite) {
        if (!horizontal) {
            sprite.rotation = 0f
            TickFrames.place(sprite, D1000Frames.PILLAR["pillar_base"], floor.x, floor.y + BASE_ABOVE_FLOOR, false)
            return
        }
        val dir = if (far > floor.x) 1f else -1f
        val def = D1000Frames.PILLAR["pillar_base"]
        TickFrames.place(sprite, def, floor.x + dir * BASE_ABOVE_FLOOR, floor.y, false)
        sprite.setOrigin(def.originX * TickFrames.SCALE, (def.height - def.originY) * TickFrames.SCALE)
        sprite.rotation = -90f * dir
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val builder = SpritesComponentBuilder()
        SEGMENTS.forEachIndexed { index, key ->
            builder.sprite(key, GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1)))
                .preProcess { _, sprite -> placeSegment(sprite, index) }
        }
        return builder.sprite(BASE, GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 2)))
        .preProcess { _, sprite -> placeBase(sprite) }
            .build()
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            when (state) {
                D1000PillarState.START -> "pillar_start"
                D1000PillarState.ON -> "pillar"
                D1000PillarState.END -> "pillar_end"
            }
        }
        pillarAnimator = Animator(keySupplier, animations)
        return AnimationsComponentBuilder(this)
            .key(SEGMENTS[0]).animator(pillarAnimator)
            .key(BASE).animator(Animator(animations["pillar_base"]))
            .build()
    }

    override fun getType() = EntityType.HAZARD
}
