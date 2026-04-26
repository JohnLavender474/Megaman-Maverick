package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.IGameShape2D
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.events.Event
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.IFixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.explosions.AsteroidExplosion
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.world.body.*

class WilyPlaneBomb(game: MegamanMaverickGame) : AbstractProjectile(game) {

    companion object {
        const val TAG = "WilyPlaneBomb"
        private const val GRAVITY = 37.5f
        private const val VEL_Y = 10f
        private const val BLAST_DUR = 1f
        private const val EXPLODE_PERIOD = 0.05f
        private const val EXPLOSION_RADIUS = 2.5f
        private var region: TextureRegion? = null
    }

    private var blink = false
    private var blasting = false
    private val blastTimer = Timer(BLAST_DUR)
    private val explodeTimer = Timer(EXPLODE_PERIOD)

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_2.source, TAG)
        super.init()
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        blasting = false
        blastTimer.reset()

        explodeTimer.reset()

        blink = false
    }

    override fun hitBlock(blockFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (!blasting) explodeAndDie()
    }

    override fun hitSand(sandFixture: IFixture, thisShape: IGameShape2D, otherShape: IGameShape2D) {
        if (!blasting) explodeAndDie()
    }

    override fun explodeAndDie(vararg params: Any?) {
        GameLogger.debug(TAG, "explodeAndDie()")

        body.physics.velocity.setZero()
        body.physics.gravityOn = false

        blasting = true
        blastTimer.reset()

        explodeTimer.reset()

        game.eventsMan.submitEvent(
            Event(
                EventType.SHAKE_CAM, props(
                    ConstKeys.DURATION pairTo ConstVals.SHAKE_DUR,
                    ConstKeys.INTERVAL pairTo ConstVals.SHAKE_INTERVAL,
                    ConstKeys.X pairTo ConstVals.SHAKE_X * ConstVals.PPM,
                    ConstKeys.Y pairTo ConstVals.SHAKE_Y * ConstVals.PPM
                )
            )
        )
    }

    private fun spawnExplosion() {
        GameLogger.debug(TAG, "spawnExplosion()")

        val angle = MathUtils.random(MathUtils.PI2)
        val distance = MathUtils.random(0f, EXPLOSION_RADIUS * ConstVals.PPM)

        val center = body.getCenter()
        val position = GameObjectPools.fetch(Vector2::class).set(
            center.x + distance * MathUtils.cos(angle),
            center.y + distance * MathUtils.sin(angle)
        )

        val explosion = MegaEntityFactory.fetch(AsteroidExplosion::class)!!
        explosion.spawn(
            props(
                ConstKeys.SOUND pairTo true,
                ConstKeys.OWNER pairTo owner,
                ConstKeys.DAMAGER pairTo true,
                ConstKeys.POSITION pairTo position,
            )
        )
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        if (!blasting) return@UpdatablesComponent

        explodeTimer.update(delta)
        if (explodeTimer.isJustFinished()) {
            blink = !blink
            spawnExplosion()
            explodeTimer.reset()
        }

        blastTimer.update(delta)
        if (blastTimer.isFinished()) game.engine.destroy(this)
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.25f * ConstVals.PPM, 2f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.physics.gravityOn = true

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            if (!blasting) {
                if (body.physics.velocity.y > -VEL_Y * ConstVals.PPM)
                    body.physics.velocity.y = -VEL_Y * ConstVals.PPM
                body.physics.gravity.set(0f, -GRAVITY * ConstVals.PPM)
            } else body.physics.gravity.setZero()
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.PROJECTILE, FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(region!!, DrawingPriority(DrawingSection.PLAYGROUND, 2))
        sprite.setSize(2f * ConstVals.PPM, 3f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.hidden = blasting && blink
        }
        return component
    }

    override fun getTag() = TAG
}
