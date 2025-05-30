package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.map
import com.mega.game.engine.common.extensions.toGdxArray
import com.mega.game.engine.common.interfaces.IActivatable
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.IntPair
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sorting.DrawingPriority
import com.mega.game.engine.drawables.sorting.DrawingSection
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.IDrawableShapesEntity
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
import com.megaman.maverick.game.entities.bosses.ElecDevilConstants.BODY_SPRITE_FACING_LEFT_OFFSET_X
import com.megaman.maverick.game.entities.bosses.ElecDevilConstants.BODY_SPRITE_FACING_RIGHT_OFFSET_X
import com.megaman.maverick.game.entities.contracts.IHazard
import com.megaman.maverick.game.entities.contracts.IOwnable
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.utils.misc.LightSourceUtils
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.getBounds
import com.megaman.maverick.game.world.body.getPositionPoint

class ElecDevilBodyPieces(game: MegamanMaverickGame) : MegaGameEntity(game), IBodyEntity, ISpritesEntity,
    IAnimatedEntity, IDrawableShapesEntity, IOwnable<ElecDevilBody>, IFaceable, IActivatable, IDamager, IHazard {

    companion object {
        const val TAG = "ElecDevilBodyPieces"

        private const val BRIGHT_SOURCE_RADIUS = 5
        private const val BRIGHT_SOURCE_RADIANCE = 3f

        private const val DIM_SOURCE_RADIUS = 2
        private const val DIM_SOURCE_RADIANCE = 1.25f

        private const val STAND_BY_ALPHA = 0.5f
        private const val STAND_BY_BLINK_DELAY = 0.1f

        private val colors = gdxArrayOf(ConstKeys.WHITE, ConstKeys.BLUE, ConstKeys.GREEN)
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override var owner: ElecDevilBody? = null
    override lateinit var facing: Facing
    override var on = false

    private val standByPieces = OrderedSet<IntPair>()
    private val activePieces = OrderedSet<IntPair>()

    private val lightSourceKeys = ObjectSet<Int>()
    private val lightSourceSendEventDelay = Timer(ElecDevilConstants.LIGHT_SOURCE_SEND_EVENT_DELAY)

    private val standByBlinkTimer = Timer(STAND_BY_BLINK_DELAY)
    private var standByBlink = false

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_3.source)
            ElecDevilConstants.forEachCell { row, column ->
                val rowColumnKey = ElecDevilConstants.getRowColumnKey(row, column)
                colors.forEach { color ->
                    val piece = atlas.findRegion("${ElecDevil.TAG}/${ConstKeys.PIECES}/${color}/$rowColumnKey")
                    if (piece == null) throw IllegalStateException("Piece region is null: ${color}/${rowColumnKey}")
                    regions.put("${color}/${rowColumnKey}", piece)
                }
            }
        }
        super.init()
        addComponent(defineUpdatablesComponent())
        addComponent(defineBodyComponent())
        addComponent(defineSpritesComponent())
        addComponent(defineAnimationsComponent())
        GameLogger.debug(
            TAG,
            "init():\n" +
                "\thashcode=${hashCode()}\n," +
                "\tregions=${regions.map { it != null }}," +
                "\tsprites=${sprites.map { entry -> "${entry.key}:${entry.value.hashCode()}" }},\n" +
                "\tanimators=${animators.map { entry -> "${entry.key}:${entry.value.hashCode()}" }}"
        )
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): hashcode=${hashCode()}, spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        setStateOfAllPieces(ElecDevilBodyPieceState.INACTIVE)

        facing = spawnProps.get(ConstKeys.FACING, Facing::class)!!
        owner = spawnProps.get(ConstKeys.OWNER, ElecDevilBody::class)!!

        body.set(owner!!.body)

        lightSourceSendEventDelay.reset()
        lightSourceKeys.addAll(
            spawnProps.get("${ConstKeys.LIGHT}_${ConstKeys.SOURCE}_${ConstKeys.KEYS}") as ObjectSet<Int>
        )

        standByBlinkTimer.reset()
        standByBlink = false
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy(): hashcode=${hashCode()}")
        super.onDestroy()

        activePieces.clear()
        lightSourceKeys.clear()
    }

    internal fun isPieceActive(row: Int, column: Int) = activePieces.contains(row pairTo column)

    internal fun setStateOfPiece(row: Int, column: Int, state: ElecDevilBodyPieceState) {
        val key = row pairTo column

        when (state) {
            ElecDevilBodyPieceState.INACTIVE -> {
                standByPieces.remove(key)
                activePieces.remove(key)
            }

            ElecDevilBodyPieceState.STANDBY -> {
                standByPieces.add(key)
                activePieces.remove(key)
            }

            ElecDevilBodyPieceState.ACTIVE -> {
                standByPieces.remove(key)
                activePieces.add(key)
            }
        }
    }

    internal fun setStateOfAllPieces(state: ElecDevilBodyPieceState) =
        ElecDevilConstants.forEachCell { row, column -> setStateOfPiece(row, column, state) }

    private fun lightUp(row: Int, column: Int, bright: Boolean) {
        LightSourceUtils.sendLightSourceEvent(
            game,
            lightSourceKeys,
            owner!!.getPositionOf(row, column).add(
                ElecDevilConstants.PIECE_WIDTH * ConstVals.PPM / 2f,
                ElecDevilConstants.PIECE_HEIGHT * ConstVals.PPM / 2f
            ),
            if (bright) BRIGHT_SOURCE_RADIANCE else DIM_SOURCE_RADIANCE,
            if (bright) BRIGHT_SOURCE_RADIUS else DIM_SOURCE_RADIUS
        )
    }

    private fun defineUpdatablesComponent() = UpdatablesComponent({ delta ->
        standByBlinkTimer.update(delta)
        if (standByBlinkTimer.isFinished()) {
            standByBlink = !standByBlink
            standByBlinkTimer.reset()
        }

        when {
            on -> {
                lightSourceSendEventDelay.update(delta)

                if (lightSourceSendEventDelay.isFinished()) {
                    if (standByBlink) standByPieces.forEach { (row, column) -> lightUp(row, column, false) }

                    activePieces.forEach { (row, column) -> lightUp(row, column, true) }

                    lightSourceSendEventDelay.reset()
                }
            }

            else -> lightSourceSendEventDelay.setToEnd()
        }
    })

    private fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)

        val debugShapes = Array<() -> IDrawableShape?>()

        ElecDevilConstants.forEachCell { row, column ->
            val damager = GameRectangle().setSize(
                ElecDevilConstants.PIECE_WIDTH * ConstVals.PPM,
                ElecDevilConstants.PIECE_HEIGHT * ConstVals.PPM
            )

            val damagerFixture = Fixture(body, FixtureType.DAMAGER, damager)
            damagerFixture.attachedToBody = false
            body.addFixture(damagerFixture)

            val key = ElecDevilConstants.getRowColumnKey(row, column)
            body.preProcess.put(key) {
                damager.setPosition(owner!!.getPositionOf(row, column))

                damagerFixture.setActive(on && isPieceActive(row, column))
                damagerFixture.drawingColor = if (damagerFixture.isActive()) Color.RED else Color.GRAY
            }

            debugShapes.add { damagerFixture }
        }

        debugShapes.add { body.getBounds() }
        body.preProcess.put(ConstKeys.BODY) {
            body.set(owner!!.body)
            body.drawingColor = if (on) Color.GREEN else Color.GRAY
        }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(this, body)
    }

    private fun defineSpritesComponent() = SpritesComponentBuilder()
        .also { builder ->
            ElecDevilConstants.forEachCell { row, column ->
                val key = ElecDevilConstants.getRowColumnKey(row, column)

                val sprite = GameSprite(DrawingPriority(DrawingSection.PLAYGROUND, 3))
                sprite.setSize(
                    ElecDevilConstants.BODY_SPRITE_WIDTH * ConstVals.PPM,
                    ElecDevilConstants.BODY_SPRITE_HEIGHT * ConstVals.PPM
                )

                builder.sprite(key, sprite)

                builder.updatable { _, sprite ->
                    val visible = on && (activePieces.contains(row pairTo column) ||
                        (standByPieces.contains(row pairTo column) && standByBlink))
                    sprite.hidden = !visible

                    sprite.setAlpha(if (activePieces.contains(row pairTo column)) 1f else STAND_BY_ALPHA)

                    sprite.setFlip(isFacing(Facing.LEFT), false)

                    val position = Position.BOTTOM_CENTER
                    sprite.setPosition(body.getPositionPoint(position), position)

                    val offsetX = when {
                        isFacing(Facing.LEFT) -> BODY_SPRITE_FACING_LEFT_OFFSET_X
                        else -> BODY_SPRITE_FACING_RIGHT_OFFSET_X
                    }
                    sprite.translateX(offsetX * ConstVals.PPM)
                }
            }
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .also { builder ->
            ElecDevilConstants.forEachCell { row, column ->
                val rowColumnKey = ElecDevilConstants.getRowColumnKey(row, column)

                val activeRegions = colors.map { color -> regions["${color}/${rowColumnKey}"] }.toGdxArray()
                val activeAnimation = Animation(activeRegions, 0.1f, true)

                val standByRegion = regions["${ConstKeys.WHITE}/${rowColumnKey}"]
                val standByAnimation = Animation(standByRegion)

                builder.key(rowColumnKey)
                    .animator(
                        AnimatorBuilder()
                            .setKeySupplier {
                                when {
                                    standByPieces.contains(row pairTo column) -> ConstKeys.STAND_BY
                                    activePieces.contains(row pairTo column) -> ConstKeys.ACTIVE
                                    else -> null
                                }
                            }
                            .applyToAnimations { animations ->
                                animations.put(ConstKeys.STAND_BY, standByAnimation)
                                animations.put(ConstKeys.ACTIVE, activeAnimation)
                            }
                            .build()
                    )
            }
        }
        .build()

    override fun getType() = EntityType.OTHER

    override fun getTag() = TAG

    override fun toString() =
        "{ ElecDevilBodyPieces:[" + when {
            spawned ->
                "\n\t\tfacing=$facing," +
                    "\n\t\tbounds=${body.getBounds()}," +
                    "\n\t\tactivePieces=" + when {
                    activePieces.isEmpty -> "[]"
                    else -> "\n\t\t${activePieces.toString("\n\t\t")}"
                } + "\n\t] }"

            else -> "not spawned] }"
        }
}
