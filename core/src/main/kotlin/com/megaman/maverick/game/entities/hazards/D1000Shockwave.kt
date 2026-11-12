package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Facing
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
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.TickFrames
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.entities.bosses.d1000.D1000Frames

class D1000Shockwave(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    IDamagerEntity, IHazard {

    companion object {
        const val TAG = "D1000Shockwave"

        private const val ART = "pillar_base"
        const val LENGTH = 1f * ConstVals.PPM
        const val THICKNESS = 0.75f * ConstVals.PPM
        const val SPEED = 7f * ConstVals.PPM
        private const val DAMAGE = 2
    }

    private enum class D1000ShockwavePhase { FLOOR, WALL }

    private val arena = GameRectangle()
    val center = Vector2()
    private val damagerShape = GameRectangle()
    lateinit var direction: Facing
        private set
    private var phase = D1000ShockwavePhase.FLOOR

    val onFloor: Boolean
        get() = phase == D1000ShockwavePhase.FLOOR

    private val animations = ObjectMap<String, IAnimation>()

    override fun init(vararg params: Any) {
        super.init()
        GameLogger.debug(TAG, "init()")
        val atlas = services.assets.getTextureAtlas(TextureAsset.BOSSES_3.source)
        TickFrames.animation(atlas, "${D1000Pillar.TAG}/$ART", ART, D1000Frames.PILLAR[ART]!!, animations)
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(AnimationsComponent(this, Animator(animations[ART])))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        arena.set(spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!)
        direction = spawnProps.get(ConstKeys.FACING, Facing::class)!!
        val start = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        center.set(start.x, arena.getY() + THICKNESS / 2f)
        phase = D1000ShockwavePhase.FLOOR
        placeBody()
    }

    override fun getDamageToMegaman() = DAMAGE

    private fun wallX() =
        if (direction == Facing.RIGHT) arena.getMaxX() - THICKNESS / 2f else arena.getX() + THICKNESS / 2f

    private fun placeBody() {
        val width = if (phase == D1000ShockwavePhase.FLOOR) LENGTH else THICKNESS
        val height = if (phase == D1000ShockwavePhase.FLOOR) THICKNESS else LENGTH
        body.setSize(width, height)
        damagerShape.setSize(width, height)
        body.setCenter(center)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        when (phase) {
            D1000ShockwavePhase.FLOOR -> {
                center.x += SPEED * direction.value * delta
                val wall = wallX()
                if ((direction == Facing.RIGHT && center.x >= wall) || (direction == Facing.LEFT && center.x <= wall)) {
                    center.set(wall, arena.getY() + LENGTH / 2f)
                    phase = D1000ShockwavePhase.WALL
                }
            }

            D1000ShockwavePhase.WALL -> {
                center.y += SPEED * delta
                if (center.y + LENGTH / 2f >= arena.getMaxY()) destroy()
            }
        }
        placeBody()
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.setSize(LENGTH, THICKNESS)
        damagerShape.setSize(LENGTH, THICKNESS)
        body.addFixture(Fixture(body, FixtureType.DAMAGER, damagerShape))
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))
        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1))
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val def = D1000Frames.PILLAR[ART]!!
            val width = def.width * TickFrames.SCALE
            val height = def.height * TickFrames.SCALE
            sprite.setSize(width, height)
            sprite.setOriginCenter()
            sprite.setFlip(direction == Facing.LEFT, false)
            if (phase == D1000ShockwavePhase.FLOOR) {
                sprite.rotation = 0f
                sprite.setCenter(center.x, arena.getY() + height / 2f)
            } else {
                sprite.rotation = if (direction == Facing.RIGHT) 90f else -90f
                val x = if (direction == Facing.RIGHT) arena.getMaxX() - height / 2f else arena.getX() + height / 2f
                sprite.setCenter(x, center.y)
            }
        }
        return component
    }

    override fun getType() = EntityType.HAZARD
}
