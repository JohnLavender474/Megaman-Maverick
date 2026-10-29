package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.OrderedSet
import com.maverick.game.engine.animations.Animation
import com.maverick.game.engine.animations.AnimationsComponent
import com.maverick.game.engine.animations.Animator
import com.maverick.game.engine.animations.IAnimation
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.ProcessState
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.extensions.objectMapOf
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.contracts.IAnimatedEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.decorations.BlockPiece
import com.megaman.maverick.game.entities.decorations.BlockPiece.BlockPieceColor
import com.megaman.maverick.game.entities.enemies.Wanaan
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.projectiles.MoonScythe
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter
import com.megaman.maverick.game.world.body.getEntity

open class BreakableBlock(game: MegaServices) : Block(game), ISpritesEntity, IAnimatedEntity {

    companion object {

        const val TAG = "BreakableBlock"

        const val BRICK_TYPE = "Brick"
        const val PRECIOUS_TYPE = "PinkBlock"
        const val SMB3_GOLD_TYPE = "SMB3_Gold"

        fun breakApart(center: Vector2, color: BlockPieceColor) {
            for (i in 0 until BRICK_PIECE_IMPULSES.size) {
                val impulse = BRICK_PIECE_IMPULSES[i].cpy().scl(ConstVals.PPM.toFloat())

                val piece = MegaEntityFactory.fetch(BlockPiece::class)!!
                piece.spawn(
                    props(
                        ConstKeys.COLOR pairTo color,
                        ConstKeys.POSITION pairTo center,
                        ConstKeys.IMPULSE pairTo impulse,
                        ConstKeys.THUMP pairTo BRICK_TYPE_THUMP
                    )
                )
            }
        }

        // set this to true to make brick pieces "thump" on blocks, else false
        private const val BRICK_TYPE_THUMP = false

        private val BRICK_PIECE_IMPULSES =
            gdxArrayOf(Vector2(-5f, 3f), Vector2(-3f, 5f), Vector2(3f, 5f), Vector2(5f, 3f))

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private lateinit var type: String
    private lateinit var color: BlockPieceColor

    private val connectedBlockIds = OrderedSet<Int>()
    private val connectedBlocks = OrderedSet<BreakableBlock>()

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val platformsAtlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            regions.put(BRICK_TYPE, platformsAtlas.findRegion(BRICK_TYPE))
            regions.put(PRECIOUS_TYPE, platformsAtlas.findRegion(PRECIOUS_TYPE))

            val smb3BlocksAtlas = game.assMan.getTextureAtlas(TextureAsset.SMB3_BLOCKS.source)
            regions.put(SMB3_GOLD_TYPE, smb3BlocksAtlas.findRegion("GoldBreakableBlock/block"))
        }
        super.init()
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        type = spawnProps.get(ConstKeys.TYPE, String::class)!!

        color = BlockPieceColor.valueOf(
            spawnProps.getOrDefault(
                ConstKeys.COLOR,
                when (type) {
                    SMB3_GOLD_TYPE -> BlockPieceColor.GOLD.name
                    else -> BlockPieceColor.RED.name
                },
                String::class
            ).uppercase()
        )

        spawnProps.forEach { key, value ->
            if (key.toString().contains(ConstKeys.CONNECTED))
                connectedBlockIds.add((value as RectangleMapObject).properties.get(ConstKeys.ID, Int::class.java))
        }
    }

    fun explodeAndDie() {
        GameLogger.debug(TAG, "explodeAndDie()")
        destroy()
        breakApart(body.getCenter(), color)
        connectedBlockIds.forEach { id ->
            val set = MegaGameEntities.getOfId(id)
            if (set.isEmpty) return@forEach
            set.forEach { block ->
                block as BreakableBlock
                if (!block.existenceState.liveAfterUpdate) return@forEach
                connectedBlocks.add(block)
            }
        }
        connectedBlockIds.clear()
        connectedBlocks.forEach { block -> block.explodeAndDie() }
        connectedBlocks.clear()

        if (game.audioMan.isSoundPlaying(SoundAsset.THUMP_SOUND)) game.audioMan.stopSound(SoundAsset.THUMP_SOUND)
        game.audioMan.playSound(SoundAsset.THUMP_SOUND, false)
    }

    override fun hitByProjectile(projectileFixture: IFixture) {
        val entity = projectileFixture.getEntity()
        if (entity is MoonScythe && entity.owner.asPlayer != null) explodeAndDie()
    }

    override fun hitByHead(processState: ProcessState, headFixture: IFixture) {
        if (processState != ProcessState.BEGIN) return

        val entity = headFixture.getEntity()
        when (type) {
            BRICK_TYPE -> if (entity is Wanaan) explodeAndDie()
            SMB3_GOLD_TYPE -> if (entity.asPlayer != null) explodeAndDie()
            else -> {}
        }
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val bounds = body.getBounds()
            sprite.setBounds(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight())
        }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: (String?) -> String? = { type }
        val animations = objectMapOf<String, IAnimation>(
            BRICK_TYPE pairTo Animation(regions[BRICK_TYPE]),
            PRECIOUS_TYPE pairTo Animation(regions[PRECIOUS_TYPE]),
            SMB3_GOLD_TYPE pairTo Animation(regions[SMB3_GOLD_TYPE])
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}
