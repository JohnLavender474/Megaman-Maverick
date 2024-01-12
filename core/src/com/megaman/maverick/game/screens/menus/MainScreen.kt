package com.megaman.maverick.game.screens.menus

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.time.Timer
import com.engine.drawables.fonts.BitmapFontHandle
import com.engine.screens.menus.IMenuButton
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.screens.utils.BlinkingArrow
import com.megaman.maverick.game.screens.utils.ScreenSlide
import com.megaman.maverick.game.utils.MegaUtilMethods.getDefaultFontSize
import com.megaman.maverick.game.utils.getDefaultCameraPosition
import com.megaman.maverick.game.utils.setToDefaultPosition

class MainScreen(game: MegamanMaverickGame) :
    AbstractMenuScreen(game, MainScreenButton.GAME_START.text) {

  enum class MainScreenButton(val text: String) {
    GAME_START("GAME START"),
    PASSWORD("PASSWORD"),
    SETTINGS("SETTINGS"),
    CREDITS("CREDITS"),
    EXTRAS("EXTRA"),
    EXIT("EXIT")
  }

  enum class MainScreenSettingsButton(val text: String) {
    BACK("BACK"),
    MUSIC_VOLUME("MUSIC VOLUME"),
    SOUND_EFFECTS_VOLUME("SOUND EFFECTS VOLUME"),
  }

  companion object {
    const val TAG = "MainScreen"
    private const val SETTINGS_ARROW_BLINK_DUR = .3f
    private const val SETTINGS_TRANS_DUR = .5f
    private val SETTINGS_TRAJ = Vector3(15f * ConstVals.PPM, 0f, 0f)
  }

  override val menuButtons = objectMapOf<String, IMenuButton>()

  private val pose = Sprite()
  private val title = Sprite()
  private val subtitle = Sprite()

  private var screenSlide =
      ScreenSlide(
          castGame.getUiCamera(),
          SETTINGS_TRAJ,
          getDefaultCameraPosition(),
          getDefaultCameraPosition().add(SETTINGS_TRAJ),
          SETTINGS_TRANS_DUR,
          true)

  private val fontHandles = Array<BitmapFontHandle>()
  private val settingsArrows = Array<Sprite>()

  private val settingsArrowBlinkTimer = Timer(SETTINGS_ARROW_BLINK_DUR)
  private val blinkArrows = ObjectMap<String, BlinkingArrow>()

  private var settingsArrowBlink = false

  init {
    var row = 0.175f * ConstVals.PPM

    MainScreenButton.values().forEach {
      val fontHandle =
          BitmapFontHandle(
              { it.text },
              getDefaultFontSize(),
              Vector2(2f * ConstVals.PPM, row * ConstVals.PPM),
              centerX = false,
              centerY = false,
              fontSource = "Megaman10Font.ttf")
      fontHandles.add(fontHandle)
      val arrowCenter =
          Vector2(1.5f * ConstVals.PPM, (row - (.0075f * ConstVals.PPM)) * ConstVals.PPM)
      blinkArrows.put(it.text, BlinkingArrow(game.assMan, arrowCenter))
      row -= ConstVals.PPM * .025f
    }

    row = 0.4f * ConstVals.PPM

    MainScreenSettingsButton.values().forEach {
      val fontHandle =
          BitmapFontHandle(
              { it.text },
              getDefaultFontSize(),
              Vector2(17f * ConstVals.PPM, row * ConstVals.PPM),
              centerX = false,
              centerY = false,
              fontSource = "Megaman10Font.ttf")
      fontHandles.add(fontHandle)
      val arrowCenter =
          Vector2(16.5f * ConstVals.PPM, (row - (.0075f * ConstVals.PPM)) * ConstVals.PPM)
      blinkArrows.put(it.text, BlinkingArrow(game.assMan, arrowCenter))
    }

    fontHandles.add(
        BitmapFontHandle(
            { "© OLD LAVY GENES, 20XX" },
            getDefaultFontSize(),
            Vector2(0.15f * ConstVals.PPM, 0.5f * ConstVals.PPM),
            centerX = false,
            centerY = false,
            fontSource = "Megaman10Font.ttf"))

    fontHandles.add(
        BitmapFontHandle(
            { castGame.audioMan.musicVolume.toString() },
            getDefaultFontSize(),
            Vector2(21f * ConstVals.PPM, 12f * ConstVals.PPM),
            centerX = false,
            centerY = false,
            fontSource = "Megaman10Font.ttf"))

    fontHandles.add(
        BitmapFontHandle(
            { castGame.audioMan.soundVolume.toString() },
            getDefaultFontSize(),
            Vector2(21f * ConstVals.PPM, 11.2f * ConstVals.PPM),
            centerX = false,
            centerY = false,
            fontSource = "Megaman10Font.ttf"))

    val arrowRegion = game.assMan.getTextureRegion(TextureAsset.UI_1.source, "Arrow")
    var y = 11.55f
    for (i in 0 until 4) {
      if (i != 0 && i % 2 == 0) y -= 0.85f
      val blinkArrow = Sprite(arrowRegion)
      blinkArrow.setBounds(
          (if (i % 2 == 0) 20.25f else 22.5f) * ConstVals.PPM,
          y * ConstVals.PPM,
          ConstVals.PPM / 2f,
          ConstVals.PPM / 2f)
      blinkArrow.setFlip(i % 2 == 0, false)
      settingsArrows.add(blinkArrow)
    }

    val atlas = game.assMan.getTextureAtlas(TextureAsset.UI_1.source)
    title.setRegion(atlas.findRegion("MegamanTitle"))
    title.setBounds(
        ConstVals.PPM.toFloat(), 8.25f * ConstVals.PPM, 13.25f * ConstVals.PPM, 5f * ConstVals.PPM)
    subtitle.setRegion(atlas.findRegion("Subtitle8bit"))
    subtitle.setSize(8f * ConstVals.PPM, 8f * ConstVals.PPM)
    subtitle.setCenter(
        ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f, (ConstVals.VIEW_HEIGHT + 1) * ConstVals.PPM / 2f)
    pose.setRegion(atlas.findRegion("MegamanMaverick"))
    pose.setBounds(8f * ConstVals.PPM, 0f, 8f * ConstVals.PPM, 8f * ConstVals.PPM)

    menuButtons.put(
        MainScreenButton.GAME_START.text,
        object : IMenuButton {
          override fun onSelect(delta: Float): Boolean {
            castGame.setCurrentScreen(ScreenEnum.BOSS_SELECT.name)
            return true
          }

          override fun onNavigate(direction: Direction, delta: Float): String? {
            return when (direction) {
              Direction.UP -> MainScreenButton.EXIT.text
              Direction.DOWN -> MainScreenButton.PASSWORD.text
              else -> null
            }
          }
        })

    menuButtons.put(
        MainScreenButton.PASSWORD.text,
        object : IMenuButton {
          override fun onSelect(delta: Float): Boolean {
            // TODO: game.setCurrentScreen(ScreenEnum.PASSWORD_SCREEN.name)
            return false
          }

          override fun onNavigate(direction: Direction, delta: Float): String? {
            return when (direction) {
              Direction.UP -> MainScreenButton.GAME_START.text
              Direction.DOWN -> MainScreenButton.SETTINGS.text
              else -> null
            }
          }
        })

    menuButtons.put(
        MainScreenButton.SETTINGS.text,
        object : IMenuButton {
          override fun onSelect(delta: Float): Boolean {
            screenSlide.init()
            currentButtonKey = MainScreenSettingsButton.BACK.text
            return false
          }

          override fun onNavigate(direction: Direction, delta: Float): String? {
            return when (direction) {
              Direction.UP -> MainScreenButton.PASSWORD.text
              Direction.DOWN -> MainScreenButton.CREDITS.text
              else -> null
            }
          }
        })

    menuButtons.put(
        MainScreenButton.CREDITS.text,
        object : IMenuButton {
          override fun onSelect(delta: Float): Boolean {
            return false
          }

          override fun onNavigate(direction: Direction, delta: Float): String? {
            return when (direction) {
              Direction.UP -> MainScreenButton.SETTINGS.text
              Direction.DOWN -> MainScreenButton.EXTRAS.text
              else -> null
            }
          }
        })

    menuButtons.put(
        MainScreenButton.EXTRAS.text,
        object : IMenuButton {
          override fun onSelect(delta: Float): Boolean {
            game.setCurrentScreen(ScreenEnum.EXTRAS.name)
            return false
          }

          override fun onNavigate(direction: Direction, delta: Float): String? {
            return when (direction) {
              Direction.UP -> MainScreenButton.CREDITS.text
              Direction.DOWN -> MainScreenButton.EXIT.text
              else -> null
            }
          }
        })

    menuButtons.put(
        MainScreenButton.EXIT.text,
        object : IMenuButton {
          override fun onSelect(delta: Float): Boolean {
            Gdx.app.exit()
            return true
          }

          override fun onNavigate(direction: Direction, delta: Float): String? {
            return when (direction) {
              Direction.UP -> MainScreenButton.EXTRAS.text
              Direction.DOWN -> MainScreenButton.GAME_START.text
              else -> null
            }
          }
        })

    menuButtons.put(
        MainScreenSettingsButton.BACK.text,
        object : IMenuButton {
          override fun onSelect(delta: Float): Boolean {
            screenSlide.init()
            currentButtonKey = MainScreenButton.SETTINGS.text
            return false
          }

          override fun onNavigate(direction: Direction, delta: Float): String? {
            return when (direction) {
              Direction.UP -> MainScreenSettingsButton.SOUND_EFFECTS_VOLUME.text
              Direction.DOWN -> MainScreenSettingsButton.MUSIC_VOLUME.text
              else -> null
            }
          }
        })

    menuButtons.put(
        MainScreenSettingsButton.MUSIC_VOLUME.text,
        object : IMenuButton {
          override fun onSelect(delta: Float): Boolean {
            return false
          }

          override fun onNavigate(direction: Direction, delta: Float): String? {
            return when (direction) {
              Direction.LEFT -> {
                var volume = castGame.audioMan.musicVolume
                volume = if (volume == 0f) 10f else volume - 1f
                castGame.audioMan.musicVolume = volume
                null
              }
              Direction.RIGHT -> {
                var volume = castGame.audioMan.musicVolume
                volume = if (volume == 10f) 0f else volume + 1f
                castGame.audioMan.musicVolume = volume
                null
              }
              Direction.UP -> MainScreenSettingsButton.BACK.text
              Direction.DOWN -> MainScreenSettingsButton.SOUND_EFFECTS_VOLUME.text
            }
          }
        })

    menuButtons.put(
        MainScreenSettingsButton.SOUND_EFFECTS_VOLUME.text,
        object : IMenuButton {
          override fun onSelect(delta: Float): Boolean {
            return false
          }

          override fun onNavigate(direction: Direction, delta: Float): String? {
            return when (direction) {
              Direction.LEFT -> {
                var volume = castGame.audioMan.soundVolume
                volume = if (volume == 0f) 10f else volume - 1f
                castGame.audioMan.soundVolume = volume
                null
              }
              Direction.RIGHT -> {
                var volume = castGame.audioMan.soundVolume
                volume = if (volume == 10f) 0f else volume + 1f
                castGame.audioMan.soundVolume = volume
                null
              }
              Direction.UP -> MainScreenSettingsButton.MUSIC_VOLUME.text
              Direction.DOWN -> MainScreenSettingsButton.BACK.text
            }
          }
        })
  }

  override fun show() {
    super.show()
    castGame.getUiCamera().setToDefaultPosition()
    castGame.audioMan.playMusic(MusicAsset.MM_OMEGA_TITLE_THEME_MUSIC)
  }

  override fun render(delta: Float) {
    super.render(delta)
    if (!game.paused) {
      screenSlide.update(delta)
      if (screenSlide.justFinished) screenSlide.reverse()

      blinkArrows.get(currentButtonKey).update(delta)

      settingsArrowBlinkTimer.update(delta)
      if (settingsArrowBlinkTimer.isFinished()) {
        settingsArrowBlink = !settingsArrowBlink
        settingsArrowBlinkTimer.reset()
      }
    }

    val batch = game.batch
    batch.projectionMatrix = castGame.getUiCamera().combined
    batch.begin()

    blinkArrows.get(currentButtonKey).draw(batch)
    title.draw(batch)
    pose.draw(batch)
    subtitle.draw(batch)

    fontHandles.forEach { it.draw(batch) }

    if (settingsArrowBlink) settingsArrows.forEach { it.draw(batch) }

    batch.end()
  }

  override fun onAnyMovement() {
    super.onAnyMovement()
    GameLogger.debug(TAG, "Current button: $currentButtonKey")
    castGame.audioMan.playSound(SoundAsset.CURSOR_MOVE_BLOOP_SOUND)
  }

  override fun onAnySelection(): Boolean {
    val allow = screenSlide.finished
    if (allow) castGame.audioMan.playSound(SoundAsset.SELECT_PING_SOUND)
    return allow
  }
}
