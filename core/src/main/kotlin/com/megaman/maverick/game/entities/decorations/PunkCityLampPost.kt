package com.megaman.maverick.game.entities.decorations

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.audio.AudioComponent
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setBounds
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IAudioEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.explosions.StarExplosion
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.BodyLabel
import com.megaman.maverick.game.world.body.FixtureLabel
import com.megaman.maverick.game.world.body.addFixtureLabel
import com.megaman.maverick.game.world.body.getCenter

class PunkCityLampPost(game: MegamanMaverickGame) : MegaGameEntity(game), ISpritesEntity, IAnimatedEntity, IAudioEntity,
    IFaceable {

    companion object {
        const val TAG = "PunkCityLampPost"

        private const val WIDTH = 2f
        private const val HEIGHT = 4.6875f

        private val animDefs = orderedMapOf(
            LampPostState.ALIGHT pairTo AnimationDef(1, 3, 0.1f, true),
            LampPostState.BROKEN pairTo AnimationDef()
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    internal enum class LampPostState { ALIGHT, BROKEN }

    override lateinit var facing: Facing

    internal lateinit var state: LampPostState
        private set
    internal val bounds = GameRectangle().setSize(WIDTH * ConstVals.PPM, HEIGHT * ConstVals.PPM)

    private var block: Block? = null

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.DECORATIONS_1.source)
            LampPostState.entries.forEach { state ->
                val key = state.name.lowercase()
                regions.put(key, atlas.findRegion("$TAG/$key"))
            }
        }
        super.init()
        addComponent(AudioComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        facing = Facing.valueOf(spawnProps.getOrDefault(ConstKeys.FACING, ConstKeys.RIGHT, String::class).uppercase())

        val position = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(
            if (isFacing(Facing.RIGHT)) Position.BOTTOM_LEFT else Position.BOTTOM_RIGHT
        )
        bounds.setBottomCenterToPoint(position)

        val block = MegaEntityFactory.fetch(PunkCityLampPostBlock::class)!!
        block.spawn(props(ConstKeys.OWNER pairTo this))
        this.block = block

        state = LampPostState.ALIGHT
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()

        block?.destroy()
        block = null
    }

    internal fun shatter() {
        if (state == LampPostState.BROKEN) {
            GameLogger.debug(TAG, "shatter(): already shattered, do nothing")
            return
        }

        GameLogger.debug(TAG, "shatter(): do shatter")

        state = LampPostState.BROKEN

        requestToPlaySound(SoundAsset.DINK_SOUND, false)

        val explosion = MegaEntityFactory.fetch(StarExplosion::class)!!
        explosion.spawn(props(ConstKeys.POSITION pairTo block!!.body.getCenter(), ConstKeys.ACTIVE pairTo false))
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2f * ConstVals.PPM, 4.6875f * ConstVals.PPM) })
        .updatable { _, sprite ->
            sprite.setBounds(bounds)
            sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { state.name.lowercase() }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key.name.lowercase()
                        val (rows, columns, durations, loop) = entry.value
                        animations.put(key, Animation(regions[key], rows, columns, durations, loop))
                    }
                }
                .build()
        )
        .build()

    override fun getType() = EntityType.DECORATION

    override fun getTag() = TAG
}

class PunkCityLampPostBlock(game: MegamanMaverickGame) : Block(game), IOwnable<PunkCityLampPost> {

    companion object {
        const val TAG = "PunkCityLampPostBlock"

        private const val WIDTH = 1.25f
        private const val HEIGHT = 0.5f
    }

    override var owner: PunkCityLampPost? = null

    override fun onSpawn(spawnProps: Properties) {
        owner = spawnProps.get(ConstKeys.OWNER, PunkCityLampPost::class)!!

        val bounds = GameObjectPools.fetch(GameRectangle::class)
            .setSize(WIDTH * ConstVals.PPM, HEIGHT * ConstVals.PPM)
        val position = if (owner!!.isFacing(Facing.LEFT)) Position.TOP_LEFT else Position.TOP_RIGHT
        bounds.positionOnPoint(owner!!.bounds.getPositionPoint(position), position)

        spawnProps.putAll(
            ConstKeys.BOUNDS pairTo bounds,
            ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
            ConstKeys.FIXTURE_LABELS pairTo objectSetOf(FixtureLabel.NO_SIDE_TOUCHIE),
            ConstKeys.BODY_LABELS pairTo objectSetOf(BodyLabel.COLLIDE_DOWN_ONLY, BodyLabel.PRESS_UP_FALL_THRU)
        )

        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        owner = null
    }

    override fun hitByFeet(processState: ProcessState, feetFixture: IFixture) {
        GameLogger.debug(TAG, "hitByFeet(): processState=$processState, feetFixture=$feetFixture")
        shatter()
    }

    override fun hitByProjectile(projectileFixture: IFixture) {
        GameLogger.debug(TAG, "hitByProjectile(): projectileFixture=$projectileFixture")
        shatter()
    }

    private fun shatter() {
        GameLogger.debug(TAG, "shatter()")
        owner!!.shatter()
        blockFixture.addFixtureLabel(FixtureLabel.NO_PROJECTILE_COLLISION)
    }
}
