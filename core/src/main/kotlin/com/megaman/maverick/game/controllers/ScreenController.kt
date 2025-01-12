package com.megaman.maverick.game.controllers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.Viewport
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.interfaces.Resizable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.controller.buttons.ControllerButton
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.setSize
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.com.megaman.maverick.game.assets.TextureAsset

class ScreenController(private val game: MegamanMaverickGame) : Updatable, Resizable {

    companion object {
        const val TAG = "ScreenController"
        private const val CELL_PADDING_WIDTH = 0.2f
        private const val CELL_PADDING_HEIGHT = 0.2f
        private const val BUTTON_SIZE = 1f
        private const val BLANK_WIDTH = 0.25f
        private const val DPAD_TABLE_SIZE = 3
        private const val ACTION_TABLE_WIDTH = 2
        private const val ACTION_TABLE_HEIGHT = 4
        private const val ALPHA_UNPRESSED = 0.5f
        private const val ALPHA_PRESSED = 0.75f
        private const val DEBUG = false
    }

    private class ScreenButtonDrawable(private val sprite: GameSprite, private val rotation: Float) : SpriteDrawable() {

        override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
            val spriteColor = sprite.color
            val oldColor = sprite.packedColor
            sprite.setColor(spriteColor.mul(batch.color))

            sprite.setScale(BUTTON_SIZE, BUTTON_SIZE)
            sprite.setBounds(x, y, width, height)
            sprite.setOriginCenter()
            sprite.rotation = rotation
            sprite.draw(batch)

            sprite.packedColor = oldColor
            sprite.draw(batch)
        }

        override fun draw(
            batch: Batch,
            x: Float,
            y: Float,
            originX: Float,
            originY: Float,
            width: Float,
            height: Float,
            scaleX: Float,
            scaleY: Float,
            rotation: Float
        ) = draw(batch, x, y, width, height)
    }

    private val stage: Stage
    private val viewport: Viewport
    private val sprites = ObjectMap<MegaControllerButton, GameSprite>()

    private val dpadRegion: TextureRegion
    private val dpadPressedRegion: TextureRegion
    private val buttonRegion: TextureRegion
    private val buttonPressedRegion: TextureRegion
    private val commandRegion: TextureRegion
    private val commandPressedRegion: TextureRegion
    private val aRegion: TextureRegion
    private val bRegion: TextureRegion
    private val startRegion: TextureRegion
    private val selectRegion: TextureRegion

    init {
        GameLogger.debug(TAG, "init()")

        val camera = OrthographicCamera()
        viewport = ExtendViewport(ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM, camera)
        stage = Stage(viewport, game.batch)
        Gdx.input.inputProcessor = stage

        val uiAtlas = game.assMan.getTextureAtlas(TextureAsset.UI_1.source)
        dpadRegion = uiAtlas.findRegion("$TAG/${ConstKeys.ARROW}")
        dpadPressedRegion = uiAtlas.findRegion("$TAG/${ConstKeys.ARROW}_${ConstKeys.PRESSED}")
        buttonRegion = uiAtlas.findRegion("$TAG/${ConstKeys.BUTTON}")
        buttonPressedRegion = uiAtlas.findRegion("$TAG/${ConstKeys.BUTTON}_${ConstKeys.PRESSED}")
        commandRegion = uiAtlas.findRegion("$TAG/${ConstKeys.COMMAND}")
        commandPressedRegion = uiAtlas.findRegion("$TAG/${ConstKeys.COMMAND}_${ConstKeys.PRESSED}")
        aRegion = uiAtlas.findRegion("$TAG/${ConstKeys.A}")
        bRegion = uiAtlas.findRegion("$TAG/${ConstKeys.B}")
        startRegion = uiAtlas.findRegion("$TAG/${ConstKeys.START}")
        selectRegion = uiAtlas.findRegion("$TAG/${ConstKeys.SELECT}")

        val pressedMap = ObjectMap<MegaControllerButton, Boolean>()
        val buttonImages = ObjectMap<MegaControllerButton, Image>()
        MegaControllerButton.entries.forEach {
            pressedMap.put(it, false)

            val sprite = GameSprite()
            sprite.setSize(BUTTON_SIZE * ConstVals.PPM)
            sprite.setOriginCenter()
            val rotation = when (it) {
                MegaControllerButton.LEFT -> 90f
                MegaControllerButton.DOWN -> 180f
                MegaControllerButton.RIGHT -> 270f
                else -> 0f
            }
            sprite.setAlpha(ALPHA_UNPRESSED)
            sprites.put(it, sprite)

            val drawable = ScreenButtonDrawable(sprite, rotation)
            val dpadImage = Image(drawable)
            dpadImage.setSize(BUTTON_SIZE * ConstVals.PPM, BUTTON_SIZE * ConstVals.PPM)
            dpadImage.addListener(object : InputListener() {

                override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                    sprite.setAlpha(ALPHA_PRESSED)
                    pressedMap.put(it, true)
                    return true
                }

                override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                    sprite.setAlpha(ALPHA_UNPRESSED)
                    pressedMap.put(it, false)
                }
            })
            buttonImages.put(it, dpadImage)

            val button = game.buttons[it] as ControllerButton
            button.alternateActuators.put(ConstKeys.SCREEN) { pressedMap[it] }
        }

        val dpadTable = Table()
        for (y in 0 until DPAD_TABLE_SIZE) {
            for (x in 0 until DPAD_TABLE_SIZE) when {
                x == 1 && y == 0 -> addButton(dpadTable, buttonImages[MegaControllerButton.UP])
                x == 0 && y == 1 -> addButton(dpadTable, buttonImages[MegaControllerButton.LEFT])
                x == 2 && y == 1 -> addButton(dpadTable, buttonImages[MegaControllerButton.RIGHT])
                x == 1 && y == 2 -> addButton(dpadTable, buttonImages[MegaControllerButton.DOWN])
                else -> addBlankCell(dpadTable)
            }
            dpadTable.row()
        }

        val actionTable = Table()
        for (y in 0 until ACTION_TABLE_HEIGHT) {
            for (x in 0 until ACTION_TABLE_WIDTH) {
                val actor = if (x == 0) when (y) {
                    0 -> buttonImages[MegaControllerButton.START]
                    1 -> Image(SpriteDrawable(Sprite(startRegion)))
                    2 -> buttonImages[MegaControllerButton.B]
                    3 -> Image(SpriteDrawable(Sprite(bRegion)))
                    else -> throw IllegalStateException("Invalid y: x=$x, y=$y")
                } else when (y) {
                    0 -> buttonImages[MegaControllerButton.SELECT]
                    1 -> Image(SpriteDrawable(Sprite(selectRegion)))
                    2 -> buttonImages[MegaControllerButton.A]
                    3 -> Image(SpriteDrawable(Sprite(aRegion)))
                    else -> throw IllegalStateException("Invalid y: x=$x, y=$y")
                }
                addButton(actionTable, actor)
            }
            actionTable.row()
        }

        val outerTable = Table()
        outerTable.setFillParent(true)
        outerTable.bottom().left()
        outerTable.add(dpadTable).bottom().left()
        outerTable.add().expandX()
        outerTable.add(actionTable).bottom().right()
        stage.addActor(outerTable)

        stage.isDebugAll = DEBUG
    }

    override fun update(delta: Float) {
        MegaControllerButton.entries.forEach {
            val pressed = game.controllerPoller.isPressed(it)
            val region = when {
                it.isDpadButton() -> if (pressed) dpadPressedRegion else dpadRegion
                it.isActionButton() -> if (pressed) buttonPressedRegion else buttonRegion
                else -> if (pressed) commandPressedRegion else commandRegion
            }
            sprites[it].setRegion(region)
        }
        stage.act(delta)
        viewport.apply()
        stage.draw()
    }

    override fun resize(width: Number, height: Number) = viewport.update(width.toInt(), height.toInt(), true)

    private fun addBlankCell(table: Table) {
        table.add().size(BLANK_WIDTH * ConstVals.PPM, BUTTON_SIZE * ConstVals.PPM).pad(
            CELL_PADDING_HEIGHT * ConstVals.PPM,
            CELL_PADDING_WIDTH * ConstVals.PPM,
            CELL_PADDING_HEIGHT * ConstVals.PPM,
            CELL_PADDING_WIDTH * ConstVals.PPM
        )
    }

    private fun addButton(table: Table, image: Image) {
        table.add(image).size(BUTTON_SIZE * ConstVals.PPM).pad(
            CELL_PADDING_HEIGHT * ConstVals.PPM,
            CELL_PADDING_WIDTH * ConstVals.PPM,
            CELL_PADDING_HEIGHT * ConstVals.PPM,
            CELL_PADDING_WIDTH * ConstVals.PPM
        )
    }
}
