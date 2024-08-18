package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.audio.AudioComponent
import com.engine.common.enums.Direction
import com.engine.common.enums.Position
import com.engine.common.extensions.*
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.common.time.Timer
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.drawables.fonts.BitmapFontHandle
import com.engine.drawables.fonts.FontsComponent
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.*
import com.engine.events.Event
import com.engine.events.IEventListener
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.IDirectionRotatable
import com.megaman.maverick.game.entities.overlapsGameCamera
import com.megaman.maverick.game.entities.utils.convertObjectPropsToEntitySuppliers
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType.SPAWN_ROOM
import com.megaman.maverick.game.utils.MegaUtilMethods
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureType
import com.megaman.maverick.game.world.setHitByPlayerReceiver
import com.megaman.maverick.game.world.setHitByProjectileReceiver

class Togglee(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, IParentEntity, ISpritesEntity,
    IAnimatedEntity, IFontsEntity, IAudioEntity, IDirectionRotatable, IDamager, IEventListener {

    enum class ToggleeState {
        TOGGLED_ON, TOGGLED_OFF, TOGGLING_TO_ON, TOGGLING_TO_OFF;

        fun isToggling() = this.equalsAny(TOGGLING_TO_ON, TOGGLING_TO_OFF)
    }

    companion object {
        const val TAG = "Togglee"
        const val ENEMY_TYPE = "enemy"
        const val LEVER_TYPE = "lever"
        const val SWITCHAROO_ARROW_TYPE = "switcharoo_arrow"
        const val TOGGLEE_ON_ENTITY = "togglee_on_entity"
        private const val ENEMY_SWITCH_DUR = 0.45f
        private const val LEVER_SWITCH_DUR = 0.25f
        private const val SWITCHAROO_ARROW_SWITCH_DUR = 0.1f
        private const val SWITCHAROO_ARROW_BLINK_DUR = 0.1f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var children = Array<IGameEntity>()
    override var directionRotation: Direction? = null
    override val eventKeyMask = objectSetOf<Any>(EventType.PLAYER_SPAWN, EventType.END_ROOM_TRANS)

    val moving: Boolean
        get() = toggleeState.equalsAny(ToggleeState.TOGGLING_TO_ON, ToggleeState.TOGGLING_TO_OFF)
    val on: Boolean
        get() = toggleeState == ToggleeState.TOGGLED_ON

    lateinit var type: String
    lateinit var toggleeState: ToggleeState
        private set
    lateinit var text: String
        private set

    private val offEntitySuppliers = Array<Pair<() -> IGameEntity, Properties>>()
    private val onEntitySuppliers = Array<Pair<() -> IGameEntity, Properties>>()

    private val switcharooArrowBlinkTimer = Timer(SWITCHAROO_ARROW_BLINK_DUR)

    private lateinit var switchTimer: Timer
    private lateinit var position: Position
    private lateinit var spawnRoom: String

    private var switcharooAlpha = 1f

    override fun getEntityType() = EntityType.SPECIAL

    override fun init() {
        if (regions.isEmpty) {
            val enemyAtlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            regions.put("${ENEMY_TYPE}/${ToggleeState.TOGGLED_ON.name}", enemyAtlas.findRegion("$TAG/Left"))
            regions.put("${ENEMY_TYPE}/${ToggleeState.TOGGLED_OFF.name}", enemyAtlas.findRegion("$TAG/Right"))
            regions.put("${ENEMY_TYPE}/${ToggleeState.TOGGLING_TO_ON.name}", enemyAtlas.findRegion("$TAG/SwitchToLeft"))
            regions.put(
                "${ENEMY_TYPE}/${ToggleeState.TOGGLING_TO_OFF.name}", enemyAtlas.findRegion("$TAG/SwitchToRight")
            )

            val specialsAtlas = game.assMan.getTextureAtlas(TextureAsset.SPECIALS_1.source)

            regions.put("${LEVER_TYPE}/on", specialsAtlas.findRegion("$TAG/Left"))
            regions.put("${LEVER_TYPE}/off", specialsAtlas.findRegion("$TAG/Right"))

            regions.put(SWITCHAROO_ARROW_TYPE, specialsAtlas.findRegion("SwitcharooArrow"))
        }
        addComponent(AudioComponent())
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(defineFontsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        game.eventsMan.addListener(this)
        super.spawn(spawnProps)

        type = spawnProps.get(ConstKeys.TYPE, String::class)!!

        val size = when (type) {
            ENEMY_TYPE -> Vector2(2f, 2f)
            LEVER_TYPE -> Vector2(1f, 2f)
            SWITCHAROO_ARROW_TYPE -> Vector2(3f, 6f)
            else -> throw IllegalStateException("Invalid type: $type")
        }.scl(ConstVals.PPM.toFloat())
        body.setSize(size)

        position = Position.valueOf(spawnProps.getOrDefault(ConstKeys.POSITION, "center", String::class).uppercase())
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(position)
        body.positionOnPoint(spawn, position)

        val directionString = spawnProps.getOrDefault(ConstKeys.DIRECTION, "up", String::class)
        directionRotation = Direction.valueOf(directionString.uppercase())

        text = spawnProps.getOrDefault(ConstKeys.TEXT, "", String::class)
        getFont(ConstKeys.DEFAULT).position.set(body.getCenter().add(0f, 1.75f * ConstVals.PPM))

        spawnRoom = spawnProps.get(SPAWN_ROOM, String::class)!!

        val childEntitySuppliers = convertObjectPropsToEntitySuppliers(spawnProps)
        childEntitySuppliers.forEach {
            val childType = it.second.getOrDefault(TOGGLEE_ON_ENTITY, true, Boolean::class)
            if (childType) onEntitySuppliers.add(it) else offEntitySuppliers.add(it)
        }

        toggleeState = ToggleeState.TOGGLED_OFF

        val switchDuration = when (type) {
            ENEMY_TYPE -> ENEMY_SWITCH_DUR
            LEVER_TYPE -> LEVER_SWITCH_DUR
            SWITCHAROO_ARROW_TYPE -> SWITCHAROO_ARROW_SWITCH_DUR
            else -> throw IllegalArgumentException("Invalid type: $type")
        }
        switchTimer = Timer(switchDuration).setToEnd()

        spawnEntities(false)

        if (type == SWITCHAROO_ARROW_TYPE) switcharooArrowBlinkTimer.reset()
        switcharooAlpha = 1f
    }

    override fun onDestroy() {
        super<MegaGameEntity>.onDestroy()
        game.eventsMan.removeListener(this)
        children.forEach { it.kill() }
        children.clear()
        offEntitySuppliers.clear()
        onEntitySuppliers.clear()
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.PLAYER_SPAWN -> kill() // this assumes that a player spawn is never in the same room as a Togglee
            EventType.END_ROOM_TRANS -> {
                val newRoom = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                if (spawnRoom != newRoom) kill()
            }
        }
    }

    override fun canDamage(damageable: IDamageable) = type == ENEMY_TYPE

    private fun spawnEntities(on: Boolean) {
        children.forEach { it.kill() }
        children.clear()

        val entitiesToSpawn = if (on) onEntitySuppliers else offEntitySuppliers
        entitiesToSpawn.forEach {
            val entity = it.first.invoke()
            val props = it.second
            game.engine.spawn(entity, props)
            children.add(entity)
        }
    }

    private fun switchToggleeState() {
        toggleeState = if (on) ToggleeState.TOGGLING_TO_OFF else ToggleeState.TOGGLING_TO_ON
        switchTimer.reset()
        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.SELECT_PING_SOUND, false)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        switchTimer.update(delta)
        if (switchTimer.isJustFinished()) {
            toggleeState = if (toggleeState == ToggleeState.TOGGLING_TO_OFF) {
                spawnEntities(false)
                ToggleeState.TOGGLED_OFF
            } else {
                spawnEntities(true)
                ToggleeState.TOGGLED_ON
            }
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBodyBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
        body.addFixture(bodyFixture)
        bodyFixture.getShape().color = Color.GRAY
        debugShapes.add { bodyFixture.getShape() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(ConstVals.PPM.toFloat()))
        body.addFixture(damagerFixture)
        damagerFixture.getShape().color = Color.RED
        debugShapes.add { if (damagerFixture.active) damagerFixture.getShape() else null }

        body.preProcess.put(ConstKeys.DEFAULT) {
            (bodyFixture.rawShape as GameRectangle).set(body)
            damagerFixture.active = type == ENEMY_TYPE && !moving
            damagerFixture.offsetFromBodyCenter.x =
                if (type == ENEMY_TYPE) (if (on) 0.5f else -0.5f) * ConstVals.PPM else 0f
        }
        body.setHitByProjectileReceiver {
            if (type != SWITCHAROO_ARROW_TYPE && switchTimer.isFinished()) switchToggleeState()
        }
        body.setHitByPlayerReceiver {
            if (type == SWITCHAROO_ARROW_TYPE && switchTimer.isFinished()) switchToggleeState()
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { delta, _sprite ->
            val size = when (type) {
                LEVER_TYPE -> vector2Of(0.5f)
                ENEMY_TYPE -> vector2Of(2f)
                SWITCHAROO_ARROW_TYPE -> vector2Of(6f)
                else -> throw IllegalStateException("Unknown type: $type")
            }.scl(ConstVals.PPM.toFloat())
            _sprite.setSize(size)
            _sprite.setPosition(body.getPositionPoint(position), position)
            _sprite.setFlip(false, type == SWITCHAROO_ARROW_TYPE && on)
            if (type == SWITCHAROO_ARROW_TYPE) {
                switcharooArrowBlinkTimer.update(delta)
                if (switcharooArrowBlinkTimer.isFinished()) {
                    switcharooAlpha -= 0.1f
                    if (switcharooAlpha < 0f) switcharooAlpha = 1f
                    switcharooArrowBlinkTimer.reset()
                }
            }
            _sprite.setAlpha(if (type == SWITCHAROO_ARROW_TYPE) switcharooAlpha else 1f)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (type) {
                ENEMY_TYPE -> "${type}/${toggleeState.name}"
                LEVER_TYPE -> "${type}/${if (on) "on" else "off"}"
                SWITCHAROO_ARROW_TYPE -> SWITCHAROO_ARROW_TYPE
                else -> throw IllegalStateException("Invalid type: $type")
            }
        }

        val animations = ObjectMap<String, IAnimation>()

        regions.forEach { entry ->
            if (entry.key == SWITCHAROO_ARROW_TYPE) {
                animations.put(entry.key, Animation(entry.value))
                return@forEach
            }

            val keyParts = entry.key.split("/")
            val type = keyParts[0]

            val region = entry.value

            val animation = when (type) {
                ENEMY_TYPE -> {
                    val state = ToggleeState.valueOf(keyParts[1])
                    if (state.isToggling()) Animation(region, 1, 3, 0.1f, false)
                    else Animation(region, 1, 2, gdxArrayOf(1f, 0.15f), true)
                }

                LEVER_TYPE -> {
                    val state = keyParts[1]
                    if (state == "on") Animation(regions.get("$type/on"))
                    else Animation(regions.get("$type/off"))
                }

                else -> throw IllegalStateException("Invalid type: $type")
            }

            animations.put(entry.key, animation)
        }

        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }

    private fun defineFontsComponent(): FontsComponent {
        val font = BitmapFontHandle(
            { text }, MegaUtilMethods.getDefaultFontSize(), fontSource = ConstVals.MEGAMAN_MAVERICK_FONT
        )
        return FontsComponent(ConstKeys.DEFAULT to font)
    }
}