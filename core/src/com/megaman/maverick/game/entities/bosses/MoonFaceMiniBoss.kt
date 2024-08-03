package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.toGdxArray
import com.engine.common.getRandom
import com.engine.common.objects.Loop
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.toGameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.sprites.SpritesComponent
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import kotlin.reflect.KClass

class MoonFaceMiniBoss(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity {

    companion object {
        const val TAG = "MoonFaceMiniBoss"
        private const val DELAY = 0.25f
        private const val DARK_DUR = 0.5f
        private const val AWAKEN_DUR = 0.75f
        private const val SHOOT_INIT_DELAY = 0.5f
        private const val SHOOT_DELAY = 0.1f
        private const val SHOOT_DUR = 1.5f
        private const val CRUMBLE_DUR = 0.325f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class MoonFaceState {
        DELAY,
        DARK,
        AWAKENING,
        SHOOT,
        MOVE,
        CRUMBLING,
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()

    private val loop = Loop(MoonFaceState.values().toGdxArray())
    private val timers = objectMapOf(
        "delay" to Timer(DELAY),
        "dark" to Timer(DARK_DUR),
        "awaken" to Timer(AWAKEN_DUR),
        "shoot_init" to Timer(SHOOT_INIT_DELAY),
        "shoot_delay" to Timer(SHOOT_DELAY),
        "shoot" to Timer(SHOOT_DUR),
        "crumble" to Timer(CRUMBLE_DUR),
    )
    private lateinit var spawnArea: GameRectangle

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            regions.put("dark", atlas.findRegion("$TAG/Dark"))
            regions.put("awaken", atlas.findRegion("$TAG/Awaken"))
            regions.put("angry", atlas.findRegion("$TAG/Angry"))
            regions.put("shoot", atlas.findRegion("$TAG/Shoot"))
            regions.put("crumble", atlas.findRegion("$TAG/Crumble"))
        }
        super<AbstractBoss>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)
        spawnArea = spawnProps.get(ConstKeys.AREA, RectangleMapObject::class)!!.rectangle.toGameRectangle()
        loop.reset()
        timers.values().forEach { it.reset() }
    }

    private fun getRandomSpawn(): Vector2 {
        val randomX = getRandom(spawnArea.x, spawnArea.getMaxX())
        val randomY = getRandom(spawnArea.y, spawnArea.getMaxY())
        return Vector2(randomX, randomY)
    }

    private fun shoot() {

    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!ready) return@add
            if (defeated) {
                explodeOnDefeat(delta)
                return@add
            }

            when (loop.getCurrent()) {
                MoonFaceState.DELAY, MoonFaceState.DARK, MoonFaceState.AWAKENING -> {
                    val key = if (loop.getCurrent() == MoonFaceState.DELAY) "delay"
                    else if (loop.getCurrent() == MoonFaceState.DARK) "dark"
                    else "awaken"

                    val timer = timers[key]

                    timer.update(delta)
                    if (timer.isFinished()) {
                        timer.reset()
                        loop.next()
                        if (loop.getCurrent() == MoonFaceState.DARK) body.setCenter(getRandomSpawn())
                    }
                }

                MoonFaceState.SHOOT -> {
                    val shootInitTimer = timers["shoot_init"]
                    shootInitTimer.update(delta)

                    val shootTimer = timers["shoot"]
                    shootTimer.update(delta)
                    if (shootTimer.isFinished()) {
                        shootInitTimer.reset()
                        loop.next()
                        return@add
                    }

                    if (!shootInitTimer.isFinished()) return@add

                    val shootDelayTimer = timers["shoot_delay"]
                    shootDelayTimer.update(delta)
                    if (shootDelayTimer.isFinished()) {
                        shoot()
                        shootDelayTimer.reset()
                    }
                }

                MoonFaceState.MOVE -> {

                }
                MoonFaceState.CRUMBLING -> TODO()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        TODO("Not yet implemented")
    }

    override fun defineSpritesComponent(): SpritesComponent {
        TODO("Not yet implemented")
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (loop.getCurrent()) {
                MoonFaceState.DELAY -> null
                MoonFaceState.DARK -> "dark"
                MoonFaceState.AWAKENING -> "awaken"
                MoonFaceState.SHOOT -> "shoot"
                MoonFaceState.MOVE -> "angry"
                MoonFaceState.CRUMBLING -> "crumble"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "dark" to Animation(regions.get("dark")),
            "awaken" to Animation(regions.get("awaken"), 1, 7, 0.1f, false),
            "shoot" to Animation(regions.get("shoot")),
            "angry" to Animation(regions.get("angry")),
            "crumble" to Animation(regions.get("crumble"), 1, 3, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}