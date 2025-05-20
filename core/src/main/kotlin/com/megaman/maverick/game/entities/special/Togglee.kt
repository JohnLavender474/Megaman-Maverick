package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.animations.IAnimation
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.equalsAny
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IDirectional
import com.mega.game.engine.common.objects.GamePair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.fonts.FontsComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.*
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.hazards.Lava
import com.megaman.maverick.game.entities.utils.convertObjectPropsToEntitySuppliers
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType.SPAWN_ROOM
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class Togglee(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, IParentEntity<IGameEntity>,
    ISpritesEntity, IAnimatedEntity, IFontsEntity, IAudioEntity, IDirectional, IDamager, IEventListener {

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

    override var direction = Direction.UP
    override var children = Array<IGameEntity>()
    override val eventKeyMask = objectSetOf<Any>(EventType.PLAYER_SPAWN, EventType.END_ROOM_TRANS)

    val moving: Boolean
        get() = state.equalsAny(ToggleeState.TOGGLING_TO_ON, ToggleeState.TOGGLING_TO_OFF)
    val on: Boolean
        get() = state == ToggleeState.TOGGLED_ON

    lateinit var type: String
    lateinit var state: ToggleeState
        private set
    /*
    lateinit var text: String
        private set
     */

    private val offEntitySuppliers = Array<GamePair<() -> MegaGameEntity, Properties>>()
    private val onEntitySuppliers = Array<GamePair<() -> MegaGameEntity, Properties>>()

    private val switcharooArrowBlinkTimer = Timer(SWITCHAROO_ARROW_BLINK_DUR)

    private lateinit var switchTimer: Timer
    private lateinit var spawnRoom: String
    private lateinit var position: Position

    private var switcharooAlpha = 1f

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

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        type = spawnProps.get(ConstKeys.TYPE, String::class)!!

        val size = GameObjectPools.fetch(Vector2::class)
        when (type) {
            ENEMY_TYPE -> size.set(2f, 2f)
            LEVER_TYPE -> size.set(0.75f, 0.75f)
            SWITCHAROO_ARROW_TYPE -> size.set(3f, 6f)
            else -> throw IllegalStateException("Invalid type: $type")
        }
        size.scl(ConstVals.PPM.toFloat())
        body.setSize(size)

        position =
            Position.valueOf(spawnProps.getOrDefault(ConstKeys.POSITION, ConstKeys.CENTER, String::class).uppercase())
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(position)
        body.positionOnPoint(spawn, position)

        val directionString = spawnProps.getOrDefault(ConstKeys.DIRECTION, ConstKeys.UP, String::class)
        direction = Direction.valueOf(directionString.uppercase())

        // text = spawnProps.getOrDefault(ConstKeys.TEXT, "", String::class)
        val position = body.getCenter().add(0f, 1.75f * ConstVals.PPM)
        // getFont(ConstKeys.DEFAULT).setPosition(position.x, position.y)
        // TODO: Togglee does not yet support displaying text

        spawnRoom = spawnProps.get(SPAWN_ROOM, String::class)!!

        val childEntitySuppliers = convertObjectPropsToEntitySuppliers(spawnProps)
        childEntitySuppliers.forEach {
            val childType = it.second.getOrDefault(TOGGLEE_ON_ENTITY, true, Boolean::class)
            if (childType) onEntitySuppliers.add(it) else offEntitySuppliers.add(it)
        }

        state = ToggleeState.TOGGLED_OFF

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
        super.onDestroy()

        game.eventsMan.removeListener(this)

        children.forEach { (it as GameEntity).destroy() }
        children.clear()

        offEntitySuppliers.clear()
        onEntitySuppliers.clear()
    }

    override fun onEvent(event: Event) {
        when (event.key) {
            EventType.PLAYER_SPAWN -> destroy() // this assumes that a player spawn is never in the same room as a Togglee
            EventType.END_ROOM_TRANS -> {
                val newRoom = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                if (spawnRoom != newRoom) destroy()
            }
        }
    }

    override fun canDamage(damageable: IDamageable) = type == ENEMY_TYPE

    private fun spawnEntities(on: Boolean) {
        children.forEach {
            if (it is Lava && it.moveBeforeKill && !it.movingBeforeKill) it.moveBeforeKill()
            else (it as GameEntity).destroy()
        }
        children.clear()

        val entitiesToSpawn = if (on) onEntitySuppliers else offEntitySuppliers
        entitiesToSpawn.forEach {
            val entity = it.first.invoke()
            val props = it.second
            entity.spawn(props)
            children.add(entity)
        }
    }

    private fun switchToggleeState() {
        state = if (on) ToggleeState.TOGGLING_TO_OFF else ToggleeState.TOGGLING_TO_ON
        switchTimer.reset()

        if (overlapsGameCamera()) requestToPlaySound(SoundAsset.SELECT_PING_SOUND, false)
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        switchTimer.update(delta)
        if (switchTimer.isJustFinished()) {
            state = if (state == ToggleeState.TOGGLING_TO_OFF) {
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
        debugShapes.add { body.getBounds() }

        val bodyFixture = Fixture(body, FixtureType.BODY, GameRectangle())
        bodyFixture.setHitByPlayerReceiver { if (switchTimer.isFinished()) switchToggleeState() }
        bodyFixture.setHitByProjectileReceiver {
            if (type != SWITCHAROO_ARROW_TYPE && switchTimer.isFinished()) switchToggleeState()
        }
        body.addFixture(bodyFixture)
        debugShapes.add { bodyFixture }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(ConstVals.PPM.toFloat()))
        body.addFixture(damagerFixture)
        debugShapes.add { if (damagerFixture.isActive()) damagerFixture.getShape() else null }

        body.preProcess.put(ConstKeys.DEFAULT) {
            (bodyFixture.rawShape as GameRectangle).set(body)
            damagerFixture.setActive(type == ENEMY_TYPE && !moving)
            damagerFixture.offsetFromBodyAttachment.x =
                if (type == ENEMY_TYPE) (if (on) 0.5f else -0.5f) * ConstVals.PPM else 0f
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { delta, _ ->
            val size = GameObjectPools.fetch(Vector2::class)
            when (type) {
                LEVER_TYPE -> size.set(0.75f, 0.75f)
                ENEMY_TYPE -> size.set(2f, 2f)
                SWITCHAROO_ARROW_TYPE -> size.set(6f, 6f)
                else -> throw IllegalStateException("Unknown type: $type")
            }
            size.scl(ConstVals.PPM.toFloat())
            sprite.setSize(size)

            sprite.setPosition(body.getPositionPoint(position), position)

            sprite.setFlip(false, type == SWITCHAROO_ARROW_TYPE && on)

            if (type == SWITCHAROO_ARROW_TYPE) {
                switcharooArrowBlinkTimer.update(delta)
                if (switcharooArrowBlinkTimer.isFinished()) {
                    switcharooAlpha -= 0.1f
                    if (switcharooAlpha < 0f) switcharooAlpha = 1f
                    switcharooArrowBlinkTimer.reset()
                }
            }

            sprite.setAlpha(if (type == SWITCHAROO_ARROW_TYPE) switcharooAlpha else 1f)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            when (type) {
                ENEMY_TYPE -> "${type}/${state.name}"
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
                    val toggleeState = ToggleeState.valueOf(keyParts[1])
                    if (toggleeState.isToggling()) Animation(region, 1, 3, 0.1f, false)
                    else Animation(region, 1, 2, gdxArrayOf(1f, 0.15f), true)
                }

                LEVER_TYPE -> {
                    val toggleeState = keyParts[1]
                    if (toggleeState == "on") Animation(regions.get("$type/on"))
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
        // val font = MegaFontHandle(text = text)
        return FontsComponent(/*ConstKeys.DEFAULT pairTo font*/)
        // TODO: fonts component should accept mega font, or should sprites component be used instead?
    }

    override fun getType() = EntityType.SPECIAL
}
