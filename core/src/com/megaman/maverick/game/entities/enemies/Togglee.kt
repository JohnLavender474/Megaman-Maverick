package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.extensions.equalsAny
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureRegion
import com.engine.common.extensions.objectMapOf
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamager
import com.engine.drawables.fonts.BitmapFontHandle
import com.engine.drawables.fonts.FontsComponent
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IFontsEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.utils.convertObjectPropsToEntities
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class Togglee(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity,
    IDirectionRotatable, IFontsEntity {

    enum class ToggleeState {
        TOGGLED_ON, TOGGLED_OFF, TOGGLING_TO_ON, TOGGLING_TO_OFF
    }

    companion object {
        const val TAG = "Toggle"
        const val TOGGLEE_ON_ENTITY = "togglee_on_entity"
        private var leftRegion: TextureRegion? = null
        private var rightRegion: TextureRegion? = null
        private var switchLeftRegion: TextureRegion? = null
        private var switchRightRegion: TextureRegion? = null
        private const val SWITCH_DURATION = 0.45f
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(10), Fireball::class to dmgNeg(ConstVals.MAX_HEALTH), ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) ConstVals.MAX_HEALTH else 15
        }, ChargedShotExplosion::class to dmgNeg(15)
    )
    override val invincible: Boolean
        get() = super.invincible || moving

    val moving: Boolean
        get() = toggleeState.equalsAny(ToggleeState.TOGGLING_TO_ON, ToggleeState.TOGGLING_TO_OFF)
    val on: Boolean
        get() = toggleeState == ToggleeState.TOGGLED_ON

    lateinit var toggleeState: ToggleeState
        private set
    lateinit var text: String
        private set

    override lateinit var directionRotation: Direction

    private val offEntities = Array<Pair<IGameEntity, Properties>>()
    private val onEntities = Array<Pair<IGameEntity, Properties>>()
    private val switchTimer = Timer(SWITCH_DURATION)

    override fun init() {
        super<AbstractEnemy>.init()
        if (leftRegion == null || rightRegion == null || switchLeftRegion == null || switchRightRegion == null) {
            leftRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "Togglee/Left")
            rightRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "Togglee/Right")
            switchLeftRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "Togglee/SwitchToLeft")
            switchRightRegion = game.assMan.getTextureRegion(TextureAsset.ENEMIES_2.source, "Togglee/SwitchToRight")
        }
        addComponent(defineAnimationsComponent())
        addComponent(defineFontsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getCenter()
        body.setCenter(spawn)

        val directionString = spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class)
        directionRotation = Direction.valueOf(directionString.uppercase())

        text = spawnProps.get(ConstKeys.TEXT, String::class)!!
        getFont(ConstKeys.DEFAULT).position.set(body.getCenter().add(0f, 1.75f * ConstVals.PPM))

        val childEntities = convertObjectPropsToEntities(spawnProps)
        GameLogger.debug(TAG, "Child entities: ${childEntities.map { "${it.first}:${it.second} " }}")
        childEntities.forEach {
            val childType = it.second.get(TOGGLEE_ON_ENTITY, Boolean::class)!!
            if (childType) onEntities.add(it)
            else offEntities.add(it)
        }

        toggleeState = ToggleeState.TOGGLED_OFF
        switchTimer.setToEnd()

        spawnEntities(false)
    }

    override fun onDestroy() {
        super<AbstractEnemy>.onDestroy()
        offEntities.forEach { it.first.kill() }
        offEntities.clear()
        onEntities.forEach { it.first.kill() }
        onEntities.clear()
    }

    override fun takeDamageFrom(damager: IDamager): Boolean {
        /*
        val takenDamage = super.takeDamageFrom(damager)
        if (takenDamage) switchToggleeState()
        return takenDamage
         */
        switchToggleeState()
        return false
    }

    private fun spawnEntities(on: Boolean) {
        val entitiesToKill = if (on) offEntities else onEntities
        val entitiesToSpawn = if (on) onEntities else offEntities
        entitiesToKill.forEach { it.first.kill() }
        entitiesToSpawn.forEach { game.gameEngine.spawn(it.first, it.second) }
    }

    private fun switchToggleeState() {
        toggleeState = if (on) ToggleeState.TOGGLING_TO_OFF else ToggleeState.TOGGLING_TO_ON
        switchTimer.reset()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add {
            if (!switchTimer.isFinished()) {
                switchTimer.update(it)
                if (switchTimer.isJustFinished()) {
                    toggleeState = if (toggleeState == ToggleeState.TOGGLING_TO_OFF) {
                        spawnEntities(false)
                        ToggleeState.TOGGLED_OFF
                    } else {
                        spawnEntities(true)
                        ToggleeState.TOGGLED_ON
                    }
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM, ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle().setSize(body.getSize()))
        body.addFixture(bodyFixture)
        bodyFixture.getShape().color = Color.GRAY
        debugShapes.add { bodyFixture.getShape() }

        val damagerFixture = Fixture(
            body,
            FixtureType.DAMAGER,
            GameRectangle().setSize(ConstVals.PPM.toFloat())
        )
        body.addFixture(damagerFixture)
        damagerFixture.getShape().color = Color.RED
        debugShapes.add { damagerFixture.getShape() }

        val damageableFixture = Fixture(
            body,
            FixtureType.DAMAGEABLE,
            GameRectangle().setSize(ConstVals.PPM.toFloat())
        )
        body.addFixture(damageableFixture)
        damageableFixture.getShape().color = Color.PURPLE
        debugShapes.add { damageableFixture.getShape() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            damagerFixture.active = !moving
            damageableFixture.active = !moving
            val fixtureOffsetX = if (on) 0.5f * ConstVals.PPM else -0.5f * ConstVals.PPM
            damagerFixture.offsetFromBodyCenter.x = fixtureOffsetX
            damageableFixture.offsetFromBodyCenter.x = fixtureOffsetX
        }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(2f * ConstVals.PPM)

        val spritesComponent = SpritesComponent(this, "togglee" to sprite)
        spritesComponent.putUpdateFunction("togglee") { _, _sprite ->
            _sprite as GameSprite
            val center = body.getCenter()
            _sprite.setCenter(center.x, center.y)
        }

        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = { toggleeState.name }
        val animations = objectMapOf<String, IAnimation>(
            ToggleeState.TOGGLED_OFF.name to Animation(leftRegion!!, 1, 2, gdxArrayOf(1f, 0.15f), true),
            ToggleeState.TOGGLED_ON.name to Animation(rightRegion!!, 1, 2, gdxArrayOf(1f, 0.15f), true),
            ToggleeState.TOGGLING_TO_ON.name to Animation(switchRightRegion!!, 1, 3, 0.1f, false),
            ToggleeState.TOGGLING_TO_OFF.name to Animation(switchLeftRegion!!, 1, 3, 0.1f, false)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun defineFontsComponent(): FontsComponent {
        val font = BitmapFontHandle(
            { text }, MegaUtilMethods.getDefaultFontSize(), fontSource = ConstVals.MEGAMAN_MAVERICK_FONT
        )
        return FontsComponent(this, ConstKeys.DEFAULT to font)
    }

}