package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureRegion
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
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.IProjectileEntity
import com.megaman.maverick.game.entities.decorations.BlockPiece
import com.megaman.maverick.game.entities.decorations.BlockPiece.BlockPieceColor
import com.megaman.maverick.game.entities.enemies.TorikoPlundge
import com.megaman.maverick.game.entities.explosions.Explosion
import com.megaman.maverick.game.entities.explosions.SpreadExplosion
import com.megaman.maverick.game.entities.projectiles.SmallGreenMissile
import com.megaman.maverick.game.utils.extensions.getCenter
import com.megaman.maverick.game.world.body.*

class CrumblingBlock(game: MegamanMaverickGame) : Block(game), ISpritesEntity {

    companion object {
        const val TAG = "CrumblingBlock"
        private val PIECE_IMPULSES = gdxArrayOf(
            Vector2(-5f, 3f), Vector2(-3f, 5f), Vector2(3f, 5f), Vector2(5f, 3f)
        )
        private var region: TextureRegion? = null
    }

    private val cells = Matrix<GameRectangle>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PLATFORMS_1.source, TAG)
        super.init()
        addComponent(SpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)
        defineDrawables()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        sprites.clear()
    }

    override fun defineBodyComponent(): BodyComponent {
        val component = super.defineBodyComponent()
        val body = component.body

        body.fixtures.get(FixtureType.BLOCK).first()
            .setEntity(this)
            .setHitByProjectileReceiver { projectile -> hitByProjectile(projectile) }
            .setHitByExplosionReceiver { explosion -> hitByExplosion(explosion) }

        val feetFixture = Fixture(body, FixtureType.FEET, GameRectangle().setHeight(0.1f * ConstVals.PPM))
        feetFixture.setEntity(this)
        body.addFixture(feetFixture)

        body.preProcess.put(ConstKeys.FEET) {
            val feet = feetFixture.rawShape as GameRectangle

            val width = body.getWidth() - 0.25f * ConstVals.PPM
            feet.setWidth(width)

            val offsetY = -body.getHeight() / 2f
            feetFixture.offsetFromBodyAttachment.y = offsetY
        }

        return component
    }

    private fun hitByProjectile(projectile: IProjectileEntity) {
        if (projectile is SmallGreenMissile) crumble()
    }

    private fun hitByExplosion(explosion: IBodyEntity) {
        if (explosion is SpreadExplosion || (explosion is Explosion && explosion.owner is TorikoPlundge)) crumble()
    }

    private fun crumble() {
        GameLogger.debug(TAG, "crumble()")

        if (dead) {
            GameLogger.debug(TAG, "crumble(): already dead, nothing to do")
            return
        }

        destroy()

        cells.forEach { _, _, bounds ->
            for (i in 0 until PIECE_IMPULSES.size) {
                val spawn = bounds!!.getCenter()

                val impulse = PIECE_IMPULSES[i].cpy().scl(ConstVals.PPM.toFloat())

                val piece = MegaEntityFactory.fetch(BlockPiece::class)!!
                piece.spawn(
                    props(
                        ConstKeys.POSITION pairTo spawn,
                        ConstKeys.IMPULSE pairTo impulse,
                        ConstKeys.COLOR pairTo BlockPieceColor.GOLD
                    )
                )
            }
        }

        game.audioMan.playSound(SoundAsset.THUMP_SOUND, false)
    }

    private fun defineDrawables() {
        val cells = body.getBounds().splitByCellSize(ConstVals.PPM.toFloat(), cells)

        cells.forEach { x, y, bounds ->
            val key = "${x}_${y}"

            val sprite = GameSprite(region!!)
            sprite.setBounds(bounds!!)

            putSprite(key, sprite)
        }
    }
}
