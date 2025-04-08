package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.contracts.AbstractProjectile
import com.megaman.maverick.game.entities.explosions.ElecExplosion
import com.megaman.maverick.game.utils.extensions.pooledCopy
import com.megaman.maverick.game.world.body.*

class ElecDevilBodyPiece(game: MegamanMaverickGame) : AbstractProjectile(game), IAnimatedEntity {

    companion object {
        const val TAG = "ElecDevilProjectilePiece"
        private const val BEGIN_DUR = 0.5f
        private const val SPEED = 10f
        private var region: TextureRegion? = null
    }

    private lateinit var processState: ProcessState

    private val target = Vector2()
    private val start = Vector2()

    private val beginTimer = Timer(BEGIN_DUR)

    private lateinit var onEnd: () -> Unit

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.PROJECTILES_1.source, TAG)
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val start = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(start)
        this.start.set(start)

        val target = spawnProps.get(ConstKeys.TARGET, Vector2::class)!!
        this.target.set(target)

        onEnd = spawnProps.get(ConstKeys.END) as () -> Unit
        processState = ProcessState.BEGIN
        beginTimer.reset()
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        beginTimer.update(delta)
        if (!beginTimer.isFinished()) return@UpdatablesComponent
        if (beginTimer.isJustFinished()) GameLogger.debug(TAG, "update(): beginTimer just finished")

        processState = ProcessState.CONTINUE

        body.physics.velocity.set(target.pooledCopy().sub(body.getCenter()).nor().scl(SPEED * ConstVals.PPM))

        if (processState == ProcessState.CONTINUE && start.dst2(target) < start.dst2(body.getCenter())) {
            GameLogger.debug(TAG, "update(): set processState to END: body.getCenter=${body.getCenter()}")
            processState = ProcessState.END
            body.setCenter(target)
            spawnExplosion()
            onEnd.invoke()
            destroy()
        }
    })

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(2f * ConstVals.PPM, ConstVals.PPM.toFloat())

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        // TODO: create damager fixtures for body pieces

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.Companion.of(FixtureType.PROJECTILE, FixtureType.DAMAGER))
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
                .also { sprite -> sprite.setSize(2f * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            sprite.setCenter(body.getCenter())

            sprite.setOriginCenter()
            sprite.rotation = body.physics.velocity.angleDeg()
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG).animator(Animator(Animation(region!!, 2, 2, 0.1f, true)))
        .build()

    private fun spawnExplosion() {
        val explosion = MegaEntityFactory.fetch(ElecExplosion::class)!!
        explosion.spawn(props(ConstKeys.OWNER pairTo owner, ConstKeys.POSITION pairTo target))
    }
}
