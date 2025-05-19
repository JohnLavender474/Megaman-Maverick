package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.IEventListener
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.FixtureLabel
import com.megaman.maverick.game.world.body.FixtureType

class SteamRoller(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IFaceable, IEventListener {

    companion object {
        const val TAG = "SteamRoller"
        private var region: TextureRegion? = null
    }

    override val eventKeyMask = objectSetOf<Any>(EventType.BEGIN_ROOM_TRANS)
    override lateinit var facing: Facing

    private val position = Vector2()
    private val blocks = OrderedSet<Block>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, TAG)
        super.init()
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        game.eventsMan.addListener(this)

        val blockBounds = spawnProps.get(ConstKeys.BLOCKS) as Iterable<GameRectangle>
        blockBounds.forEach { blockBound ->
            val block = MegaEntityFactory.fetch(Block::class)!!
            block.spawn(
                props(
                    ConstKeys.OWNER pairTo this,
                    ConstKeys.BOUNDS pairTo blockBound,
                    ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
                    "${ConstKeys.FEET}_${ConstKeys.SOUND}" pairTo false,
                    ConstKeys.BODY_LABELS pairTo objectSetOf(
                        BodyLabel.COLLIDE_DOWN_ONLY
                    ),
                    ConstKeys.FIXTURE_LABELS pairTo objectSetOf(
                        FixtureLabel.NO_SIDE_TOUCHIE,
                        FixtureLabel.NO_BODY_TOUCHIE,
                        FixtureLabel.NO_PROJECTILE_COLLISION
                    ),
                    ConstKeys.FIXTURES pairTo gdxArrayOf(
                        FixtureType.SHIELD pairTo props(ConstKeys.DIRECTION pairTo Direction.UP)
                    )
                )
            )
        }

        position.set(spawnProps.get(ConstKeys.POSITION, Vector2::class)!!)

        facing = spawnProps.get(ConstKeys.FACING, Facing::class)!!
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        game.eventsMan.removeListener(this)

        blocks.forEach { it.destroy() }
        blocks.clear()
    }

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "onEvent(): event=$event")
        if (event.key == EventType.BEGIN_ROOM_TRANS) destroy()
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite(region!!).also { sprite -> sprite.setSize(7f * ConstVals.PPM, 6f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.setFlip(isFacing(Facing.LEFT), false)
            sprite.setPosition(position, Position.BOTTOM_CENTER)
        }
        .build()

    override fun getType() = EntityType.OTHER

    override fun getTag() = TAG
}
