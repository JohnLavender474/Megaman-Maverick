package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.animations.Animation
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.*
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.shapes.DrawableShapesComponent
import com.mega.game.engine.drawables.shapes.IDrawableShape
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.IGameEntity
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.entities.contracts.IParentEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.*
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.damage.IDamageNegotiator
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.blocks.Block
import com.megaman.maverick.game.entities.blocks.CrumblingBlockEventListener
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.overlapsGameCamera
import com.megaman.maverick.game.entities.explosions.SpreadExplosion
import com.megaman.maverick.game.utils.GameObjectPools
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.world.body.*
import kotlin.reflect.KClass

class TorikoPlundge(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IParentEntity {

    companion object {
        const val TAG = "TorikoPlundge"

        private const val PLUNGE_1_DUR = 0.25f
        private const val PLUNGE_2_DUR = 0.5f
        private const val PLUNGE_3_DUR = 0.25f
        private const val PLUNGE_SPEED = 3f

        private val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
            // TODO
        )

        private val animDefs = orderedMapOf(
            "idle" pairTo AnimationDef(2, 1, gdxArrayOf(1f, 0.15f), true),
            "plunge1" pairTo AnimationDef(2, 2, 0.05f, false),
            "plunge2" pairTo AnimationDef(2, 1, 0.1f, true),
            "plunge3" pairTo AnimationDef(2, 1, 0.05f, true)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override val damageNegotiator = object : IDamageNegotiator {

        override fun get(damager: IDamager) = damageNegotiations[damager::class]?.get(damager) ?: 0
    }
    override var children = Array<IGameEntity>()

    private val plungeTimer = Timer(PLUNGE_1_DUR + PLUNGE_2_DUR + PLUNGE_3_DUR)
    private var plunging = false

    private val crumblingBlockIds = ObjectSet<Int>()

    override fun init() {
        GameLogger.debug(TAG, "init()")

        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            animDefs.keys().forEach { key -> regions.put(key, atlas.findRegion("$TAG/$key")) }
        }

        super.init()

        addComponent(defineAnimationsComponent())
    }

    override fun canSpawn(spawnProps: Properties): Boolean {
        val canSpawn = spawnProps
            .collectValues<RectangleMapObject> { key, value -> key.toString().contains(ConstKeys.BLOCK) }
            .map { obj -> obj.properties.get(ConstKeys.ID, Int::class.java) }
            .any { id -> !CrumblingBlockEventListener.get(id) }

        GameLogger.debug(TAG, "canSpawn(): canSpawn=$canSpawn, spawnProps=$spawnProps")

        return canSpawn
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")

        super.onSpawn(spawnProps)

        val position = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(position)

        plungeTimer.reset()
        plunging = false

        val block1 = MegaEntityFactory.fetch(Block::class)!!
        block1.spawn(
            props(
                ConstKeys.BOUNDS pairTo GameObjectPools.fetch(GameRectangle::class)
                    .setSize(ConstVals.PPM.toFloat(), 0.25f * ConstVals.PPM)
                    .setTopCenterToPoint(position)
                    .translate(0f, 1.85f * ConstVals.PPM),
                ConstKeys.FIXTURE_LABELS pairTo objectSetOf(
                    FixtureLabel.NO_PROJECTILE_COLLISION,
                    FixtureLabel.NO_SIDE_TOUCHIE,
                    FixtureLabel.NO_BODY_TOUCHIE
                ),
                ConstKeys.BLOCK_FILTERS pairTo objectSetOf(TAG),
                "${ConstKeys.FEET}_${ConstKeys.SOUND}" pairTo false
            )
        )
        children.add(block1)

        val block2 = MegaEntityFactory.fetch(Block::class)!!
        block2.spawn(
            props(
                ConstKeys.BOUNDS pairTo GameObjectPools.fetch(GameRectangle::class).set(body),
                ConstKeys.FIXTURE_LABELS pairTo objectSetOf(
                    FixtureLabel.NO_PROJECTILE_COLLISION,
                    FixtureLabel.NO_SIDE_TOUCHIE,
                    FixtureLabel.NO_BODY_TOUCHIE
                ),
                ConstKeys.BLOCK_FILTERS pairTo objectSetOf(TAG),
                "${ConstKeys.FEET}_${ConstKeys.SOUND}" pairTo false
            )
        )
        children.add(block2)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")

        super.onDestroy()

        children.forEach { (it as MegaGameEntity).destroy() }
        children.clear()
    }

    private fun hitByFeet(fixture: IFixture) {
        GameLogger.debug(TAG, "hitByFeet(): fixture=$fixture")

        val entity = fixture.getEntity() as IBodyEntity

        if (entity.body.physics.velocity.y > 0f) {
            GameLogger.debug(TAG, "hitByFeet(): velocity > 0, do nothing")
            return
        }

        if (!plunging) {
            plunging = true
            onStartPlunge()
        }
    }

    private fun onStartPlunge() {
        val block = children[0] as Block
        block.body.physics.velocity.y = -PLUNGE_SPEED * ConstVals.PPM
    }

    private fun onEndPlunge() {
        GameLogger.debug(TAG, "onEndPlunge()")

        val explosion = MegaEntityFactory.fetch(SpreadExplosion::class)!!
        explosion.spawn(
            props(
                ConstKeys.OWNER pairTo this,
                ConstKeys.COLOR pairTo SpreadExplosion.SpreadExplosionColor.DEFAULT,
                ConstKeys.POSITION pairTo body.getPositionPoint(Position.BOTTOM_CENTER)
            )
        )

        if (overlapsGameCamera()) playSoundNow(SoundAsset.EXPLOSION_2_SOUND, false)
    }

    override fun onHealthDepleted() {
        GameLogger.debug(TAG, "onHealthDepleted()")

        super.onHealthDepleted()

        onEndPlunge()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            if (plunging) {
                val block = children[0] as Block
                if (block.body.getY() <= body.getMaxY()) {
                    block.body.physics.velocity.y = 0f
                    block.body.setY(body.getMaxY())
                }

                plungeTimer.update(delta)
                if (plungeTimer.isFinished()) {
                    destroy()
                    onEndPlunge()
                }
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.ABSTRACT)
        body.setSize(ConstVals.PPM.toFloat())
        body.drawingColor = Color.GRAY

        val debugShapes = Array<() -> IDrawableShape?>()
        debugShapes.add { body.getBounds() }

        val headFixture = Fixture(body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f))
        headFixture.offsetFromBodyAttachment.y = (body.getHeight() / 2f) + 0.75f * ConstVals.PPM
        headFixture.setHitByFeetReceiver(ProcessState.BEGIN) { fixture, _ -> hitByFeet(fixture) }
        headFixture.setHitByFeetReceiver(ProcessState.CONTINUE) { fixture, _ -> hitByFeet(fixture) }
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.BLUE
        debugShapes.add { headFixture }

        addComponent(DrawableShapesComponent(debugShapeSuppliers = debugShapes, debug = true))

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.SHIELD, FixtureType.DAMAGER, FixtureType.DAMAGEABLE)
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { sprite -> sprite.setSize(2f * ConstVals.PPM) })
        .updatable { _, sprite ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)

            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier {
                    when {
                        plunging -> when {
                            plungeTimer.time <= PLUNGE_1_DUR -> "plunge1"
                            plungeTimer.time <= PLUNGE_2_DUR -> "plunge2"
                            else -> "plunge3"
                        }

                        else -> "idle"
                    }
                }
                .applyToAnimations { animations ->
                    animDefs.forEach { entry ->
                        val key = entry.key
                        val (rows, cols, durations, loop) = entry.value
                        try {
                            val animation = Animation(regions[key], rows, cols, durations, loop)
                            animations.put(key, animation)
                        } catch (e: Exception) {
                            throw IllegalStateException("Failed to create animation for key=$key", e)
                        }
                    }
                }
                .build()
        )
        .build()

    override fun getTag() = TAG
}
