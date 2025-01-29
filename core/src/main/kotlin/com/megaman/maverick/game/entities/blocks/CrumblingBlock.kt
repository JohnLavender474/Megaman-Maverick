package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setBounds
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.events.Event
import com.mega.game.engine.events.EventsManager
import com.mega.game.engine.events.IEventListener
import com.mega.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.decorations.BlockPiece
import com.megaman.maverick.game.entities.decorations.BlockPiece.BlockPieceColor
import com.megaman.maverick.game.entities.enemies.TorikoPlundge
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.explosions.SpreadExplosion
import com.megaman.maverick.game.entities.projectiles.SmallGreenMissile
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.setHitByExplosionReceiver
import com.megaman.maverick.game.world.body.setHitByProjectileReceiver

class CrumblingBlock(game: MegamanMaverickGame) : Block(game), ISpritesEntity {

    companion object {
        const val TAG = "CrumblingBlock"
        private val PIECE_IMPULSES = gdxArrayOf(Vector2(-5f, 3f), Vector2(-3f, 5f), Vector2(3f, 5f), Vector2(5f, 3f))
        private var region: TextureRegion? = null
    }

    private val blocks = Matrix<GameRectangle>()

    override fun init() {
        GameLogger.debug(TAG, "init()")

        if (!CrumblingBlockEventListener.isEventsManSet()) {
            GameLogger.debug(TAG, "init(): set events man for event listener")

            CrumblingBlockEventListener.setEventsMan(game.eventsMan)
        }

        if (!game.eventsMan.isListener(CrumblingBlockEventListener)) {
            GameLogger.debug(TAG, "init(): add event listener to events man")

            game.eventsMan.addListener(CrumblingBlockEventListener)
        }

        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, TAG)

        super.init()

        addComponent(SpritesComponent())
    }

    override fun canSpawn(spawnProps: Properties): Boolean {
        val mapObjId = spawnProps.get(ConstKeys.ID, Int::class)!!
        val canSpawn = CrumblingBlockEventListener.get(mapObjId)

        GameLogger.debug(TAG, "canSpawn(): canSpawn=$canSpawn, mapObjId=$mapObjId, spawnProps=$spawnProps")

        return canSpawn
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        CrumblingBlockEventListener.put(mapObjectId, true)

        defineDrawables()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")

        super.onDestroy()

        sprites.clear()
    }

    override fun defineBodyComponent(): BodyComponent {
        val component = super.defineBodyComponent()

        val body = component.body

        val blockFixture = body.fixtures.get(FixtureType.BLOCK).first()
        blockFixture.setHitByProjectileReceiver { projectile -> hitByProjectile(projectile) }
        blockFixture.setHitByExplosionReceiver { explosion -> hitByExplosion(explosion) }

        return component
    }

    private fun hitByProjectile(projectile: IProjectileEntity) {
        if (projectile is SmallGreenMissile) crumble()
    }

    private fun hitByExplosion(explosion: IBodyEntity) {
        if (explosion is SpreadExplosion || (explosion is Explosion && explosion.owner is TorikoPlundge)) crumble()
    }

    private fun crumble() {
        GameLogger.debug(TAG, "crumble()")

        destroy()

        CrumblingBlockEventListener.put(mapObjectId, false)

        blocks.forEach { _, _, bounds ->
            for (i in 0 until PIECE_IMPULSES.size) {
                val spawn = bounds!!.getCenter()

                val impulse = PIECE_IMPULSES[i].cpy().scl(ConstVals.PPM.toFloat())

                val piece = MegaEntityFactory.fetch(BlockPiece::class)!!
                piece.spawn(
                    props(
                        ConstKeys.POSITION pairTo spawn,
                        ConstKeys.IMPULSE pairTo impulse,
                        ConstKeys.COLOR pairTo BlockPieceColor.GOLD
                    )
                )
            }
        }

        game.audioMan.playSound(SoundAsset.THUMP_SOUND, false)
    }

    private fun defineDrawables() {
        val cells = body.getBounds().splitByCellSize(ConstVals.PPM.toFloat(), blocks)

        cells.forEach { x, y, bounds ->
            val key = "${x}_${y}"

            val sprite = GameSprite(region!!)
            sprite.setBounds(bounds!!)

            putSprite(key, sprite)
        }
    }
}

object CrumblingBlockEventListener : IEventListener {

    const val TAG = "CrumblingBlockEventListener"

    override val eventKeyMask = objectSetOf<Any>(EventType.PLAYER_SPAWN, EventType.END_LEVEL)

    private var eventsMan: EventsManager? = null
    private val data = ObjectMap<Int, Boolean>()

    fun isEventsManSet() = eventsMan != null

    fun setEventsMan(eventsMan: EventsManager) {
        GameLogger.debug(TAG, "setEventsMan()")

        this.eventsMan = eventsMan
    }

    override fun onEvent(event: Event) {
        GameLogger.debug(TAG, "onEvent(): event=$event")

        when (event.key) {
            EventType.PLAYER_SPAWN -> {
                data.clear()

                GameLogger.debug(TAG, "onEvent(): cleared data")
            }
            EventType.END_LEVEL -> {
                eventsMan!!.removeListener(this)

                GameLogger.debug(TAG, "onEvent(): removed this from events man")
            }
        }
    }

    fun get(key: Int) = data[key] ?: true

    fun put(key: Int, value: Boolean) {
        GameLogger.debug(TAG, "put(): key=$key, value=$value")

        data.put(key, value)
    }
}
