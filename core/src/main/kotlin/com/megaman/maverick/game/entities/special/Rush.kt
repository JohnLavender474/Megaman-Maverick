package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.orderedMapOf
import com.maverick.game.engine.common.interfaces.IFaceable
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.common.time.Timer
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setPosition
import com.maverick.game.engine.drawables.sprites.setSize
import com.maverick.game.engine.entities.EntityExistenceState
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.events.Event
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.handlers.RushHandle
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.entities.state.teleporting
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.levels.LevelFreezeRoutine
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getRotatedBounds
import com.megaman.maverick.game.weapons.WeaponDef
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.Receivers
import com.megaman.maverick.game.world.body.addBlockFilter
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getPositionPoint
import com.megaman.maverick.game.world.body.isSensing
import com.megaman.maverick.game.world.body.setBodySense
import com.megaman.maverick.game.world.body.setReceiver

enum class RushState { BEAMING_DOWN, LANDING, SITTING, JOINING, BEAMING_OUT, BEAMING_UP }

class Rush(services: MegaServices) : MegaGameEntity(services), RushHandle, IBodyEntity, ISpritesEntity,
    IAnimatedEntity, IFaceable, LevelFreezeRoutine {

    companion object {
        const val TAG = "Rush"

        private const val BODY_WIDTH = 1.25f
        private const val BODY_HEIGHT = 1f
        private const val FEET_WIDTH = 0.75f
        private const val FEET_HEIGHT = 0.1f
        private const val SPRITE_SIZE = 2f

        private const val BEAM_SPEED = 20f
        private const val GRAVITY = 37.5f
        private const val GROUND_GRAVITY = 1.5f

        private const val SPAWN_OFFSET_X = 1.5f
        private const val BLOCKS_IGNORED_ABOVE_OWNER = 2f
        private const val BACK_OFFSET_X = 0.5f

        private const val LAND_DUR = 0.15f
        private const val BEAM_OUT_DUR = 0.15f
        private const val JOIN_DUR = 0.6f

        private const val BEAM_KEY = "beam"
        private const val LAND_KEY = "land"
        private const val SIT_KEY = "sit"
        private const val BEAM_OUT_KEY = "beam_out"
        private const val JOIN_KEY_PREFIX = "join_"

        private val animDefs = orderedMapOf(
            BEAM_KEY pairTo AnimationDef(1, 1, 1f, loop = true),
            LAND_KEY pairTo AnimationDef(1, 3, LAND_DUR / 3f, loop = false),
            SIT_KEY pairTo AnimationDef(1, 2, 0.25f, loop = true),
            "${JOIN_KEY_PREFIX}rush_jet" pairTo AnimationDef(1, 4, JOIN_DUR / 4f, loop = false),
            BEAM_OUT_KEY pairTo AnimationDef(1, 3, BEAM_OUT_DUR / 3f, loop = false)
        )
        private val regions = ObjectMap<String, TextureRegion>()

        fun joinKey(weapon: WeaponDef<*>) = "$JOIN_KEY_PREFIX${weapon.id}"
    }

    override var facing = Facing.RIGHT

    var state = RushState.BEAMING_DOWN
        private set

    private lateinit var owner: Megaman
    private var joinAnimKey = ""
    private var leaveOnSpawn = false
    private val stateTimer = Timer(LAND_DUR)
    private val joinStart = Vector2()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.RUSH.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        addComponent(UpdatablesComponent({ delta -> updateState(delta) }))
    }

    override fun arrive(owner: Megaman, weapon: WeaponDef<Megaman>): Boolean {
        val view = services.gameCamera.getRotatedBounds()
        val x = owner.body.getCenter().x + SPAWN_OFFSET_X * ConstVals.PPM * owner.facing.value
        val position = GameObjectPools.fetch(Vector2::class).set(x, view.getMaxY() - BODY_HEIGHT * ConstVals.PPM)
        return spawn(
            props(
                ConstKeys.OWNER pairTo owner,
                ConstKeys.POSITION pairTo position,
                ConstKeys.STATE pairTo RushState.BEAMING_DOWN,
                ConstKeys.KEY pairTo joinKey(weapon)
            )
        )
    }

    override fun leave() {
        GameLogger.debug(TAG, "leave(): state=$state, existenceState=$existenceState")
        if (existenceState == EntityExistenceState.SPAWN_QUEUED) leaveOnSpawn = true else startBeamOut()
    }

    override fun leaveFromBack(owner: Megaman): Boolean {
        val position = backPosition(owner, GameObjectPools.fetch(Vector2::class))
        position.y -= BODY_HEIGHT * ConstVals.PPM / 2f
        return spawn(
            props(
                ConstKeys.OWNER pairTo owner,
                ConstKeys.POSITION pairTo position,
                ConstKeys.STATE pairTo RushState.BEAMING_OUT
            )
        )
    }

    override fun vanish() {
        GameLogger.debug(TAG, "vanish()")
        if (existenceState.liveAfterUpdate) destroy()
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        owner = spawnProps.get(ConstKeys.OWNER, Megaman::class)!!
        facing = owner.facing
        state = spawnProps.get(ConstKeys.STATE, RushState::class)!!
        joinAnimKey = spawnProps.getOrDefault(ConstKeys.KEY, "", String::class)
        if (state == RushState.BEAMING_DOWN && !regions.containsKey(joinAnimKey)) throw IllegalStateException(
            "$TAG has no join animation '$joinAnimKey': add its frames to ${TextureAsset.RUSH.source} and animDefs"
        )
        body.setBottomCenterToPoint(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)
        body.physics.gravityOn = false
        body.physics.velocity.setZero()
        body.setBodySense(BodySense.FEET_ON_GROUND, false)
        when {
            state == RushState.BEAMING_OUT || leaveOnSpawn -> startBeamOut()
            state == RushState.BEAMING_DOWN -> body.physics.velocity.y = -BEAM_SPEED * ConstVals.PPM
            else -> throw IllegalStateException("$TAG cannot spawn in state $state")
        }
        leaveOnSpawn = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy(): state=$state")
        super.onDestroy()
        owner.rushHandler.onRushGone(this)
    }

    override fun update(delta: Float): Boolean {
        if (state != RushState.JOINING) return true
        stateTimer.update(delta)
        val back = backPosition(owner, GameObjectPools.fetch(Vector2::class))
        val center = GameObjectPools.fetch(Vector2::class).set(joinStart).lerp(back, stateTimer.getRatio())
        body.setCenter(center)
        forEachAnimator { _, sprite, animator -> animator.animate(sprite, delta) }
        if (!stateTimer.isFinished()) return false
        GameLogger.debug(TAG, "update(): join finished")
        owner.rushHandler.onJoined(this)
        destroy()
        return true
    }

    private fun updateState(delta: Float) {
        when (state) {
            RushState.BEAMING_DOWN -> {
                body.physics.velocity.set(0f, -BEAM_SPEED * ConstVals.PPM)
                if (!ignoresBlocks() && body.isSensing(BodySense.FEET_ON_GROUND)) startLanding()
            }

            RushState.LANDING -> {
                stateTimer.update(delta)
                if (stateTimer.isFinished()) state = RushState.SITTING
            }

            RushState.SITTING -> if (canJoin()) startJoin()

            RushState.JOINING -> {}

            RushState.BEAMING_OUT -> {
                stateTimer.update(delta)
                if (stateTimer.isFinished()) {
                    state = RushState.BEAMING_UP
                    body.physics.velocity.set(0f, BEAM_SPEED * ConstVals.PPM)
                }
            }

            RushState.BEAMING_UP -> body.physics.velocity.set(0f, BEAM_SPEED * ConstVals.PPM)
        }

        if (!overlapsGameCamera()) {
            GameLogger.debug(TAG, "update(): out of the view: state=$state")
            destroy()
        }
    }

    private fun startLanding() {
        GameLogger.debug(TAG, "startLanding()")
        state = RushState.LANDING
        stateTimer.resetDuration(LAND_DUR)
        body.physics.velocity.setZero()
        services.audio.playSound(SoundAsset.BEAM_SOUND, loop = false)
    }

    private fun startBeamOut() {
        state = RushState.BEAMING_OUT
        stateTimer.resetDuration(BEAM_OUT_DUR)
        body.physics.gravityOn = false
        body.physics.velocity.setZero()
        services.audio.playSound(SoundAsset.BEAM_OUT_SOUND, loop = false)
    }

    private fun canJoin(): Boolean {
        // TODO: join in a turned room, where the gravity is not UP (item 190)
        if (!owner.inPlay || owner.damaged || owner.teleporting || owner.direction != Direction.UP) return false
        return owner.body.getBounds().overlaps(body.getBounds())
    }

    private fun startJoin() {
        if (!owner.rushHandler.onJoinStarted(this)) return
        GameLogger.debug(TAG, "startJoin()")
        state = RushState.JOINING
        stateTimer.resetDuration(JOIN_DUR)
        joinStart.set(body.getCenter())
        body.physics.gravityOn = false
        body.physics.velocity.setZero()
        services.audio.playSound(SoundAsset.BEAM_SOUND, loop = false)
        services.events.submitEvent(Event(EventType.START_FREEZE_ROUTINE, props(ConstKeys.VALUE pairTo this)))
    }

    private fun backPosition(owner: Megaman, out: Vector2): Vector2 {
        out.set(owner.body.getCenter())
        out.x -= BACK_OFFSET_X * ConstVals.PPM * owner.facing.value
        return out
    }

    // TODO: Rush can land inside a tall wall whose top is above the ignore line; accepted for the first pass (item 190)
    private fun ignoresBlocks() = when (state) {
        RushState.BEAMING_DOWN ->
            body.getY() > owner.body.getBounds().getMaxY() + BLOCKS_IGNORED_ABOVE_OWNER * ConstVals.PPM

        RushState.LANDING, RushState.SITTING -> false
        RushState.JOINING, RushState.BEAMING_OUT, RushState.BEAMING_UP -> true
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.physics.velocityClamp.set(BEAM_SPEED * ConstVals.PPM, BEAM_SPEED * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture = Fixture(
            body,
            FixtureType.FEET,
            GameRectangle().setSize(FEET_WIDTH * ConstVals.PPM, FEET_HEIGHT * ConstVals.PPM)
        )
        feetFixture.offsetFromBodyAttachment.y = -BODY_HEIGHT * ConstVals.PPM / 2f
        feetFixture.setReceiver(Receivers.STICK_TO_BLOCK) { _, _ -> state == RushState.SITTING }
        feetFixture.drawingColor = Color.GREEN
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture }

        body.addBlockFilter { _, _ -> ignoresBlocks() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val sits = state == RushState.LANDING || state == RushState.SITTING
            body.physics.gravityOn = sits
            val gravity = if (body.isSensing(BodySense.FEET_ON_GROUND)) GROUND_GRAVITY else GRAVITY
            body.physics.gravity.set(0f, -gravity * ConstVals.PPM)
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
        sprite.setSize(SPRITE_SIZE * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            sprite.setPosition(body.getPositionPoint(Position.BOTTOM_CENTER), Position.BOTTOM_CENTER)
            sprite.setFlip(facing == Facing.LEFT, false)
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = {
            when (state) {
                RushState.BEAMING_DOWN, RushState.BEAMING_UP -> BEAM_KEY
                RushState.LANDING -> LAND_KEY
                RushState.SITTING -> SIT_KEY
                RushState.JOINING -> joinAnimKey
                RushState.BEAMING_OUT -> BEAM_OUT_KEY
            }
        }
        val animations = ObjectMap<String, IAnimation>()
        animDefs.forEach { entry ->
            val (rows, columns, durations, loop) = entry.value
            animations.put(entry.key, Animation(regions[entry.key], rows, columns, durations, loop))
        }
        return AnimationsComponent(this, Animator(keySupplier, animations))
    }

    override fun getType() = EntityType.SPECIAL
}
