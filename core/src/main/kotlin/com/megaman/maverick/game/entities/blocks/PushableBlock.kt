package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.cullables.CullablesComponent
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setBounds
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.ICullableEntity
import com.mega.game.engine.entities.contracts.ISpritesEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.megaman.components.leftSideFixture
import com.megaman.maverick.game.entities.megaman.components.rightSideFixture
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.PreciousGem
import com.megaman.maverick.game.entities.projectiles.PreciousShard
import com.megaman.maverick.game.entities.utils.getStandardEventCullingLogic
import com.megaman.maverick.game.events.EventType
import com.megaman.maverick.game.screens.levels.spawns.SpawnType
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*

class PushableBlock(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity, ICullableEntity {

    companion object {
        const val TAG = "PushableBlock"

        const val METAL_CRATE = "MetalCrate"

        private const val DEFAULT_FRICTION_X = 5f
        private const val X_VEL_CLAMP = 8f

        private const val PUSH_IMPULSE = 10f
        private const val PROJECTILE_IMPULSE = 5f

        private const val GRAVITY = 0.25f
        private const val GROUND_GRAVITY = 0.01f

        private const val BODY_WIDTH = 2f
        private const val BODY_HEIGHT = 2f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private class InnerBlock(game: MegamanMaverickGame, private val pushableBody: Body) : Block(game) {

        override fun hitByProjectile(projectileFixture: IFixture) {
            val projectile = projectileFixture.getEntity()
            if (projectile.isAny(PreciousGem::class, PreciousShard::class, Axe::class)) return

            val projectileX = projectileFixture.getShape().getBoundingRectangle().getX()

            var impulse = PROJECTILE_IMPULSE * ConstVals.PPM
            if (projectileX > pushableBody.getCenter().x) impulse *= -1f

            pushableBody.physics.velocity.x += impulse
        }
    }

    private var block: InnerBlock? = null
    private lateinit var spawnRoom: String

    override fun init() {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            gdxArrayOf(METAL_CRATE).forEach { regions.put(it, atlas.findRegion(it)) }
        }
        super.init()
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!
            .getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(spawn)

        block = InnerBlock(game, body)
        block!!.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.CULL_OUT_OF_BOUNDS pairTo false,
                ConstKeys.FIXTURE_LABELS pairTo objectSetOf(FixtureLabel.NO_SIDE_TOUCHIE),
                ConstKeys.BOUNDS pairTo GameRectangle().setSize(
                    BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM
                ),
                ConstKeys.BLOCK_FILTERS pairTo { entity: MegaGameEntity, block: MegaGameEntity ->
                    blockFilter(entity, block)
                },
            )
        )

        spawnRoom = spawnProps.get(SpawnType.SPAWN_ROOM, String::class)!!
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        block?.destroy()
        block = null
    }

    private fun blockFilter(entity: MegaGameEntity, block: MegaGameEntity) =
        entity == this && block == this.block

    private fun defineCullablesComponent() = CullablesComponent(
        objectMapOf(
            ConstKeys.CULL_EVENTS pairTo getStandardEventCullingLogic(
                this, objectSetOf(EventType.END_ROOM_TRANS), { event ->
                    val room = event.getProperty(ConstKeys.ROOM, RectangleMapObject::class)!!.name
                    val cull = room != spawnRoom
                    GameLogger.debug(
                        TAG,
                        "defineCullablesComponent(): currentRoom=$room, spawnRoom=$spawnRoom, cull=$cull"
                    )
                    cull
                }
            )
        )
    )

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        block!!.body.set(body)

        if (body.isSensing(BodySense.FEET_ON_GROUND)) when {
            megaman.leftSideFixture.overlaps(body.getBounds()) &&
                megaman.isFacing(Facing.LEFT) &&
                megaman.body.physics.velocity.x < 0f -> {
                val impulse = PUSH_IMPULSE * ConstVals.PPM * delta
                body.physics.velocity.x -= impulse
            }

            megaman.rightSideFixture.overlaps(body.getBounds()) &&
                megaman.isFacing(Facing.RIGHT) &&
                megaman.body.physics.velocity.x > 0f -> {
                val impulse = PUSH_IMPULSE * ConstVals.PPM * delta
                body.physics.velocity.x += impulse
            }
        } else body.physics.velocity.x = 0f
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.physics.velocityClamp.x = X_VEL_CLAMP * ConstVals.PPM
        body.physics.defaultFrictionOnSelf.x = DEFAULT_FRICTION_X
        body.physics.applyFrictionY = false

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(BODY_WIDTH * ConstVals.PPM, 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -ConstVals.PPM.toFloat()
        body.addFixture(feetFixture)
        debugShapes.add { feetFixture }

        body.preProcess.put(ConstKeys.DEFAULT) {
            val gravity = when {
                game.isProperty(ConstKeys.ROOM_TRANSITION, true) -> 0f
                body.isSensing(BodySense.FEET_ON_GROUND) -> GROUND_GRAVITY
                else -> GRAVITY
            }
            body.physics.gravity.y = -gravity * ConstVals.PPM

            if (game.isProperty(ConstKeys.ROOM_TRANSITION, true)) body.physics.velocity.setZero()
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body, BodyFixtureDef.of(FixtureType.BODY))
    }

    private fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        val component = SpritesComponent(sprite)
        component.putPreProcess { _, _ ->
            sprite.setRegion(regions[METAL_CRATE])
            sprite.setBounds(body.getBounds())
        }
        return component
    }

    override fun getType() = EntityType.BLOCK
}
