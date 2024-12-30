package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

abstract class Switch(game: MegamanMaverickGame) : MegaGameEntity(game) {

    companion object {
        const val TAG = "Switch"
        protected val regions = ObjectMap<String, TextureRegion>()
    }

    enum class SwitchState { UP, SWITCH_TO_DOWN, SWITCH_TO_UP, DOWN }

    lateinit var state: SwitchState
        private set

    override fun init() {
        GameLogger.debug(TAG, "init()")

        super.init()

        val updatablesComponent = UpdatablesComponent()
        defineUpdatablesComponent(updatablesComponent)
        addComponent(updatablesComponent)
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        state = getStateOnSpawn(spawnProps)
    }

    protected open fun getStateOnSpawn(spawnProps: Properties) = SwitchState.UP

    protected open fun defineUpdatablesComponent(component: UpdatablesComponent) {
        component.put(ConstKeys.STATE) { delta ->
            when (state) {
                SwitchState.UP -> if (shouldBeginSwitchToDown(delta)) {
                    state = SwitchState.SWITCH_TO_DOWN
                    onBeginSwitchToDown()
                }

                SwitchState.DOWN -> if (shouldBeginSwitchToUp(delta)) {
                    state = SwitchState.SWITCH_TO_UP
                    onBeginSwitchToUp()
                }

                SwitchState.SWITCH_TO_DOWN -> if (shouldFinishSwitchToDown(delta)) {
                    state = SwitchState.DOWN
                    onFinishSwitchToDown()
                }

                SwitchState.SWITCH_TO_UP -> if (shouldFinishSwitchToUp(delta)) {
                    state = SwitchState.UP
                    onFinishSwitchToUp()
                }
            }
        }
    }

    protected abstract fun shouldBeginSwitchToDown(delta: Float): Boolean

    protected abstract fun shouldBeginSwitchToUp(delta: Float): Boolean

    protected abstract fun shouldFinishSwitchToDown(delta: Float): Boolean

    protected abstract fun shouldFinishSwitchToUp(delta: Float): Boolean

    protected open fun onBeginSwitchToDown() {}

    protected open fun onBeginSwitchToUp() {}

    protected open fun onFinishSwitchToDown() {}

    protected open fun onFinishSwitchToUp() {}

    override fun getTag() = TAG

    override fun getEntityType() = EntityType.SPECIAL
}
