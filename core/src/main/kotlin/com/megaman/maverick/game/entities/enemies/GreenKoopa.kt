package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.mega.game.engine.animations.AnimationsComponentBuilder
import com.mega.game.engine.animations.AnimatorBuilder
import com.mega.game.engine.common.GameLogger
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.enums.ProcessState
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectSetOf
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.objects.props
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamageable
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponentBuilder
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.drawables.sprites.setSize
import com.mega.game.engine.entities.contracts.IBodyEntity
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.animations.AnimationDef
import com.megaman.maverick.game.assets.SoundAsset
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.entities.MegaGameEntities
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.entities.contracts.MegaGameEntity
import com.megaman.maverick.game.entities.contracts.megaman
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.entities.projectiles.Fireball
import com.megaman.maverick.game.entities.utils.DrawableShapesComponentBuilder
import com.megaman.maverick.game.utils.AnimationUtils
import com.megaman.maverick.game.utils.extensions.getBoundingRectangle
import com.megaman.maverick.game.utils.extensions.getPositionPoint
import com.megaman.maverick.game.utils.misc.FacingUtils
import com.megaman.maverick.game.world.body.*

class GreenKoopa(game: MegamanMaverickGame) : AbstractEnemy(game), IFaceable {

    companion object {
        const val TAG = "GreenKoopa"

        private const val GRAVITY = -0.15f
        private const val GROUND_GRAVITY = -0.01f

        private const val BUMPED_NO_DMG_DUR = 0.1f

        private const val WALK_SPEED = 3f

        private val SHELL_TAGS = objectSetOf(GreenKoopaShell.TAG, RedKoopaShell.TAG)

        private const val KNOCKED_TO_DEATH_BUMP = 5f

        private val animDefs = orderedMapOf(
            "walk" pairTo AnimationDef(2, 1, 0.1f, true)
        )
        private val regions = ObjectMap<String, TextureRegion>()
    }

    override lateinit var facing: Facing

    private var knockedToDeath = false

    private val bumpedNoDmgTimer = Timer(BUMPED_NO_DMG_DUR)

    private val tempOut = ObjectSet<MegaGameEntity>()

    override fun init() {
        GameLogger.debug(TAG, "init()")
        super.init()
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.SMB3_ENEMIES.source)
            AnimationUtils.loadRegions(TAG, atlas, animDefs.keys(), regions)
        }
        addComponent(defineAnimationsComponent())
    }

    override fun canSpawn(spawnProps: Properties): Boolean {
        if (!super.canSpawn(spawnProps)) return false

        val thisId = spawnProps.get(ConstKeys.ID, Int::class)!!

        val shells = MegaGameEntities.getOfTag(GreenKoopaShell.TAG)
        return !shells.any { (it as GreenKoopaShell).koopaId == thisId }
    }

    override fun onSpawn(spawnProps: Properties) {
        GameLogger.debug(TAG, "onSpawn(): spawnProps=$spawnProps")
        super.onSpawn(spawnProps)

        val position =
            spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getPositionPoint(Position.BOTTOM_CENTER)
        body.setBottomCenterToPoint(position)
        body.forEachFixture { it.setActive(true) }

        facing = if (megaman.body.getBounds().getX() < position.x) Facing.LEFT else Facing.RIGHT
        knockedToDeath = false
        bumpedNoDmgTimer.setToEnd()
    }

    override fun canBeDamagedBy(damager: IDamager) = damager is Fireball || super.canBeDamagedBy(damager)

    override fun takeDamageFrom(damager: IDamager): Boolean {
        if (damager is Fireball) {
            val damagerBounds = damager.body.fixtures
                .get(FixtureType.DAMAGER)
                .first()
                .getShape()
                .getBoundingRectangle()

            getKnockedToDeath(damagerBounds)

            return false
        }

        return super.takeDamageFrom(damager)
    }

    override fun onDestroy() {
        GameLogger.debug(TAG, "onDestroy()")
        super.onDestroy()
        tempOut.clear()
    }

    override fun onHealthDepleted() {
        GameLogger.debug(TAG, "onHealthDepleted()")
        super.onHealthDepleted()
        spawnFloatingPoints()
    }

    override fun canDamage(damageable: IDamageable) = bumpedNoDmgTimer.isFinished() && super.canDamage(damageable)

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            bumpedNoDmgTimer.update(delta)

            tempOut.clear()

            if (knockedToDeath) body.physics.velocity.x = 0f
            else {
                val shells = MegaGameEntities.getOfTags(tempOut, SHELL_TAGS)
                for (shell in shells) {
                    shell as IBodyEntity
                    if (body.getBounds().overlaps(shell.body.getBounds())) {
                        getKnockedToDeath(shell.body.getBounds())
                        return@add
                    }
                }

                if (FacingUtils.isFacingBlock(this)) swapFacing()

                val speed = WALK_SPEED * ConstVals.PPM * facing.value
                body.physics.velocity.x = speed
            }
        }
    }

    private fun spawnShell() {
        val shell = MegaEntityFactory.fetch(GreenKoopaShell::class)!!
        shell.spawn(
            props(
                "${ConstKeys.PARENT}_${ConstKeys.ID}" pairTo id,
                ConstKeys.POSITION pairTo body.getPositionPoint(Position.BOTTOM_CENTER)
            )
        )
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(ConstVals.PPM.toFloat(), 1.5f * ConstVals.PPM)
        body.physics.applyFrictionX = false
        body.physics.applyFrictionY = false
        body.drawingColor = Color.RED

        val drawableShapesComponent = DrawableShapesComponentBuilder().addDebug { body.getBounds() }

        val damagerFixture =
            Fixture(body, FixtureType.DAMAGER, GameRectangle().setSize(0.85f * ConstVals.PPM, 1.25f * ConstVals.PPM))
        body.addFixture(damagerFixture)
        drawableShapesComponent.addDebug { damagerFixture }

        val headFixture =
            Fixture(body, FixtureType.HEAD, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        headFixture.offsetFromBodyAttachment.y = body.getHeight() / 2f
        headFixture.setHitByFeetReceiver(ProcessState.BEGIN) { feet, _ ->
            if (feet.getEntity() == megaman && megaman.body.physics.velocity.y <= 0f && !knockedToDeath) {
                destroy()

                spawnShell()
                spawnWhackForOverlap(headFixture.getShape(), feet.getShape())
                spawnFloatingPoints()

                megaman.body.physics.velocity.y = MegamanValues.JUMP_VEL * ConstVals.PPM / 2f
                playSoundNow(SoundAsset.SWIM_SOUND, false)

                bumpedNoDmgTimer.reset()
            }
        }
        body.addFixture(headFixture)
        headFixture.drawingColor = Color.ORANGE
        drawableShapesComponent.addDebug { headFixture }

        val feetFixture =
            Fixture(body, FixtureType.FEET, GameRectangle().setSize(ConstVals.PPM.toFloat(), 0.1f * ConstVals.PPM))
        feetFixture.offsetFromBodyAttachment.y = -body.getHeight() / 2f
        body.addFixture(feetFixture)
        feetFixture.drawingColor = Color.GREEN
        drawableShapesComponent.addDebug { feetFixture }

        val leftFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        leftFixture.offsetFromBodyAttachment.x = -body.getWidth() / 2f
        body.addFixture(leftFixture)
        leftFixture.drawingColor = Color.YELLOW
        drawableShapesComponent.addDebug { leftFixture }

        val rightFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        rightFixture.offsetFromBodyAttachment.x = body.getWidth() / 2f
        body.addFixture(rightFixture)
        rightFixture.drawingColor = Color.YELLOW
        drawableShapesComponent.addDebug { rightFixture }

        body.preProcess.put(ConstKeys.DEATH) {
            body.physics.collisionOn = !knockedToDeath
            if (knockedToDeath) body.forEachFixture { it.setActive(false) }
        }

        body.preProcess.put(ConstKeys.GRAVITY) {
            body.preProcess.put(ConstKeys.GRAVITY) {
                val gravity = when {
                    !body.isSensing(BodySense.FEET_ON_GROUND) || knockedToDeath -> GRAVITY
                    else -> GROUND_GRAVITY
                }
                body.physics.gravity.y = gravity * ConstVals.PPM
            }
        }

        addComponent(drawableShapesComponent.build())

        return BodyComponentCreator.create(
            this,
            body,
            BodyFixtureDef.of(FixtureType.BODY, FixtureType.DAMAGEABLE),
        )
    }

    override fun defineSpritesComponent() = SpritesComponentBuilder()
        .sprite(TAG, GameSprite().also { it.setSize(2f * ConstVals.PPM) })
        .preProcess { _, sprite ->
            val position = Position.BOTTOM_CENTER
            sprite.setPosition(body.getPositionPoint(position), position)
            sprite.setFlip(isFacing(Facing.RIGHT), knockedToDeath)
            sprite.hidden = damageBlink
        }
        .build()

    private fun defineAnimationsComponent() = AnimationsComponentBuilder(this)
        .key(TAG)
        .animator(
            AnimatorBuilder()
                .setKeySupplier { "walk" }
                .applyToAnimations { animations ->
                    AnimationUtils.loadAnimationDefs(animDefs, animations, regions)
                }
                .build()
        )
        .build()

    private fun getKnockedToDeath(shellBounds: GameRectangle) {
        knockedToDeath = true
        spawnFloatingPoints()
        spawnWhackForOverlap(body.getBounds(), shellBounds)
        body.physics.velocity.y = KNOCKED_TO_DEATH_BUMP * ConstVals.PPM
    }
}
