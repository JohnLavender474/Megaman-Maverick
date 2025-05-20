package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Size
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Matrix
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.cullables.ICullable
import com.mega.game.engine.damage.IDamager
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
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.utils.moveTowards
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.extensions.getRandomPositionInBounds
import com.megaman.maverick.game.utils.extensions.overlaps
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class Beezee(game: MegamanMaverickGame) : AbstractEnemy(game, size = Size.SMALL), IAnimatedEntity, IFaceable,
    IOwnable<CannoHoney>, ICullable {

    companion object {
        const val TAG = "Beezee"

        private const val CULL_TIME = 0.1f

        private const val HOVER_SPEED = 8f
        private const val HOVER_SIZE = 5f
        private const val MIN_HOVER_DUR = 0.1f
        private const val MAX_HOVER_DUR = 0.5f
        private const val HOVER_LERP_SCALAR = 10f

        private const val ATTACK_SPEED = 10f
        private const val ATTACK_DELAY = 1f

        private const val SPLIT_PARENT_BOUNDS_ROWS = 2
        private const val SPLIT_PARENT_BOUNDS_COLS = 2

        private val animDefs = orderedMapOf(
            "hover" pairTo AnimationDef(3, 1, 0.1f, true),
            "attack" pairTo AnimationDef(3, 1, 0.05f, true)
        )
        private var regions = ObjectMap<String, TextureRegion>()
    }

    private enum class BeezeeState { HOVER, ATTACK }

    override var owner: CannoHoney? = null
    override lateinit var facing: Facing

    private lateinit var state: BeezeeState

    private val hoverTimer = Timer()
    private val hoverTarget = Vector2()
    private val hoverArea = GameRectangle().setSize(HOVER_SIZE * ConstVals.PPM)

    private val attackTrajectory = Vector2()
    private val attackDelay = Timer(ATTACK_DELAY)

    private val cullTimer = Timer(CULL_TIME)

    private val reusableBoundsMatrix = Matrix<GameRectangle>()
    private val reusableBoundsArray = Array<GameRectangle>()

    override fun init() {
        GameLogger.debug(TAG, "init(): hashCode=${hashCode()}")
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_1.source)
            AnimationUtils.loadRegions(TAG, atlas, animDefs.keys(), regions)
        }
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun onSpawn(spawnProps: Properties) {
        spawnProps.put(ConstKeys.CULL_OUT_OF_BOUNDS, false)
        GameLogger.debug(TAG, "onSpawn(): hashCode=${hashCode()}, spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val spawn = spawnProps.get(ConstKeys.POSITION, Vector2::class)!!
        body.setCenter(spawn)

        state = BeezeeState.HOVER
        resetHoverTimer()

        attackDelay.reset()
        cullTimer.reset()

        facing = UtilMethods.getRandom(Facing.LEFT, Facing.RIGHT)

        owner = spawnProps.get(ConstKeys.OWNER, CannoHoney::class)!!
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy(): hashCode=${hashCode()}")
        super.onDestroy()
        owner = null
    }

    override fun takeDamageFrom(damager: IDamager): Boolean {
        GameLogger.debug(TAG, "takeDamageFrom(): hashCode=${hashCode()}, damager=$damager")
        val damaged = super.takeDamageFrom(damager)
        if (damaged) setToAttack()
        return damaged
    }

    override fun shouldBeCulled(delta: Float) = cullTimer.isFinished()

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add update@{ delta ->
            if (state == BeezeeState.ATTACK &&
                !game.getGameCamera().overlaps(body.getBounds())
            ) cullTimer.update(delta)
            else cullTimer.reset()

            owner?.let { hoverArea.setCenter(it.body.getCenter()) }

            when (state) {
                BeezeeState.HOVER -> {
                    hoverTimer.update(delta)

                    if (!hoverTimer.isFinished()) {
                        body.physics.velocity.setZero()
                        return@update
                    }

                    if (hoverTimer.isJustFinished()) {
                        val target = findHoverTarget()
                        GameLogger.debug(TAG, "update(): HOVER: find hover target: $target")
                        hoverTarget.set(target)
                    }

                    moveTowards(hoverTarget, HOVER_SPEED * ConstVals.PPM, true, HOVER_LERP_SCALAR)

                    facing = if (body.physics.velocity.x < 0f) Facing.LEFT else Facing.RIGHT

                    if (body.getBounds().contains(hoverTarget)) {
                        GameLogger.debug(TAG, "update(): HOVER: reached hover target: $hoverTimer")
                        body.physics.velocity.setZero()
                        FacingUtils.setFacingOf(this)
                        resetHoverTimer()
                    }
                }
                BeezeeState.ATTACK -> {
                    attackDelay.update(delta)

                    if (!attackDelay.isFinished()) {
                        body.physics.velocity.setZero()
                        FacingUtils.setFacingOf(this)
                        return@update
                    }

                    if (attackDelay.isJustFinished()) {
                        GameLogger.debug(
                            TAG, "update(): hashCode=${hashCode()}: " +
                                "ATTACK: attack delay just finished"
                        )

                        attackTrajectory.set(
                            megaman.body.getCenter()
                                .sub(body.getCenter())
                                .nor()
                                .scl(ATTACK_SPEED * ConstVals.PPM)
                        )
                    }

                    body.physics.velocity.set(attackTrajectory)

                    facing = if (body.physics.velocity.x < 0f) Facing.LEFT else Facing.RIGHT
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(0.5f * ConstVals.PPM)

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        body.preProcess.put(ConstKeys.DEFAULT) {
            body.drawingColor = if (owner == null) Color.RED else Color.BLUE
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(
            TAG, GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 5))
                .also { sprite -> sprite.setSize(1.5f * ConstVals.PPM) }
        )
        .updatable { _, sprite ->
            sprite.hidden = damageBlink
            sprite.setCenter(body.getCenter())
            sprite.setFlip(isFacing(Facing.RIGHT), false)
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { state.name.lowercase() }
                .applyToAnimations { animations ->
                    AnimationUtils.loadAnimationDefs(animDefs, animations, regions)
                }
                .build()
        )
        .build()

    private fun resetHoverTimer() {
        GameLogger.debug(TAG, "resetHoverTimer(): hashCode=${hashCode()}")
        val duration = UtilMethods.getRandom(MIN_HOVER_DUR, MAX_HOVER_DUR)
        hoverTimer.resetDuration(duration)
    }

    private fun findHoverTarget(): Vector2 {
        GameLogger.debug(TAG, "findHoverTarget()")

        reusableBoundsArray.clear()
        reusableBoundsMatrix.clear()

        val sections = hoverArea
            .splitIntoCells(
                SPLIT_PARENT_BOUNDS_ROWS,
                SPLIT_PARENT_BOUNDS_COLS,
                reusableBoundsMatrix
            )
            .flatten(reusableBoundsArray)
        sections.shuffle()
        val target = sections[0].getRandomPositionInBounds()

        reusableBoundsArray.clear()
        reusableBoundsMatrix.clear()

        GameLogger.debug(TAG, "findHoverTarget(): set hover target: $target")

        return target
    }

    internal fun setToAttack() {
        GameLogger.debug(TAG, "setToAttack(): hashCode=${hashCode()}")
        state = BeezeeState.ATTACK
        owner = null
    }
}
