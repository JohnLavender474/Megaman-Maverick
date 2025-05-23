package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IParentEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.enemies.CactusLauncher.CactusLauncherState.*
import com.megaman.maverick.game.entities.projectiles.CactusMissile
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class CactusLauncher(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IParentEntity<CactusMissile>,
    IAnimatedEntity {

    companion object {
        const val TAG = "CactusLauncher"
        private const val WAIT_DUR = 0.75f
        private const val FIRE_DUR = 0.5f
        private const val RELOAD_DUR = 0.5f
        private const val MAX_CHILDREN = 2
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class CactusLauncherState { WAIT, FIRE, RELOAD }

    override var children = Array<CactusMissile>()

    private val loop = Loop(CactusLauncherState.entries.toGdxArray())
    private val timers = objectMapOf(
        "wait" pairTo Timer(WAIT_DUR),
        "fire" pairTo Timer(FIRE_DUR),
        "reload" pairTo Timer(RELOAD_DUR)
    )

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            gdxArrayOf("fire", "reload", "wait").forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        loop.reset()
        timers.values().forEach { it.reset() }
    }

    override fun onDestroy() {
        super.onDestroy()
        children.clear()
    }

    private fun launchMissile() {
        val missile = MegaEntityFactory.fetch(CactusMissile::class)!!
        missile.spawn(props(ConstKeys.POSITION pairTo body.getPositionPoint(Position.TOP_CENTER)))

        children.add(missile)

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.CHILL_SHOOT_SOUND, false)
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            val iter = children.iterator()
            while (iter.hasNext()) {
                val child = iter.next() as MegaGameEntity
                if (child.dead) iter.remove()
            }

            if (children.size >= MAX_CHILDREN) {
                loop.setIndex(1)
                return@add
            }

            val key = when (loop.getCurrent()) {
                WAIT -> "wait"
                FIRE -> "fire"
                RELOAD -> "reload"
            }
            val timer = timers[key]

            timer.update(delta)

            if (timer.isFinished()) {
                loop.next()
                if (loop.getCurrent() == FIRE) launchMissile()

                timer.reset()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this, body, BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _ ->
            val bodyPosition = body.getPositionPoint(Position.BOTTOM_CENTER)
            sprite.setPosition(bodyPosition, Position.BOTTOM_CENTER)

            sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            when (loop.getCurrent()) {
                WAIT -> "wait"
                FIRE -> "fire"
                RELOAD -> "reload"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "wait" pairTo Animation(regions["wait"]),
            "fire" pairTo Animation(regions["fire"], 2, 1, 0.1f, false),
            "reload" pairTo Animation(regions["reload"], 2, 1, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
