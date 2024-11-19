package com.megaman.maverick.game.entities.megaman.extensions


import com.badlogic.gdx.utils.ObjectSet
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.megaman.Megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanKeys

fun Megaman.getFeetBlocks(): ObjectSet<Block> {
    if (!hasProperty(MegamanKeys.FEET_BLOCKS)) putProperty(MegamanKeys.FEET_BLOCKS, ObjectSet<Block>())
    return getProperty(MegamanKeys.FEET_BLOCKS) as ObjectSet<Block>
}

fun Megaman.clearFeetBlocks() = getFeetBlocks().clear()

fun Megaman.addFeetBlock(block: Block) = getFeetBlocks().add(block)

fun Megaman.removeFeetBlock(block: Block) = getFeetBlocks().remove(block)

fun Megaman.hasFeetBlock(block: Block) = getFeetBlocks().contains(block)

fun Megaman.getFeetBlocksCount() = getFeetBlocks().size

fun Megaman.hasAnyFeetBlock() = getFeetBlocksCount() > 0
