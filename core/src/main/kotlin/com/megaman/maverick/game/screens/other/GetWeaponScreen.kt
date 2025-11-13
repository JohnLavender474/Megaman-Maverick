package com.megaman.maverick.game.screens.other

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.badlogic.gdx.utils.Queue
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.Initializable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.containsRegion
import com.mega.game.engine.screens.BaseScreen
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.controllers.MegaControllerButton
import com.megaman.maverick.game.drawables.fonts.MegaFontHandle
import com.megaman.maverick.game.entities.megaman.constants.MegamanWeapon
import com.megaman.maverick.game.screens.ScreenEnum
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.setToDefaultPosition
import java.util.*

class GetWeaponScreen(private val game: MegamanMaverickGame) : BaseScreen(), Initializable {

    companion object {
        const val TAG = "GetWeaponScreen"

        private const val MEGAMAN_REG_PREFIX = "megaman/"
        private val MEGAMAN_START = Vector2(ConstVals.VIEW_WIDTH / 2f, -5f)
        private val MEGAMAN_END = Vector2(4f, ConstVals.VIEW_HEIGHT / 2f)

        private val EVENT_DURS = gdxArrayOf(0.5f, 0.4f, 2f, 0.5f)

        private val ANIM_DEFS = ObjectMap<String, AnimationDef>().also {
            MegamanWeapon.entries.forEach { weapon ->
                it.put(weapon.name.lowercase(), AnimationDef(2, 2, 0.1f, false))
            }
        }

        private val END_BUTTONS = gdxArrayOf<Any>(
            MegaControllerButton.A,
            MegaControllerButton.B,
            MegaControllerButton.START
        )

        private const val TEXT_X = 10f
        private const val TEXT_Y = 4f

        private const val LETTER_DELAY = 0.2f

        private const val PRESS_START_BLINK = 0.25f
    }

    private enum class GetWeaponEventType { MEGAMAN_MOVE, MEGAMAN_TRANS, WEAPON_EVENT, END_EVENT }

    private data class GetWeaponEvent(val type: GetWeaponEventType, val props: Properties = props())

    private val eventQueue = Queue<GetWeaponEvent>()
    private val eventTimer = Timer()
    private val currentEventType: GetWeaponEventType?
        get() = eventQueue.firstOrNull()?.type

    private val backgroundSprite = GameSprite().also { sprite ->
        sprite.setBounds(0f, 0f, ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM)
    }

    private val overlaySprite = GameSprite().also { sprite ->
        sprite.setBounds(0f, 0f, ConstVals.VIEW_WIDTH * ConstVals.PPM, ConstVals.VIEW_HEIGHT * ConstVals.PPM)
    }

    private val megamanAnimators = OrderedMap<String, IAnimator>()
    private val megamanSprite = GameSprite().also { sprite ->
        sprite.setBounds(0f, 0f, 8f * ConstVals.PPM, 10f * ConstVals.PPM)
    }

    private val textHandle = MegaFontHandle()

    private var blinkTextDelay = Timer(PRESS_START_BLINK)
    private var blinkText = false

    private val letterDelay = Timer(LETTER_DELAY)
    private val lettersQ = LinkedList<Runnable>()

    private var initialized = false

    override fun init() {
        val uiAtlas = game.assMan.getTextureAtlas(TextureAsset.UI_1.source)
        val backgroundRegion = uiAtlas.findRegion("menu_screen_bkg")
        backgroundSprite.setRegion(backgroundRegion)

        val weaponGetAtlas = game.assMan.getTextureAtlas(TextureAsset.WEAPON_GET.source)
        overlaySprite.setRegion(weaponGetAtlas.findRegion("overlay"))
        MegamanWeapon.entries.forEach { weapon ->
            val baseKey = weapon.name.lowercase()

            val fullKey = "${MEGAMAN_REG_PREFIX}${weapon.name.lowercase()}"
            if (!weaponGetAtlas.containsRegion(fullKey)) {
                GameLogger.error(TAG, "init(): no region for full key: $fullKey")
                return@forEach
            }

            val region = weaponGetAtlas.findRegion(fullKey)

            val (rows, columns, durations, loop) = ANIM_DEFS[baseKey]
            val animation = Animation(region, rows, columns, durations, loop)

            val animator1 = Animator(animation)
            megamanAnimators.put(baseKey, animator1)

            val animator2 = Animator(animation.reversed())
            megamanAnimators.put("${baseKey}_reversed", animator2)
        }
        GameLogger.debug(TAG, "init(): megaman animators: $megamanAnimators")

        textHandle.positionX = TEXT_X * ConstVals.PPM
        textHandle.positionY = TEXT_Y * ConstVals.PPM
    }

    override fun show() {
        if (!initialized) {
            init()
            initialized = true
        }

        game.getUiCamera().setToDefaultPosition()

        val weaponsAttained = (game.getProperty(ConstKeys.WEAPONS_ATTAINED) as OrderedSet<MegamanWeapon>?)?.toGdxArray()
        if (weaponsAttained == null || weaponsAttained.isEmpty) {
            GameLogger.error(TAG, "show(): failed to load weapons attained")
            game.setCurrentScreen(ScreenEnum.SAVE_GAME_SCREEN.name)
            return
        }

        game.removeProperty(ConstKeys.WEAPONS_ATTAINED)
        GameLogger.debug(TAG, "show(): remove weapons attained prop from global game object")

        eventQueue.clear()
        eventQueue.addLast(GetWeaponEvent(GetWeaponEventType.MEGAMAN_MOVE))
        for (i in 0 until weaponsAttained.size) {
            val currentWeapon = weaponsAttained[i]
            val previousWeapon = if (i == 0) MegamanWeapon.MEGA_BUSTER else weaponsAttained[i - 1]

            eventQueue.addLast(
                GetWeaponEvent(
                    GetWeaponEventType.MEGAMAN_TRANS,
                    props(ConstKeys.WEAPON pairTo previousWeapon)
                )
            )

            eventQueue.addLast(
                GetWeaponEvent(
                    GetWeaponEventType.WEAPON_EVENT,
                    props(ConstKeys.WEAPON pairTo currentWeapon)
                )
            )
        }
        eventQueue.addLast(GetWeaponEvent(GetWeaponEventType.END_EVENT))
        GameLogger.debug(TAG, "show(): eventQueue=$eventQueue")

        resetEventTimer()

        game.audioMan.playMusic(MusicAsset.VINNYZ_WEAPON_GET_MUSIC, true)
    }

    override fun render(delta: Float) {
        val event = eventQueue.firstOrNull()

        if (event == null) {
            GameLogger.debug(TAG, "render(): event is null: go to next screen")
            game.setCurrentScreen(ScreenEnum.SAVE_GAME_SCREEN.name)
            return
        }

        val (eventType, props) = event

        when (eventType) {
            GetWeaponEventType.MEGAMAN_MOVE -> {
                val position = UtilMethods.interpolate(
                    MEGAMAN_START, MEGAMAN_END, eventTimer.getRatio(),
                    GameObjectPools.fetch(Vector2::class)
                )

                // Animator with 0f delta so that the sprite stays at the first animation region
                val animator = megamanAnimators.get(MegamanWeapon.MEGA_BUSTER.name.lowercase())
                animator.animate(megamanSprite, 0f)

                megamanSprite.setCenter(position.x * ConstVals.PPM, position.y * ConstVals.PPM)

                updateEventTimer(delta)
            }

            GetWeaponEventType.MEGAMAN_TRANS, GetWeaponEventType.WEAPON_EVENT -> {
                val weapon = props.get(ConstKeys.WEAPON, MegamanWeapon::class)!!

                if (eventType == GetWeaponEventType.MEGAMAN_TRANS) updateEventTimer(delta)
                else {
                    if (!lettersQ.isEmpty()) {
                        letterDelay.update(delta)
                        if (letterDelay.isFinished()) {
                            lettersQ.removeFirst().run()
                            letterDelay.reset()
                        }
                    } else updateEventTimer(delta)
                }

                try {
                    var animKey = weapon.name.lowercase()
                    if (eventType == GetWeaponEventType.MEGAMAN_TRANS) animKey += "_reversed"

                    val animator = megamanAnimators.get(animKey)
                    animator.animate(megamanSprite, delta)
                } catch (e: Exception) {
                    throw Exception("Failed to animate animator for event=$eventType, weapon=$weapon", e)
                }
            }

            GetWeaponEventType.END_EVENT -> {
                blinkTextDelay.update(delta)
                if (blinkTextDelay.isFinished()) {
                    blinkText = !blinkText
                    blinkTextDelay.reset()
                }

                if (game.controllerPoller.isAnyJustReleased(END_BUTTONS)) {
                    GameLogger.debug(TAG, "render(): end event: go to next screen")
                    game.setCurrentScreen(ScreenEnum.SAVE_GAME_SCREEN.name)
                }
            }
        }
    }

    override fun draw(drawer: Batch) {
        val batch = game.batch
        batch.projectionMatrix = game.getUiCamera().combined
        batch.begin()

        backgroundSprite.draw(drawer)
        overlaySprite.draw(drawer)
        megamanSprite.draw(drawer)

        val doRenderText = if (currentEventType == GetWeaponEventType.END_EVENT) blinkText else true
        if (doRenderText) textHandle.draw(drawer)

        batch.end()
    }

    override fun reset() {
        lettersQ.clear()
        eventQueue.clear()
        textHandle.clearText()
        megamanAnimators.values().forEach { animator ->
            (animator as Animator).animations
                .values()
                .forEach { animation -> animation.reset() }
        }
    }

    private fun updateEventTimer(delta: Float) {
        eventTimer.update(delta)
        if (eventTimer.isFinished()) {
            val old = eventQueue.removeFirst()

            if (eventQueue.isEmpty) game.setCurrentScreen(ScreenEnum.SAVE_GAME_SCREEN.name)
            else {
                val new = eventQueue.first()
                onChangeEvent(new, old)
                resetEventTimer()
            }
        }
    }

    private fun resetEventTimer() {
        GameLogger.debug(TAG, "resetEventTimer()")

        val eventType = eventQueue.first().type
        eventTimer.resetDuration(EVENT_DURS[eventType.ordinal])
    }

    private fun onChangeEvent(new: GetWeaponEvent, old: GetWeaponEvent) {
        GameLogger.debug(TAG, "onChangeEvent(): new=$new, old=$old")

        when (new.type) {
            GetWeaponEventType.MEGAMAN_TRANS -> textHandle.clearText()
            GetWeaponEventType.WEAPON_EVENT -> {
                val weaponName = new.props.get(ConstKeys.WEAPON, MegamanWeapon::class)!!.name.replace("_", " ")
                for (i in 0 until weaponName.length) {
                    val text = weaponName.subSequence(0, i + 1).toString()
                    val runnable = Runnable { textHandle.setText(text) }
                    lettersQ.addLast(runnable)
                }
            }
            GetWeaponEventType.END_EVENT -> {
                textHandle.setText("PRESS START")
                blinkTextDelay.reset()
                blinkText = false
            }
            else -> {}
        }
    }
}
