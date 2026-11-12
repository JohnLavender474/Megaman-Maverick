package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.AnimationsComponentBuilder
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.UtilMethods
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponentBuilder
import com.maverick.game.engine.entities.IGameEntity
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.hazards.D1000CeilingDebris
import com.megaman.maverick.game.entities.hazards.D1000DebrisDrop
import com.megaman.maverick.game.entities.utils.TickFrames
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import com.megaman.maverick.game.entities.bosses.d1000.D1000Frames
import com.megaman.maverick.game.entities.bosses.d1000.flickerOn
import com.megaman.maverick.game.entities.bosses.d1000.D1000Ceiling

class D1000DebrisWarning(services: MegaServices) : MegaGameEntity(services), ISpritesEntity, IAnimatedEntity {

    companion object {
        const val TAG = "D1000DebrisWarning"
        const val LEAD_DUR = 0.6f
        const val LOOSE_DUR = 0.15f
        const val HANG_DROP = 0.5f * ConstVals.PPM
        private const val DUST = "dust"
        private const val CHUNK = "chunk"
        private const val SHAKE_STEP = 1f * TickFrames.SCALE
        private const val WOBBLE_DEGREES = 15f
        private const val WOBBLE_HZ = 5f
        private val chunkRegions = ObjectMap<String, TextureRegion>()
    }

    val position = Vector2()
    val hangPoint = Vector2()
    val chunkPoint = Vector2()
    val pieces = Array<D1000DebrisDrop>()
    var ceiling = 0f
        private set
    var spread = 0f
        private set

    private val arena = GameRectangle()
    private var owner: IGameEntity? = null
    private var time = 0f
    private var next = 0

    val fallsIn: Float
        get() = max(LEAD_DUR - time, 0f)

    val chunkKey: String
        get() = pieces.first().key

    fun dropsIn(piece: D1000DebrisDrop) = max(LEAD_DUR + piece.delay - time, 0f)

    private val animations = ObjectMap<String, IAnimation>()

    override fun init(vararg params: Any) {
        super.init()
        GameLogger.debug(TAG, "init()")
        val atlas = services.assets.getTextureAtlas(TextureAsset.BOSSES_3.source)
        TickFrames.animations(atlas, TAG, D1000Frames.DUST, animations)
        if (chunkRegions.isEmpty) D1000CeilingDebris.SIZES.keys().forEach {
            chunkRegions.put(it, atlas.findRegion("${D1000CeilingDebris.TAG}/$it"))
        }
        addComponent(UpdatablesComponent({ delta -> update(delta) }))
        addComponent(defineSpritesComponent())
        addComponent(AnimationsComponentBuilder(this).key(DUST).animator(Animator(animations[DUST])).build())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        position.set(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)
        hangPoint.set(position.x, position.y - HANG_DROP)
        chunkPoint.set(position)
        ceiling = spawnProps.get(ConstKeys.MAX_Y, Float::class)!!
        arena.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)
        spread = spawnProps.getOrDefault(ConstKeys.WIDTH, 0f, Float::class)
        owner = spawnProps.get(ConstKeys.OWNER, IGameEntity::class)!!
        time = 0f
        next = 0
        D1000CeilingDebris.roll(
            pieces, position.x, spread, arena.getX() + D1000Ceiling.CEILING_POCKET, arena.getMaxX() - D1000Ceiling.CEILING_POCKET,
            hangPoint.y, arena.getY(), UtilMethods.random
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        owner = null
    }

    private fun update(delta: Float) {
        time += delta
        val loose = min(time / LOOSE_DUR, 1f)
        chunkPoint.set(position.x, position.y - HANG_DROP * (1f - (1f - loose) * (1f - loose)))
        val source = owner
        val dropping = source !is AbstractBoss || !source.defeated
        while (next < pieces.size && time >= LEAD_DUR + pieces[next].delay) {
            val piece = pieces[next++]
            piece.dropped = true
            if (dropping) drop(piece)
        }
        if (next >= pieces.size) destroy()
    }

    private fun drop(piece: D1000DebrisDrop) {
        val debris = MegaEntityFactory.fetch(D1000CeilingDebris::class)!!
        debris.spawn(
            props(
                ConstKeys.OWNER pairTo owner,
                ConstKeys.KEY pairTo piece.key,
                ConstKeys.POSITION pairTo Vector2(piece.startX, hangPoint.y),
                ConstKeys.TRAJECTORY pairTo Vector2(piece.velocity),
                ConstKeys.SPIN pairTo piece.spin,
                ConstKeys.BOUNDS pairTo GameRectangle(arena)
            )
        )
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(DUST, GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 0)))
        .preProcess { _, sprite -> TickFrames.place(sprite, D1000Frames.DUST[DUST], position.x, ceiling, false) }
        .sprite(CHUNK, GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1)))
        .preProcess { _, sprite ->
            sprite.hidden = next > 0
            val loosening = time < LOOSE_DUR
            val shake = if (!loosening) 0f else if (flickerOn(time)) SHAKE_STEP else -SHAKE_STEP
            val key = chunkKey
            val size = D1000CeilingDebris.size(key)
            sprite.setRegion(chunkRegions[key])
            sprite.setSize(size.x, size.y)
            sprite.setCenter(chunkPoint.x + shake, chunkPoint.y)
            sprite.setOriginCenter()
            sprite.rotation = if (loosening) 0f else WOBBLE_DEGREES * sin((time - LOOSE_DUR) * WOBBLE_HZ * MathUtils.PI2)
        }
        .build()

    override fun getType() = EntityType.DECORATION
}
