package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.extensions.gdxArrayOf
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
import com.mega.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.decorations.BlockPiece
import com.megaman.maverick.game.entities.decorations.BlockPiece.BlockPieceColor
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.explosions.SpreadExplosion
import com.megaman.maverick.game.entities.enemies.TorikoPlundge
import com.megaman.maverick.game.entities.projectiles.SmallGreenMissile
import com.megaman.maverick.game.world.body.*

class CrumblingGoldBlock(game: MegamanMaverickGame) : Block(game), ISpritesEntity {

    companion object {
        const val TAG = "CrumblingGoldBlock"
        private val PIECE_IMPULSES = gdxArrayOf(Vector2(-5f, 3f), Vector2(-3f, 5f), Vector2(3f, 5f), Vector2(5f, 3f))
        private var region: TextureRegion? = null
    }

    private val out = Matrix<GameRectangle>()

    override fun init() {
        super.init()
        addComponent(SpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        defineDrawables()
    }

    override fun onDestroy() {
        super.onDestroy()
        sprites.clear()
    }

    override fun defineBodyComponent(): BodyComponent {
        val component = super.defineBodyComponent()

        val body = component.body

        val bodyFixture = body.fixtures.get(FixtureType.BODY).first()
        bodyFixture.setHitByProjectileReceiver { projectile -> hitByProjectile(projectile) }
        bodyFixture.setHitByExplosionReceiver { explosion -> hitByExplosion(explosion) }

        return component
    }

    private fun hitByProjectile(projectile: IProjectileEntity) {
        if (projectile is SmallGreenMissile) crumble()
    }

    private fun hitByExplosion(explosion: IBodyEntity) {
        if (explosion is SpreadExplosion || (explosion is Explosion && explosion.owner is TorikoPlundge)) crumble()
    }

    private fun crumble() {
        destroy()

        for (i in 0 until PIECE_IMPULSES.size) {
            val spawn = body.getCenter()

            val impulse = PIECE_IMPULSES[i].cpy().scl(ConstVals.PPM.toFloat())

            val piece = MegaEntityFactory.fetch(BlockPiece::class)!!
            piece.spawn(
                props(
                    ConstKeys.POSITION pairTo spawn,
                    ConstKeys.IMPULSE pairTo impulse,
                    ConstKeys.COLOR pairTo BlockPieceColor.RED
                )
            )
        }

        game.audioMan.playSound(SoundAsset.THUMP_SOUND, false)
    }

    private fun defineDrawables() {
        val cells = body.getBounds().splitByCellSize(ConstVals.PPM.toFloat(), out)

        cells.forEach { x, y, bounds ->
            val key = "${x}_${y}"

            val sprite = GameSprite(region!!)
            sprite.setBounds(bounds!!)

            putSprite(key, sprite)
        }
    }
}
