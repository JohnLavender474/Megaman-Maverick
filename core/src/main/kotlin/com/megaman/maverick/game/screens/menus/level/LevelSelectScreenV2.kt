package com.megaman.maverick.game.screens.menus.level

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.interfaces.Updatable
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.IDrawable
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstFuncs
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.levels.LevelType
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.menus.MegaMenuScreen

class LevelSelectScreenV2(game: MegamanMaverickGame) : MegaMenuScreen(game, Position.CENTER.name), Initializable {

    companion object {
        const val TAG = "LevelSelectScreenV2"
        private const val SELECTOR_BLINK_DUR = 0.1f
        private const val OUTRO_DUR = 1f
        private val MUGSHOT_POSITIONS = objectMapOf(
            Position.TOP_LEFT pairTo LevelDefinition.REACTOR_MAN,
            Position.TOP_CENTER pairTo LevelDefinition.GLACIER_MAN,
            Position.TOP_RIGHT pairTo LevelDefinition.PRECIOUS_WOMAN,
            Position.CENTER_LEFT pairTo LevelDefinition.DESERT_MAN,
            Position.CENTER_RIGHT pairTo LevelDefinition.INFERNO_MAN,
            Position.BOTTOM_LEFT pairTo LevelDefinition.TIMBER_WOMAN,
            Position.BOTTOM_CENTER pairTo LevelDefinition.MOON_MAN,
            Position.BOTTOM_RIGHT pairTo LevelDefinition.RODENT_MAN
        )
    }

    private val mugshotGrid = ObjectMap<Position, MugshotV2>()

    private val outroTimer = Timer(OUTRO_DUR)
    private var outro = false

    private val background = GameSprite()
    private val backgroundAnims = ObjectMap<String, Animation>()
    private val currentBkgAnim: Animation
        get() {
            var key = if (selectionMade) ConstKeys.SELECTED else ConstKeys.STATIC
            if (game.state.allRobotMasterLevelsDefeated()) key += "_wily"
            return backgroundAnims[key]
        }

    private val selector = GameSprite()
    private val selectorRegions = ObjectMap<String, TextureRegion>()
    private val selectorBlinkTimer = Timer(SELECTOR_BLINK_DUR)
    private val shouldDrawSelector: Boolean
        get() = buttonKey != null && !selectionMade

    private val foregroundSprites = OrderedMap<GameSprite, Updatable?>()

    private var selectedLevelDef: LevelDefinition? = null

    private var initialized = false

    override fun init() {
        if (initialized) {
            GameLogger.debug(TAG, "init(): already initialized, nothing to do")
            return
        }

        val levelSelectScreenAtlas = game.assMan.getTextureAtlas(TextureAsset.LEVEL_SELECT_SCREEN_V2.source)

        val staticBkgRegion =
            levelSelectScreenAtlas.findRegion("${ConstKeys.BACKGROUND}_${ConstKeys.STATIC}")
        val staticAnim = Animation(staticBkgRegion)
        backgroundAnims.put(ConstKeys.STATIC, staticAnim)

        val staticWilyBkgRegion =
            levelSelectScreenAtlas.findRegion("${ConstKeys.BACKGROUND}_${ConstKeys.STATIC}_wily")
        val staticWilyAnim = Animation(staticWilyBkgRegion)
        backgroundAnims.put("${ConstKeys.STATIC}_wily", staticWilyAnim)

        val selectedBackgroundRegion =
            levelSelectScreenAtlas.findRegion("${ConstKeys.BACKGROUND}_${ConstKeys.SELECTED}")
        val selectedAnim = Animation(selectedBackgroundRegion, 2, 1, 0.1f, true)
        backgroundAnims.put(ConstKeys.SELECTED, selectedAnim)

        val selectedBackgroundWilyRegion =
            levelSelectScreenAtlas.findRegion("${ConstKeys.BACKGROUND}_${ConstKeys.SELECTED}_wily")
        val selectedWilyAnim = Animation(selectedBackgroundWilyRegion, 2, 1, 0.1f, true)
        backgroundAnims.put("${ConstKeys.SELECTED}_wily", selectedWilyAnim)

        Position.entries.forEach { position ->
            val key = position.name
            val selectorRegion = levelSelectScreenAtlas.findRegion("${ConstKeys.SELECTOR}/${key.lowercase()}")
            selectorRegions.put(key, selectorRegion)
        }

        MUGSHOT_POSITIONS.forEach { entry ->
            val position = entry.key
            val level = entry.value
            putBossMugshot(position, level)
        }

        putCenterMugshot()

        background.setBounds(
            0f,
            0f,
            ConstVals.VIEW_WIDTH * ConstVals.PPM,
            ConstVals.VIEW_HEIGHT * ConstVals.PPM
        )

        selector.setBounds(
            0f,
            0f,
            ConstVals.VIEW_WIDTH * ConstVals.PPM,
            ConstVals.VIEW_HEIGHT * ConstVals.PPM
        )
    }

    private fun putCenterMugshot() {
        val atlas = game.assMan.getTextureAtlas(TextureAsset.LEVEL_SELECT_SCREEN_V2.source)

        val megamanFaces = ObjectMap<Position, TextureRegion>()

        Position.entries.forEach { pos ->
            val region = atlas.findRegion("${ConstKeys.FACES}/megaman_${pos.name.lowercase()}")
            megamanFaces.put(pos, region)
        }

        val wilyFace = atlas.findRegion("${ConstKeys.FACES}/wily")

        val faceSupplier: () -> TextureRegion? = faceSupplier@{
            if (game.state.allRobotMasterLevelsDefeated()) return@faceSupplier wilyFace

            val position = Position.entries.find { it.name == buttonKey } ?: Position.CENTER
            val megamanFace = megamanFaces[position]
            return@faceSupplier megamanFace
        }

        val mugshot = MugshotV2(game, faceSupplier)
        mugshotGrid.put(Position.CENTER, mugshot)

        buttons.put(Position.CENTER.name, object : IMenuButton {

            override fun onSelect(delta: Float): Boolean {
                if (game.state.allRobotMasterLevelsDefeated()) {
                    val wilyStage = game.state.getNextWilyStage()

                    if (wilyStage == null) {
                        GameLogger.error(TAG, "Next Wily stage is null")
                        return false
                    }

                    select(wilyStage)

                    return true
                }

                return false
            }

            override fun onNavigate(direction: Direction, delta: Float) = Position.CENTER.move(direction).name
        })
    }

    private fun select(levelDef: LevelDefinition) {
        game.audioMan.let {
            it.playSound(SoundAsset.BEAM_OUT_SOUND, false)
            it.stopMusic(null)
        }

        selectedLevelDef = levelDef

        outro = true
    }

    private fun putBossMugshot(position: Position, levelDef: LevelDefinition) {
        val mugshotRegion = game.assMan.getTextureRegion(
            TextureAsset.LEVEL_SELECT_SCREEN_V2.source, "${ConstKeys.FACES}/${levelDef.name.lowercase()}"
        )
        val faceSupplier: () -> TextureRegion? = {
            when {
                game.state.isLevelDefeated(levelDef) -> null
                else -> mugshotRegion
            }
        }

        val mugshot = MugshotV2(game, faceSupplier)
        mugshotGrid.put(position, mugshot)

        buttons.put(position.name, object : IMenuButton {

            override fun onSelect(delta: Float): Boolean {
                select(levelDef)
                return true
            }

            override fun onNavigate(direction: Direction, delta: Float) = position.move(direction).name
        })
    }

    override fun show() {
        if (!initialized) {
            init()
            initialized = true
        }

        super.show()

        outro = false
        outroTimer.reset()

        selectorBlinkTimer.reset()

        game.getUiCamera().position.set(ConstFuncs.getUiCamInitPos())

        val musicAsset = when {
            game.state.allRobotMasterLevelsDefeated() -> MusicAsset.VINNYZ_WILY_STAGE_SELECT_V1_MUSIC
            else -> MusicAsset.VINNYZ_STAGE_SELECT_V1_MUSIC
        }
        game.audioMan.playMusic(musicAsset, true)
    }

    override fun onAnyMovement(direction: Direction) =
        game.audioMan.playSound(SoundAsset.CURSOR_MOVE_BLOOP_SOUND, false)

    override fun render(delta: Float) {
        super.render(delta)

        if (!game.paused) {
            if (outro) outroTimer.update(delta)
            if (outroTimer.isJustFinished()) {
                game.setCurrentLevel(selectedLevelDef!!)

                if (selectedLevelDef!!.type != LevelType.ROBOT_MASTER_LEVEL ||
                    game.state.isLevelDefeated(selectedLevelDef!!)
                ) game.startLevel()
                else game.setCurrentScreen(ScreenEnum.ROBOT_MASTER_INTRO_SCREEN.name)
            }
            if (outroTimer.isFinished()) return

            currentBkgAnim.update(delta)
            background.setRegion(currentBkgAnim.getCurrentRegion())

            if (shouldDrawSelector) {
                val selectorRegion = selectorRegions[buttonKey]
                selector.setRegion(selectorRegion)

                selectorBlinkTimer.update(delta)
                if (selectorBlinkTimer.isFinished()) {
                    selector.hidden = !selector.hidden
                    selectorBlinkTimer.reset()
                }
            }

            foregroundSprites.values().forEach { it?.update(delta) }
        }
    }

    override fun draw(drawer: Batch) {
        game.viewports.get(ConstKeys.UI).apply()
        drawer.projectionMatrix = game.getUiCamera().combined
        drawer.begin()

        background.draw(drawer)
        mugshotGrid.forEach { it.value.draw(drawer) }
        foregroundSprites.keys().forEach { it.draw(drawer) }
        if (shouldDrawSelector) selector.draw(drawer)

        drawer.end()
    }
}

private class MugshotV2(
    private val game: MegamanMaverickGame,
    private val faceSupplier: () -> TextureRegion?
) : Initializable, IDrawable<Batch> {

    companion object {
        const val TAG = "Mugshot"

        private const val NONE_REGION_KEY = "none"
        private const val BLINKING_REGION_KEY = "blinking"
        private const val HIGHLIGHTED_REGION_KEY = "highlighted"

        private val regions = ObjectMap<String, TextureRegion>()
    }

    val faceSprite = GameSprite()
    var initialized = false

    override fun init() {
        if (initialized) {
            GameLogger.debug(TAG, "init(): already initialized, nothing to do")
            return
        }

        if (regions.isEmpty) {
            val uiAtlas = game.assMan.getTextureAtlas(TextureAsset.UI_1.source)
            gdxArrayOf(NONE_REGION_KEY, BLINKING_REGION_KEY, HIGHLIGHTED_REGION_KEY).forEach {
                regions.put(it, uiAtlas.findRegion("${TAG}/$it"))
            }
        }


        faceSprite.setBounds(0f, 0f, ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM)
    }

    override fun draw(drawer: Batch) {
        if (!initialized) {
            init()
            initialized = true
        }
        drawFace(drawer)
    }

    private fun drawFace(drawer: Batch) {
        val face = faceSupplier.invoke()
        if (face != null) {
            faceSprite.setRegion(face)
            faceSprite.draw(drawer)
        }
    }
}
