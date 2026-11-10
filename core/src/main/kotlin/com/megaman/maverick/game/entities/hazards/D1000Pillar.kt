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
import com.megaman.maverick.game.entities.bosses.D1000Frames
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds

class D1000Pillar(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    IDamagerEntity, IHazard {

    companion object {
        const val TAG = "D1000Pillar"

        private const val PILLAR = "pillar"
        private const val BASE = "base"

        private const val WIDTH = 1f * ConstVals.PPM
        private const val HEIGHT = 6.4375f * ConstVals.PPM
        private const val ORIGIN_ABOVE_FLOOR = 2.4375f * ConstVals.PPM
        private const val BASE_ABOVE_FLOOR = 0.25f * ConstVals.PPM
        private const val ACTIVE_DUR = 0.85f
        private const val DAMAGE = 6
    }

    private enum class D1000PillarState { START, ON, END }

    private val floor = Vector2()
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
        body.setBottomCenterToPoint(floor)
        state = D1000PillarState.START
        time = 0f
        services.audio.playSound(SoundAsset.ELECTRIC_2_SOUND, false)
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
        body.setSize(WIDTH, HEIGHT)

        val damager = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(WIDTH, HEIGHT))
        body.addFixture(damager)

        body.preProcess.put(ConstKeys.DEFAULT) { damager.setActive(state == D1000PillarState.ON) }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ body.getBounds() }), debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(PILLAR, GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 1)))
        .preProcess { _, sprite ->
            val key = pillarAnimator.currentKey ?: "pillar_start"
            TickFrames.place(sprite, D1000Frames.PILLAR[key], floor.x, floor.y + ORIGIN_ABOVE_FLOOR, false)
        }
        .sprite(BASE, GameSprite(DrawingPriority(DrawingSection.FOREGROUND, 2)))
        .preProcess { _, sprite ->
            TickFrames.place(sprite, D1000Frames.PILLAR["pillar_base"], floor.x, floor.y + BASE_ABOVE_FLOOR, false)
        }
        .build()

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
            .key(PILLAR).animator(pillarAnimator)
            .key(BASE).animator(Animator(animations["pillar_base"]))
            .build()
    }

    override fun getType() = EntityType.HAZARD
}
