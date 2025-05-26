package com.megaman.maverick.game.entities.items

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.FeetRiseSinkBlock
import com.megaman.maverick.game.entities.contracts.ILightSource
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegaHeartTank
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.utils.extensions.toProps
import com.megaman.maverick.game.utils.misc.DirectionPositionMapper
import com.megaman.maverick.game.utils.misc.LightSourceUtils
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getEntity
import com.megaman.maverick.game.world.body.getPositionPoint
import com.megaman.maverick.game.world.body.setFilter

class HeartTank(game: MegamanMaverickGame) : AbstractItem(game), ISpritesEntity, IAnimatedEntity, ILightSource {

    companion object {
        const val TAG = "HeartTank"

        private const val BODY_SIZE = 1f

        private const val SAND_GRAVITY = -0.1f
        private const val MIN_SAND_VEL_Y = -4f

        private const val LIGHT_RADIUS = 5
        private const val LIGHT_RADIANCE = 1.25f

        private var region: TextureRegion? = null
    }

    override val lightSourceKeys = ObjectSet<Int>()
    override val lightSourceCenter: Vector2
        get() = body.getCenter()
    override var lightSourceRadius = LIGHT_RADIUS
    override var lightSourceRadiance = LIGHT_RADIANCE

    lateinit var heartTank: MegaHeartTank
        private set

    private val spawnOnContact = Array<RectangleMapObject>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.ITEMS_1.source, TAG)
        super.init()
        addComponent(defineSpritesCompoent())
        addComponent(defineAnimationsComponent())
    }

    override fun canSpawn(spawnProps: Properties): Boolean {
        val value = spawnProps.get(ConstKeys.VALUE)!!

        this.heartTank = when (value) {
            is MegaHeartTank -> value
            is String -> MegaHeartTank.valueOf(value.uppercase())
            else -> throw IllegalArgumentException("Invalid value: $value")
        }

        val canSpawn = !megaman.hasHeartTank(heartTank)
        GameLogger.debug(TAG, "canSpawn(): canSpawn=$canSpawn")

        return canSpawn
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        if (!this::heartTank.isInitialized) throw IllegalStateException("Heart tank value is not initialized")

        body.setSize(BODY_SIZE * ConstVals.PPM)

        super.onSpawn(spawnProps)

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.SPAWN) && value is RectangleMapObject) spawnOnContact.add(value)
        }

        body.fixtures[FixtureType.FEET].first().setFilter filter@{ fixture ->
            val entity = fixture.getEntity()
            return@filter entity !is FeetRiseSinkBlock
        }

        LightSourceUtils.loadLightSourceKeysFromProps(this, spawnProps)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        lightSourceKeys.clear()
    }

    override fun whenFeetOnSand() {
        body.physics.gravity.y = SAND_GRAVITY * ConstVals.PPM

        if (body.physics.velocity.y < MIN_SAND_VEL_Y * ConstVals.PPM)
            body.physics.velocity.y = MIN_SAND_VEL_Y * ConstVals.PPM
    }

    override fun contactWithPlayer(megaman: Megaman) {
        GameLogger.debug(TAG, "contactWithPlayer(): megaman=$megaman")

        destroy()

        spawnOnContact.forEach {
            val props = it.properties.toProps()

            val entityType = EntityType.valueOf(props.get(ConstKeys.ENTITY_TYPE, String::class)!!.uppercase())
            val key = entityType.getFullyQualifiedName(it.name)

            val entity = MegaEntityFactory.fetch(key, MegaGameEntity::class)!!
            entity.spawn(props)
        }

        game.eventsMan.submitEvent(Event(EventType.ATTAIN_HEART_TANK, props(ConstKeys.VALUE pairTo heartTank)))
    }

    override fun defineUpdatablesComponent(component: UpdatablesComponent) {
        super.defineUpdatablesComponent(component)
        component.add {
            if (game.getCurrentLevel() == LevelDefinition.DESERT_MAN)
                LightSourceUtils.sendLightSourceEvent(game, this)
        }
    }

    private fun defineSpritesCompoent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(2f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { _, _ ->
            val position = DirectionPositionMapper.getInvertedPosition(direction)
            val bodyPosition = body.getPositionPoint(position)
            sprite.setPosition(bodyPosition, position)
            sprite.setOriginCenter()
            sprite.rotation = direction.rotation
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 1, 2, 0.15f, true)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    override fun getTag() = TAG
}
