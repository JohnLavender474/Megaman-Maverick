package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.engine.animations.Animation
import com.engine.animations.AnimationsComponent
import com.engine.animations.Animator
import com.engine.animations.IAnimation
import com.engine.common.GameLogger
import com.engine.common.enums.Direction
import com.engine.common.enums.Facing
import com.engine.common.enums.Position
import com.engine.common.extensions.gdxArrayOf
import com.engine.common.extensions.getTextureAtlas
import com.engine.common.extensions.objectMapOf
import com.engine.common.extensions.objectSetOf
import com.engine.common.interfaces.IFaceable
import com.engine.common.interfaces.isFacing
import com.engine.common.objects.Properties
import com.engine.common.objects.props
import com.engine.common.shapes.GameRectangle
import com.engine.common.shapes.getPosition
import com.engine.common.time.Timer
import com.engine.damage.IDamageable
import com.engine.damage.IDamager
import com.engine.drawables.shapes.DrawableShapesComponent
import com.engine.drawables.shapes.IDrawableShape
import com.engine.drawables.sorting.DrawingPriority
import com.engine.drawables.sorting.DrawingSection
import com.engine.drawables.sprites.GameSprite
import com.engine.drawables.sprites.SpritesComponent
import com.engine.drawables.sprites.setPosition
import com.engine.drawables.sprites.setSize
import com.engine.entities.IGameEntity
import com.engine.entities.contracts.IAnimatedEntity
import com.engine.entities.contracts.IChildEntity
import com.engine.entities.contracts.IParentEntity
import com.engine.updatables.UpdatablesComponent
import com.engine.world.Body
import com.engine.world.BodyComponent
import com.engine.world.BodyType
import com.engine.world.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.dmgNeg
import com.megaman.maverick.game.entities.EntityType
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.bosses.GutsTank.GutsTankAttackState
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.explosions.ChargedShotExplosion
import com.megaman.maverick.game.entities.factories.EntityFactories
import com.megaman.maverick.game.entities.factories.impl.BlocksFactory
import com.megaman.maverick.game.entities.factories.impl.ExplosionsFactory
import com.megaman.maverick.game.entities.projectiles.Bullet
import com.megaman.maverick.game.entities.projectiles.ChargedShot
import com.megaman.maverick.game.utils.getMegamanMaverickGame
import com.megaman.maverick.game.world.BodyComponentCreator
import com.megaman.maverick.game.world.FixtureLabel
import com.megaman.maverick.game.world.FixtureType
import kotlin.reflect.KClass

class GutsTank(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IParentEntity {

    enum class GutsTankMoveState {
        PAUSE, MOVE,
    }

    enum class GutsTankAttackState {
        LAUNCH_FIST, LAUNCH_RUNNING_METS, LAUNCH_FLYING_METS, CHUNK_BULLETS, SHOOT_BLASTS
    }/*
    TODO:
      - cannot chunk bullets and shoot blasts at the same time
      - cannot launch fist and launch running or flying mets at the same time
      -
    */

    companion object {
        const val TAG = "GutsTank"

        private const val BODY_WIDTH = 8f
        private const val BODY_HEIGHT = 4.46875f

        private const val TANK_BLOCK_HEIGHT = 2.0365f

        private const val BODY_BLOCK_WIDTH = 5.65f
        private const val BODY_BLOCK_HEIGHT = 12f

        private const val X_VEL = 2f
        private const val MOVEMENT_PAUSE_DUR = 2f

        private const val BULLETS_TO_CHUNK = 5
        private const val BULLET_CHUNK_DELAY = 0.5f

        private const val BLASTS_TO_SHOOT = 3
        private const val BLAST_SHOOT_DELAY = 1.5f

        private const val LAUNCH_FIST_DELAY = 10f

        private var mouthOpenRegion: TextureRegion? = null
        private var mouthClosedRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()
    override var children = Array<IGameEntity>()

    private val attackStates = ObjectSet<GutsTankAttackState>()

    private val movementPauseTimer = Timer(MOVEMENT_PAUSE_DUR)
    private val bulletChunkDelayTimer = Timer(BULLET_CHUNK_DELAY)
    private val blastShootDelayTimer = Timer(BLAST_SHOOT_DELAY)
    private val launchFistDelayTimer = Timer(LAUNCH_FIST_DELAY)

    private val ableToLaunchFist: Boolean
        get() = fist?.dead == false &&
                !fist!!.body.overlaps(megaman.body as Rectangle) &&
                launchFistDelayTimer.isFinished() &&
                !attackStates.contains(GutsTankAttackState.LAUNCH_FIST)

    private lateinit var frontPoint: Vector2
    private lateinit var backPoint: Vector2
    private lateinit var tankBlockOffset: Vector2
    private lateinit var bodyBlockOffset: Vector2
    private lateinit var moveState: GutsTankMoveState

    private var tankBlock: Block? = null
    private var bodyBlock: Block? = null
    private var fist: GutsTankFist? = null

    private var moveToFront = true

    private var bulletsChunked = 0
    private var blastsShot = 0

    override fun init() {
        if (mouthOpenRegion == null || mouthClosedRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            mouthOpenRegion = atlas.findRegion("GutsTank/MouthOpen")
            mouthClosedRegion = atlas.findRegion("GutsTank/MouthClosed")
        }
        super<AbstractBoss>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        super.spawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomLeftPoint()
        body.setSize(BODY_WIDTH * ConstVals.PPM, BODY_HEIGHT * ConstVals.PPM)
        body.setBottomLeftToPoint(spawn)

        tankBlock = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.STANDARD)!! as Block
        val tankBlockBounds =
            GameRectangle(spawn.x, spawn.y, BODY_WIDTH * ConstVals.PPM, TANK_BLOCK_HEIGHT * ConstVals.PPM)
        game.gameEngine.spawn(
            tankBlock!!, props(
                ConstKeys.CULL_OUT_OF_BOUNDS to false,
                ConstKeys.BOUNDS to tankBlockBounds,
                ConstKeys.FIXTURE_LABELS to objectSetOf(FixtureLabel.NO_PROJECTILE_COLLISION),
                ConstKeys.FIXTURES to gdxArrayOf(
                    Pair(FixtureType.SHIELD, props(ConstKeys.DIRECTION to Direction.DOWN))
                )
            )
        )

        bodyBlock = EntityFactories.fetch(EntityType.BLOCK, BlocksFactory.STANDARD)!! as Block
        val bodyBlockBounds = GameRectangle().setSize(
            BODY_BLOCK_WIDTH * ConstVals.PPM, BODY_BLOCK_HEIGHT * ConstVals.PPM
        ).setBottomRightToPoint(tankBlockBounds.getBottomRightPoint())
        game.gameEngine.spawn(
            bodyBlock!!, props(
                ConstKeys.CULL_OUT_OF_BOUNDS to false,
                ConstKeys.BOUNDS to bodyBlockBounds,
                ConstKeys.FIXTURE_LABELS to objectSetOf(
                    FixtureLabel.NO_SIDE_TOUCHIE, FixtureLabel.NO_PROJECTILE_COLLISION
                ),
                ConstKeys.FIXTURES to gdxArrayOf(
                    Pair(FixtureType.SHIELD, props(ConstKeys.DIRECTION to Direction.UP))
                )
            )
        )

        tankBlockOffset = tankBlockBounds.getCenter().sub(body.getCenter())
        bodyBlockOffset = bodyBlockBounds.getCenter().sub(body.getCenter())

        fist = GutsTankFist(game as MegamanMaverickGame)
        game.gameEngine.spawn(fist!!, props(ConstKeys.PARENT to this))

        frontPoint = spawnProps.get(ConstKeys.FRONT, RectangleMapObject::class)!!.rectangle.getPosition()
        backPoint = spawnProps.get(ConstKeys.BACK, RectangleMapObject::class)!!.rectangle.getPosition()

        moveState = GutsTankMoveState.MOVE
        moveToFront = true
        attackStates.clear()

        movementPauseTimer.reset()

        bulletChunkDelayTimer.reset()
        bulletsChunked = 0

        blastShootDelayTimer.reset()
        blastsShot = 0

        launchFistDelayTimer.reset()
    }

    override fun onDestroy() {
        super<AbstractBoss>.onDestroy()
        children.forEach { it.kill() }
        children.clear()

        fist?.kill()
        fist = null

        tankBlock!!.onDestroy()
        tankBlock = null

        bodyBlock!!.onDestroy()
        bodyBlock = null
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (!tankBlock!!.body.getCenter()
                    .epsilonEquals(body.getCenter().add(tankBlockOffset), 0.01f) || !bodyBlock!!.body.getCenter()
                    .epsilonEquals(body.getCenter().add(bodyBlockOffset), 0.01f)
            ) {
                tankBlock!!.body.setCenter(body.getCenter().add(tankBlockOffset))
                bodyBlock!!.body.setCenter(body.getCenter().add(bodyBlockOffset))
            }

            launchFistDelayTimer.update(delta)
            if (fist?.dead == true) fist = null
            if (ableToLaunchFist) {
                fist!!.launch()
                attackStates.add(GutsTankAttackState.LAUNCH_FIST)
            }

            when (moveState) {
                GutsTankMoveState.MOVE -> {
                    if (moveToFront) {
                        body.physics.velocity.x = -X_VEL * ConstVals.PPM
                        tankBlock!!.body.physics.velocity.x = -X_VEL * ConstVals.PPM
                        bodyBlock!!.body.physics.velocity.x = -X_VEL * ConstVals.PPM
                        if (body.x <= frontPoint.x) {
                            body.x = frontPoint.x
                            body.physics.velocity.x = 0f
                            moveState = GutsTankMoveState.PAUSE
                            moveToFront = false
                            movementPauseTimer.reset()
                        }
                    } else {
                        body.physics.velocity.x = X_VEL * ConstVals.PPM
                        tankBlock!!.body.physics.velocity.x = X_VEL * ConstVals.PPM
                        bodyBlock!!.body.physics.velocity.x = X_VEL * ConstVals.PPM
                        if (body.x >= backPoint.x) {
                            body.x = backPoint.x
                            body.physics.velocity.x = 0f
                            moveState = GutsTankMoveState.PAUSE
                            moveToFront = true
                            movementPauseTimer.reset()
                        }

                    }
                }

                GutsTankMoveState.PAUSE -> {
                    body.physics.velocity.x = 0f
                    tankBlock!!.body.physics.velocity.x = 0f
                    bodyBlock!!.body.physics.velocity.x = 0f
                    movementPauseTimer.update(delta)
                    if (movementPauseTimer.isFinished()) moveState = GutsTankMoveState.MOVE
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        val damageableFixture = Fixture(
            GameRectangle().setSize(0.75f * ConstVals.PPM, 0.5f * ConstVals.PPM), FixtureType.DAMAGEABLE
        )
        damageableFixture.offsetFromBodyCenter.x = -1f * ConstVals.PPM
        damageableFixture.offsetFromBodyCenter.y = 2.5f * ConstVals.PPM
        body.addFixture(damageableFixture)
        damageableFixture.shape.color = Color.PURPLE
        debugShapes.add { damageableFixture.shape }

        val damagerFixture = Fixture(GameRectangle().setSize(1f * ConstVals.PPM), FixtureType.DAMAGER)
        damageableFixture.offsetFromBodyCenter.x = -2f * ConstVals.PPM
        damageableFixture.offsetFromBodyCenter.y = 2.5f * ConstVals.PPM
        body.addFixture(damageableFixture)
        damagerFixture.shape.color = Color.RED
        debugShapes.add { damagerFixture.shape }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 0))
        sprite.setSize(8f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, TAG to sprite)
        spritesComponent.putUpdateFunction(TAG) { _, _sprite ->
            _sprite as GameSprite
            _sprite.setPosition(body.getBottomLeftPoint(), Position.BOTTOM_LEFT)
        }
        return spritesComponent
    }

    internal fun finishAttack(attackState: GutsTankAttackState) {
        attackStates.remove(attackState)
        when (attackState) {
            GutsTankAttackState.CHUNK_BULLETS -> {
                bulletsChunked = 0
                bulletChunkDelayTimer.reset()
            }

            GutsTankAttackState.SHOOT_BLASTS -> {
                blastsShot = 0
                blastShootDelayTimer.reset()
            }

            GutsTankAttackState.LAUNCH_FIST -> {
                launchFistDelayTimer.reset()
            }

            GutsTankAttackState.LAUNCH_RUNNING_METS -> TODO()
            GutsTankAttackState.LAUNCH_FLYING_METS -> TODO()
        }
    }

    internal fun laugh() {
        GameLogger.debug(TAG, "GutsTank laughs")
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            "mouth_closed"
        }
        val animations = objectMapOf<String, IAnimation>(
            "mouth_closed" to Animation(mouthClosedRegion!!, 1, 2, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}

class GutsTankFist(game: MegamanMaverickGame) : AbstractEnemy(game, dmgDuration = DAMAGE_DURATION), IChildEntity,
    IFaceable {

    enum class GutsTankFistState {
        ATTACHED, LAUNCHED, RETURNING
    }

    companion object {
        const val TAG = "GutsTankFist"
        private const val DAMAGE_DURATION = 0.75f
        private const val LAUNCH_DELAY = 1f
        private const val LAUNCH_SPEED = 10f
        private const val RETURN_DELAY = 1f
        private const val RETURN_SPEED = 2f
        private const val FIST_OFFSET_X = -2.5f
        private const val FIST_OFFSET_Y = 0.65f
        private var fistRegion: TextureRegion? = null
        private var launchedRegion: TextureRegion? = null
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        Bullet::class to dmgNeg(1), ChargedShot::class to dmgNeg {
            it as ChargedShot
            if (it.fullyCharged) 3 else 2
        }, ChargedShotExplosion::class to dmgNeg(1)
    )
    override var parent: IGameEntity? = null
    override lateinit var facing: Facing

    private val launchDelayTimer = Timer(LAUNCH_DELAY)
    private val returnDelayTimer = Timer(RETURN_DELAY)

    private lateinit var state: GutsTankFistState
    private lateinit var target: Vector2

    private val attachment: Vector2
        get() = (parent as GutsTank).body.getCenter().add(FIST_OFFSET_X * ConstVals.PPM, FIST_OFFSET_Y * ConstVals.PPM)

    override fun init() {
        if (launchedRegion == null || fistRegion == null) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES.source)
            fistRegion = atlas.findRegion("GutsTank/Fist")
            launchedRegion = atlas.findRegion("GutsTank/FistLaunched")
        }
        super<AbstractEnemy>.init()
        addComponent(defineAnimationsComponent())
    }

    override fun spawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        super.spawn(spawnProps)

        parent = spawnProps.get(ConstKeys.PARENT, GutsTank::class)
        state = GutsTankFistState.ATTACHED
        facing = Facing.LEFT
    }

    override fun onDestroy() {
        super<AbstractEnemy>.onDestroy()
        if (getCurrentHealth() <= ConstVals.MIN_HEALTH) {
            val explosion = EntityFactories.fetch(EntityType.EXPLOSION, ExplosionsFactory.EXPLOSION)!!
            game.gameEngine.spawn(
                explosion,
                props(
                    ConstKeys.POSITION to body.getCenter(),
                    ConstKeys.SOUND to SoundAsset.EXPLOSION_1_SOUND
                )
            )
        }
    }

    override fun onDamageInflictedTo(damageable: IDamageable) = (parent as GutsTank).laugh()

    fun launch() {
        target = getMegamanMaverickGame().megaman.body.getCenter()
        GameLogger.debug(TAG, "Target on launch: $target")
        state = GutsTankFistState.LAUNCHED
        launchDelayTimer.reset()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (state) {
                GutsTankFistState.ATTACHED -> {
                    facing = Facing.LEFT
                    body.physics.velocity.setZero()
                    body.setCenter(attachment)
                }

                GutsTankFistState.LAUNCHED -> {
                    facing = Facing.LEFT
                    launchDelayTimer.update(delta)
                    if (!launchDelayTimer.isFinished()) {
                        body.physics.velocity.setZero()
                        return@add
                    }
                    body.physics.velocity = target.cpy().sub(body.getCenter()).nor().scl(LAUNCH_SPEED * ConstVals.PPM)
                    if (body.contains(target)) {
                        GameLogger.debug(TAG, "Fist hit target")
                        state = GutsTankFistState.RETURNING
                        returnDelayTimer.reset()
                    }
                }

                GutsTankFistState.RETURNING -> {
                    facing = Facing.RIGHT
                    returnDelayTimer.update(delta)
                    if (returnDelayTimer.isFinished()) {
                        body.physics.velocity = attachment.cpy().sub(body.getCenter()).nor().scl(
                            RETURN_SPEED * ConstVals.PPM
                        )
                        if (body.contains(attachment)) {
                            state = GutsTankFistState.ATTACHED
                            (parent as GutsTank).finishAttack(GutsTankAttackState.LAUNCH_FIST)
                        }
                    } else body.physics.velocity.setZero()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(1.25f * ConstVals.PPM)
        body.color = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body }

        val damagerFixture = Fixture(
            GameRectangle().setSize(1.05f * ConstVals.PPM), FixtureType.DAMAGER
        )
        body.addFixture(damagerFixture)
        damagerFixture.shape.color = Color.RED
        debugShapes.add { damagerFixture.shape }

        val damageableFixture = Fixture(
            GameRectangle().setSize(0.2f * ConstVals.PPM, 1.05f * ConstVals.PPM), FixtureType.DAMAGEABLE
        )
        body.addFixture(damageableFixture)
        damageableFixture.shape.color = Color.PURPLE
        debugShapes.add { damageableFixture.shape }

        val shieldFixture = Fixture(
            GameRectangle().setSize(1.05f * ConstVals.PPM), FixtureType.SHIELD
        )
        body.addFixture(shieldFixture)
        shieldFixture.shape.color = Color.BLUE
        debugShapes.add { shieldFixture.shape }

        body.preProcess.put(ConstKeys.DEFAULT) {
            shieldFixture.offsetFromBodyCenter.x = 0.2f * facing.value * ConstVals.PPM
            damageableFixture.offsetFromBodyCenter.x = 0.75f * -facing.value * ConstVals.PPM
        }

        addComponent(DrawableShapesComponent(this, debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 10))
        sprite.setSize(2.25f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(this, TAG to sprite)
        spritesComponent.putUpdateFunction(TAG) { _, _sprite ->
            _sprite as GameSprite
            _sprite.hidden = damageBlink
            val position = when (state) {
                GutsTankFistState.ATTACHED, GutsTankFistState.LAUNCHED -> Position.CENTER_LEFT

                GutsTankFistState.RETURNING -> Position.CENTER_RIGHT
            }
            val bodyPosition = body.getPositionPoint(position)
            _sprite.setPosition(bodyPosition, position)
            _sprite.setFlip(isFacing(Facing.LEFT), false)
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        val keySupplier: () -> String? = {
            when (state) {
                GutsTankFistState.ATTACHED -> "fist"
                else -> "launched"
            }
        }
        val animations = objectMapOf<String, IAnimation>(
            "fist" to Animation(fistRegion!!), "launched" to Animation(launchedRegion!!, 2, 1, 0.1f, true)
        )
        val animator = Animator(keySupplier, animations)
        return AnimationsComponent(this, animator)
    }
}