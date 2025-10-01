package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods.getOverlapPushDirection
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.isAny
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.projectiles.PreciousShard.PreciousShardColor
import com.megaman.maverick.game.entities.projectiles.PreciousShard.PreciousShardSize
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

class PreciousGemBomb(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        const val TAG = "PreciousGemBomb"

        private val SHATTER_IMPULSES = objectMapOf(
            Direction.UP pairTo gdxArrayOf(
                Vector2(5f, 9f),
                Vector2(0f, 9f),
                Vector2(-5f, 9f)
            ),
            Direction.DOWN pairTo gdxArrayOf(
                Vector2(5f, -9f),
                Vector2(0f, -9f),
                Vector2(-5f, -9f)
            ),
            Direction.LEFT pairTo gdxArrayOf(
                Vector2(-9f, 5f),
                Vector2(-9f, 0f),
                Vector2(-9f, -5f)
            ),
            Direction.RIGHT pairTo gdxArrayOf(
                Vector2(9f, 5f),
                Vector2(9f, 0f),
                Vector2(9f, -5f)
            )
        )

        private const val GRAVITY = -0.15f

        private const val CULL_TIME = 0.5f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    enum class PreciousGemBombColor {
        GREEN, BLUE, PINK
    }

    private lateinit var color: PreciousGemBombColor

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PROJECTILES_1.source)
            val keys = PreciousGemBombColor.entries.map { it.name.lowercase() }
            AnimationUtils.loadRegions(TAG, atlas, keys, regions)
        }
        super.init()
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_TIME, CULL_TIME)
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        val trajectory = spawnProps.get(ConstKeys.TRAJECTORY, Vector2::class)!!
        body.physics.velocity.set(trajectory)

        color = spawnProps.get(ConstKeys.COLOR, PreciousGemBombColor::class)!!
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    override fun hitShield(shieldFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val shieldEntity = shieldFixture.getEntity()
        if (shieldEntity.isAny(PreciousGemBomb::class, PreciousShard::class)) return

        val direction = getOverlapPushDirection(body.getBounds(), otherShape) ?: Direction.UP
        explodeAndDie(direction)
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val direction = getOverlapPushDirection(body.getBounds(), otherShape) ?: Direction.UP
        explodeAndDie(direction)
    }

    override fun hitProjectile(projectileFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        val projectile = projectileFixture.getEntity()
        if (projectile is SlashWave) {
            val direction = if (otherShape.getCenter().x < thisShape.getCenter().x) Direction.RIGHT else Direction.LEFT
            explodeAndDie(direction)
        }
    }

    override fun explodeAndDie(vararg params: Any?) {
        val direction = params[0] as Direction
        SHATTER_IMPULSES.get(direction).forEach { impulse ->
            val shard = MegaEntityFactory.fetch(PreciousShard::class)!!
            shard.spawn(
                props(
                    ConstKeys.OWNER pairTo owner,
                    ConstKeys.POSITION pairTo when (direction) {
                        Direction.UP -> body.getPositionPoint(Position.TOP_CENTER)
                        Direction.DOWN -> body.getPositionPoint(Position.BOTTOM_CENTER)
                        Direction.LEFT -> body.getPositionPoint(Position.CENTER_LEFT)
                        Direction.RIGHT -> body.getPositionPoint(Position.CENTER_RIGHT)
                    },
                    ConstKeys.IMPULSE pairTo GameObjectPools.fetch(Vector2::class)
                        .set(impulse)
                        .scl(ConstVals.PPM.toFloat()),
                    ConstKeys.COLOR pairTo when (color) {
                        PreciousGemBombColor.GREEN -> PreciousShardColor.GREEN
                        PreciousGemBombColor.BLUE -> PreciousShardColor.BLUE
                        PreciousGemBombColor.PINK -> PreciousShardColor.PINK
                    },
                    "${ConstKeys.COLLIDE}_${ConstKeys.DELAY}" pairTo false,
                    ConstKeys.SIZE pairTo PreciousShardSize.SMALL
                )
            )
        }

        destroy()

        requestToPlaySound(SoundAsset.DINK_SOUND, false)
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.physics.gravity.y = GRAVITY * ConstVals.PPM

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val leftFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        debugShapes.add { leftFixture }

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.SHIELD, FixtureType.PROJECTILE, FixtureType.DAMAGER)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { it.setSize(ConstVals.PPM.toFloat()) })
        .updatable { _, sprite ->
            val region = regions[color.name.lowercase()]
            sprite.setRegion(region)

            sprite.setCenter(body.getCenter())
        }
        .build()
}
