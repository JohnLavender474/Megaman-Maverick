package com.megaman.maverick.game.entities.players

import com.badlogic.gdx.graphics.g2d.TextureAtlas
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
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.players.MegaPlayer
import com.megaman.maverick.game.players.PlayerSabre
import com.megaman.maverick.game.players.SabreMove
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getCenter
import kotlin.math.abs

// TODO: each target gives the slash one static damage per move strength (full or half); a more dynamic rule (by move
//  and by target) is still to come (item 141)
class PlayerSabreSlash(services: MegaServices) : MegaGameEntity(services), IBodyEntity, ISpritesEntity,
    IOwnable<MegaPlayer>, IDamagerEntity {

    companion object {
        const val TAG = "PlayerSabreSlash"

        private val regions = ObjectMap<SabreMove, Array<TextureRegion>>()
    }

    override var owner: MegaPlayer? = null

    var sabre: PlayerSabre? = null
        private set
    var fullStrength = true
        private set

    private val hitFixture = Fixture(Body(BodyType.ABSTRACT), FixtureType.DAMAGER, GameRectangle())

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(UpdatablesComponent({ if (sabre?.slash !== this) destroy() }))
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        owner = spawnProps.get(ConstKeys.OWNER, MegaPlayer::class)!!
        val sabre = spawnProps.get(ConstKeys.PARENT, PlayerSabre::class)!!
        this.sabre = sabre
        loadRegions(sabre)
        placeHitBox()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        owner = null
        sabre = null
    }

    private fun loadRegions(sabre: PlayerSabre) {
        val atlas = services.assets.getTextureAtlas(sabre.params.atlas.source)
        for (moves in sabre.params.moves.values()) for (move in moves) loadRegion(atlas, move)
        for (move in sabre.params.landingMoves.values()) loadRegion(atlas, move)
    }

    private fun loadRegion(atlas: TextureAtlas, move: SabreMove) {
        if (!regions.containsKey(move))
            regions.put(move, atlas.findRegion(move.arcRegion)!!.splitAndFlatten(move.stripFrames, 1, Array()))
    }

    private fun currentMove(): SabreMove? = sabre?.takeIf { it.slash === this }?.move

    private fun flipped(player: MegaPlayer, move: SabreMove) =
        if (move.stripFacesLeft) player.facing == Facing.RIGHT else player.facing == Facing.LEFT

    internal fun center(player: MegaPlayer, sabre: PlayerSabre, move: SabreMove, out: Vector2): Vector2 {
        val up = sabre.params.spriteSize * ConstVals.PPM / 2f - player.body.getHeight() / 2f +
            move.arcCenterAboveBody * sabre.params.artPx + sabre.params.spriteOffsetY()
        return out.set(0f, up).rotateDeg(player.direction.rotation).add(player.body.getCenter())
    }

    private fun placeHitBox() {
        val box = hitFixture.rawShape as GameRectangle
        val player = owner
        val sabre = sabre
        val move = currentMove()
        val rect = if (sabre != null && move != null) move.hitBoxes[sabre.frame] else null
        if (player == null || sabre == null || move == null || rect == null) {
            hitFixture.setActive(false)
            return
        }
        val artPx = sabre.params.artPx
        val left = if (flipped(player, move)) move.arcWidth - rect[0] - rect[2] else rect[0]
        val localX = (left - move.arcWidth / 2f) * artPx
        val localY = (rect[1] - move.arcHeight / 2f) * artPx
        val center = center(player, sabre, move, GameObjectPools.fetch(Vector2::class))
        val a = GameObjectPools.fetch(Vector2::class).set(localX, localY).rotateDeg(player.direction.rotation)
        val b = GameObjectPools.fetch(Vector2::class)
            .set(localX + rect[2] * artPx, localY + rect[3] * artPx).rotateDeg(player.direction.rotation)
        hitFixture.body.set(
            center.x + minOf(a.x, b.x), center.y + minOf(a.y, b.y),
            abs(b.x - a.x), abs(b.y - a.y)
        )
        box.set(hitFixture.body)
        fullStrength = move.fullStrength
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
        component.drawOffsetBody = { currentMove()?.let { owner?.body } }
        component.putPreProcess { _, _ ->
            val player = owner
            val sabre = sabre
            val move = currentMove()
            if (player == null || sabre == null || move == null) {
                sprite.hidden = true
                return@putPreProcess
            }
            val artPx = sabre.params.artPx
            sprite.setRegion(regions[move][sabre.frame])
            sprite.setSize(move.arcWidth * artPx, move.arcHeight * artPx)
            sprite.setOriginCenter()
            sprite.rotation = player.direction.rotation
            sprite.setFlip(flipped(player, move), false)
            val center = center(player, sabre, move, GameObjectPools.fetch(Vector2::class))
            sprite.setCenter(center.x, center.y)
            sprite.hidden = false
        }
        return component
    }

    override fun getType() = EntityType.OTHER
}
