package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.map
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IActivatable
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.IntPair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity

class ElecDevilBodyPieces(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity,
    IOwnable<ElecDevilBody>, IFaceable, IActivatable {

    companion object {
        const val TAG = "ElecDevilBodyPieces"
        private val colors = gdxArrayOf(ConstKeys.WHITE, ConstKeys.BLUE, ConstKeys.GREEN)
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var owner: ElecDevilBody? = null
    override lateinit var facing: Facing
    override var on = false

    private val pieces = ObjectMap<IntPair, Boolean>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_3.source)
            ElecDevilConstants.forEachCell { row, column ->
                val rowColumnKey = ElecDevilConstants.getRowColumnKey(row, column)
                colors.forEach { color ->
                    val piece = atlas.findRegion("${ElecDevilBody.TAG}/${ConstKeys.PIECES}/$color/$rowColumnKey")
                    regions.put("${rowColumnKey}/$color", piece)
                }
            }
        }
        super.init()
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        setStateOfAllPieces(false)
        facing = spawnProps.get(ConstKeys.FACING, Facing::class)!!
        owner = spawnProps.get(ConstKeys.OWNER, ElecDevilBody::class)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    internal fun setStateOfPiece(row: Int, column: Int, state: Boolean) = pieces.put(row pairTo column, state)

    internal fun setStateOfAllPieces(state: Boolean) = ElecDevilConstants.CELLS.forEach { entry ->
        ElecDevilConstants.forEachCell { row, column -> setStateOfPiece(row, column, state) }
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .also { builder ->
            ElecDevilConstants.forEachCell { row, column ->
                val key = ElecDevilConstants.getRowColumnKey(row, column)

                val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 3))
                builder.sprite(key, sprite)

                builder.updatable { _, sprite ->
                    val width = owner!!.body.getWidth() / ElecDevilConstants.PIECE_COLUMNS
                    val height = owner!!.body.getHeight() / ElecDevilConstants.PIECE_ROWS
                    sprite.setSize(width, height)

                    val position = owner!!.getPositionOf(row, column)
                    sprite.setPosition(position)

                    sprite.setFlip(isFacing(Facing.LEFT), false)

                    sprite.hidden = !on || !pieces.containsKey(row pairTo column) || !pieces[row pairTo column]
                }
            }
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .also { builder ->
            ElecDevilConstants.forEachCell { row, column ->
                val rowColumnKey = ElecDevilConstants.getRowColumnKey(row, column)
                val regions = colors.map { color -> regions["${rowColumnKey}/$color"] }.toGdxArray()
                builder.key(rowColumnKey).animator(Animator(Animation(regions, 0.1f, true)))
            }
        }
        .build()

    override fun getType() = EntityType.OTHER

    override fun getTag() = TAG
}
