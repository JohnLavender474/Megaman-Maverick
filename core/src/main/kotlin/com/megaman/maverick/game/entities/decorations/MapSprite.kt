package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TEXTURE_ASSET_PREFIX
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.state.mapObjectId

// TODO: a map sprite appears 1 frame after its rect enters the view, because the spawner tests the camera of the
//  frame before (MegaLevelScreen: spawns update before the camera moves); see the 138a docs/TODO.md backlog candidate
class MapSprite(services: MegaServices) : MegaGameEntity(services), ISpritesEntity {

    companion object {
        const val TAG = "MapSprite"
    }

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(SpritesComponent(GameSprite(DrawingPriority(DrawingSection.BACKGROUND, 1))))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        val bounds = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
        defaultSprite.setRegion(
            findRegion(
                atlas = spawnProps.get(ConstKeys.ATLAS, String::class),
                region = spawnProps.get(ConstKeys.REGION, String::class)
            )
        )
        defaultSprite.setBounds(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight())
    }

    private fun findRegion(atlas: String?, region: String?): TextureRegion {
        val where = "$TAG (map object id=$mapObjectId, ${ConstKeys.ATLAS}=$atlas, ${ConstKeys.REGION}=$region)"
        if (atlas == null) throw IllegalArgumentException(
            "$where has no ${ConstKeys.ATLAS} property: set it to an atlas file name, for example Enemies2.txt"
        )
        if (region == null) throw IllegalArgumentException(
            "$where has no ${ConstKeys.REGION} property: set it to a region name in $atlas, for example Wreck/wreck_a"
        )
        val source = TEXTURE_ASSET_PREFIX + atlas
        if (!services.assets.isLoaded(source, TextureAtlas::class.java)) throw IllegalArgumentException(
            "$where: no atlas $source is loaded: use the file name of a TextureAsset"
        )
        return services.assets.getTextureAtlas(source).findRegion(region)
            ?: throw IllegalArgumentException("$where: region $region is not in atlas $atlas")
    }

    override fun getType() = EntityType.DECORATION
}
