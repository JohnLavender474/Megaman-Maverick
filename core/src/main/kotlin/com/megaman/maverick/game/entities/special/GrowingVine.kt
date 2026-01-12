package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.utils.DrawableShapesComponentBuilder
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds

class GrowingVine(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity,
    IAnimatedEntity, IAudioEntity {

    companion object {
        const val TAG = "GrowingVine"
        private const val GROW_DUR = 0.05f
        private var region: TextureRegion? = null
    }

    enum class GrowingVineState { IDLE, GROWING, GROWN }

    private var state = GrowingVineState.IDLE
        set(value) {
            field = value
            if (value == GrowingVineState.GROWING)
                requestToPlaySound(SoundAsset.SMB3_BEANSTALK_SOUND, false)
        }

    private var up = true

    private var ladderIndex = -1
    private val ladder = GameRectangle()
    private val ladderTiles = Array<GameRectangle>()

    private val growTimer = Timer(GROW_DUR)

    private val tempMatrix = Matrix<GameRectangle>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null)
            region = game.assMan.getTextureRegion(TextureAsset.SMB3_SPECIALS.source, "$TAG/vine")
        super.init()
        addComponent(AudioComponent())
        addComponent(SpritesComponent())
        addComponent(AnimationsComponent())
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        body.set(bounds)

        state = spawnProps.getOrDefault(ConstKeys.STATE, GrowingVineState.IDLE, GrowingVineState::class)

        up = spawnProps.getOrDefault(ConstKeys.UP, true, Boolean::class)

        ladderIndex = if (up) -1 else ladderTiles.size
        ladder.set(0f, 0f, 0f, 0f)
        ladderTiles.clear()

        val matrix = bounds.splitByCellSize(ConstVals.PPM.toFloat(), tempMatrix)
        for (row in 0 until matrix.rows) ladderTiles.add(matrix[0, row])

        defineDrawables()

        growTimer.setToEnd()
    }

    private fun defineDrawables() {
        for (i in 0 until ladderTiles.size) {
            val ladderTile = ladderTiles[i]

            val sprite = GameSprite()
            sprite.setSize(ConstVals.PPM.toFloat())
            putSprite(i, sprite)

            putSpritePreProcess(i) { _, sprite ->
                sprite.setCenter(ladderTile.getCenter())
                sprite.hidden = when {
                    up -> i >= ladderIndex
                    else -> i <= ladderIndex
                }
            }

            val animation = Animation(region!!, 2, 1, gdxArrayOf(0.5f, 0.1f), true)
            val animator = Animator(animation)
            putAnimator(i, sprite, animator)
        }
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (state == GrowingVineState.GROWING) {
            growTimer.update(delta)
            if (growTimer.isFinished()) {
                if (up) {
                    ladderIndex += 1
                    if (ladderIndex >= ladderTiles.size) state = GrowingVineState.GROWN
                } else {
                    ladderIndex -= 1
                    if (ladderIndex <= 0) state = GrowingVineState.GROWN
                }

                growTimer.reset()
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val drawablesComponent = DrawableShapesComponentBuilder()
        addComponent(drawablesComponent.build())

        val ladderFixture = Fixture(body, FixtureType.LADDER, ladder)
        ladderFixture.attachedToBody = false
        body.addFixture(ladderFixture)
        ladderFixture.drawingColor = Color.GREEN
        drawablesComponent.addDebug { ladderFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            when (state) {
                GrowingVineState.IDLE -> {
                    ladder.set(0f, 0f, 0f, 0f)
                    ladderFixture.setActive(false)
                }
                GrowingVineState.GROWING -> {
                    val x: Float
                    val y: Float
                    val height: Float
                    if (up) {
                        x = body.getBounds().getX()
                        y = body.getBounds().getY()
                        height = (ladderIndex + 1) * ConstVals.PPM.toFloat()
                    } else {
                        x = body.getBounds().getX()
                        y = body.getBounds().getMaxY() - ((ladderTiles.size - ladderIndex) * ConstVals.PPM)
                        height = (ladderTiles.size - ladderIndex) * ConstVals.PPM.toFloat()
                    }
                    ladder.set(x, y, ConstVals.PPM.toFloat(), height)
                    ladderFixture.setActive(true)
                }
                GrowingVineState.GROWN -> {
                    ladder.set(body.getBounds())
                    ladderFixture.setActive(true)
                }
            }
        }

        return BodyComponentCreator.create(this, body)
    }

    override fun getType() = EntityType.SPECIAL
}
