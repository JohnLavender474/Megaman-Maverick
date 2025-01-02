package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
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
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.EnemyDamageNegotiations
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

class Merserker(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "Merserker"
        private const val BODY_WIDTH = 1f
        private const val BODY_HEIGHT = 2f
        private const val SPRITE_SIZE = 2f
        private const val PRE_CHOP_DOWN_DELAY = 0.25f
        private const val ON_GROUND_DUR = 0.5f
        private val ANIM_DEFS = objectMapOf(
            MerserkerState.FLY pairTo AnimationDef(2, 1, 0.1f, true),
            MerserkerState.PRE_CHOP_DOWN pairTo AnimationDef(2, 1, 0.05f, true),
            MerserkerState.CHOP_DOWN pairTo AnimationDef(2, 1, 0.05f, true),
            MerserkerState.ON_GROUND pairTo AnimationDef(2, 1, 0.1f, true),
            MerserkerState.RETURN_UP pairTo AnimationDef(2, 1, 0.1f, true)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class MerserkerState { FLY, PRE_CHOP_DOWN, CHOP_DOWN, ON_GROUND, RETURN_UP }

    override val damageNegotiations = EnemyDamageNegotiations.getEnemyDmgNegs(Size.MEDIUM)
    override lateinit var facing: Facing

    private val loop = Loop(MerserkerState.entries.toGdxArray())
    private val state: MerserkerState
        get() = loop.getCurrent()
    private val timers = ObjectMap<MerserkerState, Timer>()

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            ANIM_DEFS.keys().forEach { regions.put(it.name.lowercase(), atlas.findRegion("$TAG/$it")) }
        }
        if (timers.isEmpty) {
            timers.put(MerserkerState.PRE_CHOP_DOWN, Timer(PRE_CHOP_DOWN_DELAY))
            timers.put(MerserkerState.ON_GROUND, Timer(ON_GROUND_DUR))
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val center = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(center)
        loop.reset()
    }

    private fun shouldChopDown() =
        megaman.body.getX() >= body.getX() &&
            megaman.body.getMaxX() <= body.getMaxX() &&
            megaman.getY() <= body.getMaxY()

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (state) {
                MerserkerState.FLY -> TODO()
                MerserkerState.PRE_CHOP_DOWN -> TODO()
                MerserkerState.CHOP_DOWN -> TODO()
                MerserkerState.ON_GROUND -> TODO()
                MerserkerState.RETURN_UP -> TODO()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.9f * body.getWidth(), 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        body.addFixture(feetFixture)

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().apply { setSize(SPRITE_SIZE * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.hidden = damageBlink
            sprite.setCenter(body.getCenter())
            sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { state.name.lowercase() }
                .addAnimations(
                    ObjectMap<String, IAnimation>().apply {
                        ANIM_DEFS.entries().forEach {
                            val key = it.key.name.lowercase()
                            val def = it.value
                            put(key, Animation(regions[key], def.rows, def.cols, def.durations, def.loop))
                        }
                    }
                )
                .build()
        )
        .build()
}
