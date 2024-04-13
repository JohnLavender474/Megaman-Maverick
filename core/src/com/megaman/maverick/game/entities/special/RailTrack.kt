package com.megaman.maverick.game.entities.special

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.engine.animations.AnimationsComponent
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.toGdxArray
import com.engine.common.extensions.toInt
import com.engine.common.extensions.toObjectSet
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setSize
import com.engine.entities.GameEntity
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IChildEntity
import com.engine.entities.contracts.ISpriteEntity
import com.engine.updatables.UpdatablesComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.blocks.Block

class RailTrack(game: MegamanMaverickGame) : GameEntity(game), ISpriteEntity, IAnimatedEntity {

    companion object {
        const val TAG = "RailTrack"
        private var leftTrackRegion: TextureRegion? = null
        private var rightTrackRegion: TextureRegion? = null
        private var middleTrackRegion: TextureRegion? = null
        private var dropTrackRegion: TextureRegion? = null
    }

    private lateinit var drops: Array<GameRectangle>

    private var platform: RailTrackPlatform? = null

    override fun init() {
        if (leftTrackRegion == null || rightTrackRegion == null || middleTrackRegion == null || dropTrackRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SPECIALS_1.source)
            leftTrackRegion = atlas.findRegion("RailTrack/Left")
            rightTrackRegion = atlas.findRegion("RailTrack/Right")
            middleTrackRegion = atlas.findRegion("RailTrack/Middle")
            dropTrackRegion = atlas.findRegion("RailTrack/Drop")
        }
        addComponent(defineUpdatablesComponent())
        addComponent(SpritesComponent(this))
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPosition()
        val width = spawnProps.get(ConstKeys.WIDTH, Int::class)!!
        val dropIndices = spawnProps.get(ConstKeys.ARRAY, String::class)!!.split(",").map { it.toInt() }.toObjectSet()
        drops = dropIndices.map {
            val drop = GameRectangle()
            drop.setSize(ConstVals.PPM.toFloat(), ConstVals.PPM.toFloat())
            drop.setPosition(spawn.x + it * ConstVals.PPM, spawn.y)
            drop
        }.toGdxArray()

        val leftSprite = GameSprite()
        leftSprite.setSize(ConstVals.PPM.toFloat())
        leftSprite.setPosition(spawn.x, spawn.y)
        leftSprite.setRegion(leftTrackRegion!!)
        sprites.put(ConstKeys.LEFT, leftSprite)

        for (i in 1 until width - 1) {
            val middleSprite = GameSprite()
            middleSprite.setSize(ConstVals.PPM.toFloat())
            middleSprite.setPosition(spawn.x + i * ConstVals.PPM, spawn.y)
            val region = if (dropIndices.contains(i)) dropTrackRegion else middleTrackRegion
            middleSprite.setRegion(region!!)
            sprites.put(ConstKeys.MIDDLE + i, middleSprite)
        }

        val rightSprite = GameSprite()
        rightSprite.setSize(ConstVals.PPM.toFloat())
        rightSprite.setPosition(spawn.x + (width - 1) * ConstVals.PPM, spawn.y)
        rightSprite.setRegion(rightTrackRegion!!)
        sprites.put(ConstKeys.RIGHT, rightSprite)
    }

    override fun onDestroy() {
        super<GameEntity>.onDestroy()
        sprites.clear()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent(this, {
        TODO()
    })

    private fun defineSpritesComponent(): SpritesComponent {
        TODO()
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        TODO()
    }
}

class RailTrackPlatform(game: MegamanMaverickGame) : Block(game), IChildEntity, ISpriteEntity, IAnimatedEntity {

    override var parent: IGameEntity? = null

    companion object {
        const val TAG = "RailTrackPlatform"
        private var platformRegion: TextureRegion? = null
        private var platformDropRegion: TextureRegion? = null
    }

    override fun init() {
        super<Block>.init()
        if (platformRegion == null || platformDropRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SPECIALS_1.source)
            platformRegion = atlas.findRegion("RailTrack/Platform")
            platformDropRegion = atlas.findRegion("RailTrack/PlatformDrop")
        }
    }

    override fun spawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.spawn(spawnProps)
        parent = spawnProps.get(ConstKeys.PARENT, IGameEntity::class)!!
    }

    internal fun drop() {
        body.physics.collisionOn = false
    }

    internal fun raise() {
        body.physics.collisionOn = true
    }

    private fun defineSpritesComponent(): SpritesComponent {
        TODO()
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        TODO()
    }
}