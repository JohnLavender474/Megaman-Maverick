package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.IActivatable
import com.mega.game.engine.common.interfaces.UpdateFunction
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.bosses.WilyFinalBoss
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodyFixtureDef
import com.megaman.maverick.game.world.body.FixtureType

class WilyDeathPlaneLazor(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity,
    IHazard, IDamager, IOwnable<WilyFinalBoss>, IActivatable {

    companion object {
        const val TAG = "WilyDeathPlaneLazor"

        private const val MIN_FRAME = 1
        private const val MAX_FRAME = 5

        private const val FRAME_DUR = 0.05f

        private const val FRAME_ROWS = 2
        private const val FRAME_WIDTH = 2f
        private const val FRAME_HEIGHT = 6f

        private const val BODY_WIDTH = FRAME_WIDTH
        private const val BODY_HEIGHT = FRAME_ROWS * FRAME_HEIGHT

        private val regions = ObjectMap<Int, TextureRegion>()
    }

    override var owner: WilyFinalBoss? = null
    override var on = false

    private var currentFrame = MIN_FRAME
    private val frameTimer = Timer(FRAME_DUR)

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.HAZARDS_2.source)
            for (i in 1..5) regions.put(i, atlas.findRegion("$TAG/$i"))
        }
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER, WilyFinalBoss::class)!!

        currentFrame = MIN_FRAME
        frameTimer.reset()

        on = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        owner = null
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        frameTimer.update(delta)
        if (frameTimer.isFinished()) {
            currentFrame++
            if (currentFrame > MAX_FRAME) currentFrame = MIN_FRAME

            frameTimer.reset()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.preProcess.put(ConstKeys.DEFAULT) { body.forEachFixture { it.setActive(on) } }
        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.DAMAGER))
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprites = OrderedMap<Any, GameSprite>()
        val preProcessFuncs = OrderedMap<Any, UpdateFunction<GameSprite>>()

        for (i in 0 until FRAME_ROWS) {
            val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 4))
            sprite.setSize(FRAME_WIDTH * ConstVals.PPM, FRAME_HEIGHT * ConstVals.PPM)
            sprites.put(i, sprite)

            preProcessFuncs.put(i) { _, _ ->
                sprite.hidden = !on

                val x = body.getX()
                val y = body.getY() + i * FRAME_HEIGHT * ConstVals.PPM
                sprite.setPosition(x, y)

                sprite.setRegion(regions.get(currentFrame))
            }
        }

        return SpritesComponent(sprites = sprites, preProcessFuncs = preProcessFuncs)
    }

    override fun getType() = EntityType.HAZARD
}
