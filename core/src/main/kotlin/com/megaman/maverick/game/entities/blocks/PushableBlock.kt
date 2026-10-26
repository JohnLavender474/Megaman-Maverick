package com.megaman.maverick.game.entities.blocks

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.maverick.game.engine.common.GameLogger
import com.maverick.game.engine.common.enums.Facing
import com.maverick.game.engine.common.enums.Position
import com.maverick.game.engine.common.extensions.*
import com.maverick.game.engine.common.objects.Properties
import com.maverick.game.engine.common.objects.pairTo
import com.maverick.game.engine.common.objects.props
import com.maverick.game.engine.common.shapes.GameRectangle
import com.maverick.game.engine.cullables.CullablesComponent
import com.maverick.game.engine.drawables.shapes.DrawableShapesComponent
import com.maverick.game.engine.drawables.shapes.IDrawableShape
import com.maverick.game.engine.drawables.sprites.GameSprite
import com.maverick.game.engine.drawables.sprites.SpritesComponent
import com.maverick.game.engine.drawables.sprites.setBounds
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.contracts.IBodyEntity
import com.maverick.game.engine.entities.contracts.ICullableEntity
import com.maverick.game.engine.entities.contracts.ISpritesEntity
import com.maverick.game.engine.entities.facing
import com.maverick.game.engine.players.any
import com.maverick.game.engine.updatables.UpdatablesComponent
import com.maverick.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.players
import com.megaman.maverick.game.entities.leftSideFixture
import com.megaman.maverick.game.entities.megaman.components.leftSideFixture
import com.megaman.maverick.game.entities.megaman.components.rightSideFixture
import com.megaman.maverick.game.entities.projectiles.Axe
import com.megaman.maverick.game.entities.projectiles.PreciousGem
import com.megaman.maverick.game.entities.projectiles.PreciousShard
import com.megaman.maverick.game.entities.rightSideFixture
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

        private const val GRAVITY = 37.5f
        private const val GROUND_GRAVITY = 1.5f

        private const val BODY_WIDTH = 2f
        private const val BODY_HEIGHT = 2f

        private val regions = ObjectMap<String, TextureRegion>()
    }

    private class InnerBlock(game: MegamanMaverickGame, private val pushableBody: Body) : Block(game) {

        override fun hitByProjectile(projectileFixture: IFixture) {
            val projectile = projectileFixture.getEntity()
            if (projectile.isAny(PreciousGem::class, PreciousShard::class, Axe::class)) return

            val projectileX = projectileFixture.getShape().getBoundingRectangle().getX()

            val isProjectileRightOfBlock = projectileX > pushableBody.getCenter().x

            var impulse = PROJECTILE_IMPULSE * ConstVals.PPM
            if (isProjectileRightOfBlock) impulse *= -1f

            if (players.any {
                    (impulse > 0f && it.leftSideFixture.overlaps(pushableBody.getBounds())) ||
                        (impulse < 0f && it.rightSideFixture.overlaps(pushableBody.getBounds()))
                }
            ) {
                pushableBody.physics.velocity.x = 0f
                return
            }

            pushableBody.physics.velocity.x += impulse
        }
    }

    private var block: InnerBlock? = null

    private lateinit var spawnRoom: String

    override fun init(vararg params: Any) {
        GameLogger.debug(TAG, "init()")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.PLATFORMS_1.source)
            gdxArrayOf(METAL_CRATE).forEach { regions.put(it, atlas.findRegion(it)) }
        }
        super.init()
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineCullablesComponent())
        addComponent(defineUpdatablesComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        val currentRoom = game.getCurrentRoom()?.name
        GameLogger.debug(TAG, "onSpawn(): currentRoom=$currentRoom, spawnProps=$spawnProps")
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
                this, objectSetOf(EventType.PLAYER_SPAWN, EventType.END_ROOM_TRANS), cull@{ event ->
                    val room = game.getCurrentRoom()?.name
                    val cull = room != spawnRoom
                    GameLogger.debug(
                        TAG,
                        "defineCullablesComponent(): event=$event, currentRoom=$room, spawnRoom=$spawnRoom, cull=$cull"
                    )
                    return@cull cull
                }
            )
        )
    )

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        block!!.body.set(body)

        if (body.isSensing(BodySense.FEET_ON_GROUND)) when {
            players.any {
                it.leftSideFixture.overlaps(body.getBounds()) &&
                    it.facing == Facing.LEFT &&
                    it.body.physics.velocity.x < 0f
            } -> {
                val impulse = PUSH_IMPULSE * ConstVals.PPM * delta
                body.physics.velocity.x -= impulse
            }

            players.any {
                it.rightSideFixture.overlaps(body.getBounds()) &&
                    it.facing == Facing.RIGHT &&
                    it.body.physics.velocity.x > 0f
            } -> {
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

            val velocity = body.physics.velocity
            if (players.any {
                    (velocity.x > 0f && it.leftSideFixture.overlaps(body.getBounds())) ||
                        (velocity.x < 0f && it.rightSideFixture.overlaps(body.getBounds()))
                }
            ) velocity.x = 0f
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
