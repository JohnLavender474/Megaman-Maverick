package com.megaman.maverick.game.controllers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.viewport.FitViewport
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.controller.buttons.ControllerButton
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.setSize
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset

class ScreenController(private val game: MegamanMaverickGame) {

    private class ScreenButtonDrawable(private val sprite: GameSprite, private val rotation: Float) : SpriteDrawable() {

        override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
            val spriteColor = sprite.color
            val oldColor = sprite.packedColor
            sprite.setColor(spriteColor.mul(batch.color))

            sprite.setScale(1.25f, 1.25f)
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

    companion object {
        const val TAG = "ScreenController"
        private const val PADDING = 0.15f
        private const val BUTTON_SIZE = 1.15f
        private const val BLANK_WIDTH = 0.35f
        private const val TABLE_WIDTH = 16
        private const val ALPHA = 0.5f
        private const val DEBUG = false
    }

    var viewport: FitViewport
        private set
    var stage: Stage
        private set

    init {
        GameLogger.debug(TAG, "init()")

        val camera = OrthographicCamera()
        viewport = FitViewport(ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM, camera)
        stage = Stage(viewport, game.batch)
        Gdx.input.inputProcessor = stage

        val uiAtlas = game.assMan.getTextureAtlas(TextureAsset.UI_1.source)
        val dpadRegion = uiAtlas.findRegion("$TAG/${ConstKeys.ARROW}")
        val dpadPressedRegion = uiAtlas.findRegion("$TAG/${ConstKeys.ARROW}_${ConstKeys.PRESSED}")
        val buttonRegion = uiAtlas.findRegion("$TAG/${ConstKeys.BUTTON}")
        val buttonPressedRegion = uiAtlas.findRegion("$TAG/${ConstKeys.BUTTON}_${ConstKeys.PRESSED}")
        val aRegion = uiAtlas.findRegion("$TAG/${ConstKeys.A}")
        val bRegion = uiAtlas.findRegion("$TAG/${ConstKeys.B}")

        val pressedMap = ObjectMap<MegaControllerButton, Boolean>()
        val buttonImages = ObjectMap<MegaControllerButton, Image>()
        val dpadButtons = MegaControllerButton.getDpadButtons()
        MegaControllerButton.entries.forEach {
            pressedMap.put(it, false)

            val sprite = GameSprite()
            sprite.setSize(BUTTON_SIZE * ConstVals.PPM)
            val region = when (it) {
                MegaControllerButton.A, MegaControllerButton.B -> buttonRegion
                else -> dpadRegion
            }
            sprite.setRegion(region)
            sprite.setOriginCenter()
            val rotation = when (it) {
                MegaControllerButton.LEFT -> 90f
                MegaControllerButton.DOWN -> 180f
                MegaControllerButton.RIGHT -> 270f
                else -> 0f
            }
            sprite.setAlpha(ALPHA)

            val drawable = ScreenButtonDrawable(sprite, rotation)
            val dpadImage = Image(drawable)
            dpadImage.setSize(BUTTON_SIZE * ConstVals.PPM, BUTTON_SIZE * ConstVals.PPM)
            dpadImage.addListener(object : InputListener() {

                override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                    val region = if (dpadButtons.contains(it)) dpadPressedRegion else buttonPressedRegion
                    sprite.setRegion(region)
                    pressedMap.put(it, true)
                    return true
                }

                override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                    val region = if (dpadButtons.contains(it)) dpadRegion else buttonRegion
                    sprite.setRegion(region)
                    pressedMap.put(it, false)
                }
            })
            buttonImages.put(it, dpadImage)

            val button = game.buttons[it] as ControllerButton
            button.alternateActuators.put(ConstKeys.SCREEN) { pressedMap[it] }
        }

        val table = Table()
        table.bottom().left()
        for (i in 0 until TABLE_WIDTH) {
            when (i) {
                2 -> addButton(table, buttonImages[MegaControllerButton.UP])
                else -> addBlankCell(table)
            }
        }
        table.row()
        for (i in 0 until TABLE_WIDTH) {
            when (i) {
                1 -> addButton(table, buttonImages[MegaControllerButton.LEFT])
                3 -> addButton(table, buttonImages[MegaControllerButton.RIGHT])
                TABLE_WIDTH - 3 -> addButton(table, buttonImages[MegaControllerButton.B])
                TABLE_WIDTH - 1 -> addButton(table, buttonImages[MegaControllerButton.A])
                else -> addBlankCell(table)
            }
        }
        table.row()
        for (i in 0 until TABLE_WIDTH) {
            when (i) {
                2 -> addButton(table, buttonImages[MegaControllerButton.DOWN])
                TABLE_WIDTH - 3 -> addButton(table, Image(SpriteDrawable(Sprite(aRegion))))
                TABLE_WIDTH - 1 -> addButton(table, Image(SpriteDrawable(Sprite(bRegion))))
                else -> addBlankCell(table)
            }
        }

        stage.addActor(table)
        stage.isDebugAll = DEBUG
    }

    private fun addBlankCell(table: Table) {
        table.add().size(BLANK_WIDTH * ConstVals.PPM, BUTTON_SIZE * ConstVals.PPM).pad(PADDING * ConstVals.PPM)
    }

    private fun addButton(table: Table, image: Image) {
        table.add(image).size(BUTTON_SIZE * ConstVals.PPM).pad(PADDING * ConstVals.PPM)
    }
}
