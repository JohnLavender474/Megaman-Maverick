package com.megaman.maverick.game.entities.explosions

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.animations.Animator
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.extensions.getTextureRegion
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setCenter
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.GameEntity
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getCenter

class TubeBeamExplosion(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, IAnimatedEntity,
    IDamager, IOwnable<IGameEntity> {

    companion object {
        const val TAG = "TubeBeamExplosion"
        private const val ANIM_FRAME_DUR = 0.05f
        private const val EXPLOSION_DUR = 0.2f
        private var region: TextureRegion? = null
    }

    override var owner: IGameEntity? = null

    private val timer = Timer(EXPLOSION_DUR)

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (region == null) region = game.assMan.getTextureRegion(TextureAsset.EXPLOSIONS_1.source, TAG)
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        owner = spawnProps.get(ConstKeys.OWNER) as GameEntity?

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        timer.reset()
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        timer.update(delta)
        if (timer.isFinished()) destroy()
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.75f * ConstVals.PPM)
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val damagerFixture = Fixture(body, FixtureType.DAMAGER, GameCircle().setRadius(body.getWidth() / 2f))
        body.addFixture(damagerFixture)
        damagerFixture.drawingColor = Color.RED
        debugShapes.add { damagerFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 15))
        sprite.setSize(4.5f * ConstVals.PPM)
        val component = SpritesComponent(sprite)
        component.putUpdateFunction { _, _ -> sprite.setCenter(body.getCenter()) }
        return component
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val animation = Animation(region!!, 2, 2, ANIM_FRAME_DUR, false)
        val animator = Animator(animation)
        return AnimationsComponent(this, animator)
    }

    override fun getType() = EntityType.EXPLOSION

    override fun getTag() = TAG
}
