package com.megaman.maverick.game.entities.megamanx

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.extensions.gdxArrayOf
import com.maverick.game.engine.common.extensions.getTextureAtlas
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.damage.IDamagerEntity
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sorting.DrawingSection
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.splitAndFlatten
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.Body
import com.maverick.game.engine.world.body.BodyComponent
import com.maverick.game.engine.world.body.BodyType
import com.maverick.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegaServices
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.abs

// TODO: each target gives the slash one static damage for every slash kind; a more dynamic rule (by slash kind and
//  by target) is still to come (item 141)
class MegamanXSabreSlash(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity,
    IOwnable<MegamanX>, IDamagerEntity {

    companion object {
        const val TAG = "MegamanXSabreSlash"

        private const val ART_PX_PER_BODY_FRAME = 48

        internal val HIT_BOXES = ObjectMap<SabreKind, kotlin.Array<IntArray?>>().apply {
            put(
                SabreKind.FLOOR, arrayOf(
                    null,
                    intArrayOf(42, 21, 11, 12),
                    intArrayOf(37, 25, 23, 13),
                    intArrayOf(53, 10, 52, 34),
                    intArrayOf(64, 0, 43, 44),
                    intArrayOf(48, 4, 43, 37),
                    intArrayOf(46, 5, 42, 23),
                    intArrayOf(47, 5, 23, 13),
                    null
                )
            )
            put(
                SabreKind.AIR, arrayOf(
                    null,
                    null,
                    intArrayOf(43, 31, 17, 8),
                    intArrayOf(52, 21, 23, 19),
                    intArrayOf(43, 12, 43, 28),
                    intArrayOf(31, 0, 60, 36),
                    intArrayOf(30, 11, 12, 18),
                    null,
                    null
                )
            )
            put(
                SabreKind.WALL, arrayOf(
                    null,
                    intArrayOf(45, 28, 16, 10),
                    intArrayOf(8, 11, 35, 27),
                    intArrayOf(7, 10, 36, 24),
                    intArrayOf(6, 10, 37, 9),
                    intArrayOf(19, 11, 24, 7),
                    intArrayOf(35, 13, 8, 5),
                    intArrayOf(35, 13, 8, 5)
                )
            )
        }

        private val regions = ObjectMap<SabreKind, Array<TextureRegion>>()

        internal fun artPx() = MegamanXValues.SPRITE_SIZE * ConstVals.PPM / ART_PX_PER_BODY_FRAME
    }

    override var owner: MegamanX? = null

    lateinit var kind: SabreKind
        internal set

    private val hitFixture = Fixture(Body(BodyType.ABSTRACT), FixtureType.DAMAGER, GameRectangle())

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = services.assets.getTextureAtlas(TextureAsset.MEGAMAN_X.source)
            SabreKind.entries.forEach {
                regions.put(it, atlas.findRegion(it.arcRegion)!!.splitAndFlatten(it.stripFrames, 1, Array()))
            }
        }
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(UpdatablesComponent({ if (owner?.sabre?.slash !== this) destroy() }))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        owner = spawnProps.get(ConstKeys.OWNER, MegamanX::class)!!
        kind = spawnProps.get(ConstKeys.TYPE, SabreKind::class)!!
        placeHitBox()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        owner = null
    }

    private fun current(): MegamanX? = owner?.takeIf { it.sabre.slash === this && it.sabre.kind == kind }

    private fun flipped(x: MegamanX) =
        if (kind.stripFacesLeft) x.facing == Facing.RIGHT else x.facing == Facing.LEFT

    internal fun center(x: MegamanX, out: Vector2): Vector2 {
        val artPx = artPx()
        val up = MegamanXValues.SPRITE_SIZE * ConstVals.PPM / 2f - x.body.getHeight() / 2f +
            kind.arcCenterAboveBody * artPx
        return out.set(0f, up).rotateDeg(x.direction.rotation).add(x.body.getCenter())
    }

    private fun placeHitBox() {
        val box = hitFixture.rawShape as GameRectangle
        val x = current()
        val rect = x?.let { HIT_BOXES[kind][it.sabre.frame] }
        if (x == null || rect == null) {
            hitFixture.setActive(false)
            return
        }
        val artPx = artPx()
        val left = if (flipped(x)) kind.arcWidth - rect[0] - rect[2] else rect[0]
        val localX = (left - kind.arcWidth / 2f) * artPx
        val localY = (rect[1] - kind.arcHeight / 2f) * artPx
        val center = center(x, GameObjectPools.fetch(Vector2::class))
        val a = GameObjectPools.fetch(Vector2::class).set(localX, localY).rotateDeg(x.direction.rotation)
        val b = GameObjectPools.fetch(Vector2::class)
            .set(localX + rect[2] * artPx, localY + rect[3] * artPx).rotateDeg(x.direction.rotation)
        hitFixture.body.set(
            center.x + minOf(a.x, b.x), center.y + minOf(a.y, b.y),
            abs(b.x - a.x), abs(b.y - a.y)
        )
        box.set(hitFixture.body)
        hitFixture.setActive(true)
    }

    private fun defineBodyComponent(): BodyComponent {
        val body = hitFixture.body
        body.addFixture(hitFixture)
        body.preProcess.put(ConstKeys.DEFAULT) { placeHitBox() }
        addComponent(DrawableShapesComponent(debugShapeSuppliers = gdxArrayOf({ hitFixture }), debug = true))
        return BodyComponentCreator.create(this, body, doUpdate = { true })
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 2))
        val component = SpritesComponent(sprite)
        component.drawOffsetBody = { current()?.body }
        component.putPreProcess { _, _ ->
            val x = current()
            if (x == null) {
                sprite.hidden = true
                return@putPreProcess
            }
            val artPx = artPx()
            sprite.setRegion(regions[kind][x.sabre.frame])
            sprite.setSize(kind.arcWidth * artPx, kind.arcHeight * artPx)
            sprite.setOriginCenter()
            sprite.rotation = x.direction.rotation
            sprite.setFlip(flipped(x), false)
            val center = center(x, GameObjectPools.fetch(Vector2::class))
            sprite.setCenter(center.x, center.y)
            sprite.hidden = false
        }
        return component
    }

    override fun getType() = EntityType.OTHER
}
