package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.enums.Position
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.getRandomBool
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.objects.Properties
import com.mega.game.engine.common.objects.pairTo
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.common.time.Timer
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sprites.GameSprite
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.drawables.sprites.setPosition
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.state.StateMachine
import com.mega.game.engine.state.StateMachineBuilder
import com.mega.game.engine.updatables.UpdatablesComponent
import com.mega.game.engine.world.body.BodyComponent
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.contracts.AbstractBoss
import kotlin.reflect.KClass

class GlacierMan(game: MegamanMaverickGame) : AbstractBoss(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "GlacierMan"
        private const val STAND_DUR = 1f
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class GlacierManState {
        STAND, STOP, JUMP, SLED, BRAKE, DUCK, ICE_BLAST_ATTACK
    }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>(
        // TODO
    )
    override lateinit var facing: Facing

    private val timers = objectMapOf(
        "stand" pairTo Timer(STAND_DUR)
    )
    private lateinit var stateMachine: StateMachine<GlacierManState>
    private var randBool = true

    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.BOSSES_1.source)
            regions.put("stand", atlas.findRegion("$TAG/stand"))
            regions.put("stand_shoot", atlas.findRegion("$TAG/stand_shoot"))
            regions.put("stop", atlas.findRegion("$TAG/stop"))
            regions.put("jump", atlas.findRegion("$TAG/jump"))
            regions.put("fall", atlas.findRegion("$TAG/fall"))
            regions.put("sled", atlas.findRegion("$TAG/sled"))
            regions.put("sled_shoot", atlas.findRegion("$TAG/sled_shoot"))
            regions.put("brake", atlas.findRegion("$TAG/brake"))
            regions.put("brake_shoot", atlas.findRegion("$TAG/brake_shoot"))
            regions.put("duck", atlas.findRegion("$TAG/duck"))
            regions.put("duck_shoot", atlas.findRegion("$TAG/duck_shoot"))
            regions.put("ice_blast_attack", atlas.findRegion("$TAG/ice_blast_attack"))
        }
        super.init()
        addComponent(defineAnimationsComponent())
        stateMachine = buildStateMachine()
    }

    override fun onSpawn(spawnProps: Properties) {
        super.onSpawn(spawnProps)
        val spawn = spawnProps.get(ConstKeys.BOUNDS, GameRectangle::class)!!.getBottomCenterPoint()
        body.setBottomCenterToPoint(spawn)
        facing = if (getMegaman().body.x < body.x) Facing.LEFT else Facing.RIGHT
        stateMachine.reset()
        timers.values().forEach { it.reset() }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun defineUpdatablesComponent(updatablesComponent: UpdatablesComponent) {
        super.defineUpdatablesComponent(updatablesComponent)
        updatablesComponent.add { delta ->
            when (stateMachine.getCurrent()) {
                GlacierManState.STAND -> {
                    val timer = timers["stand"]
                    timer.update(delta)
                    if (timer.isFinished()) {
                        timer.reset()
                        randBool = getRandomBool()
                        stateMachine.next()
                    }
                }

                GlacierManState.STOP -> TODO()
                GlacierManState.JUMP -> TODO()
                GlacierManState.SLED -> TODO()
                GlacierManState.BRAKE -> TODO()
                GlacierManState.DUCK -> TODO()
                GlacierManState.ICE_BLAST_ATTACK -> TODO()
            }
        }
    }

    override fun defineBodyComponent(): BodyComponent {
        TODO("Not yet implemented")
    }

    override fun defineSpritesComponent(): SpritesComponent {
        val sprite = GameSprite()
        sprite.setSize(4f * ConstVals.PPM, 3f * ConstVals.PPM)
        val spritesComponent = SpritesComponent(sprite)
        spritesComponent.putUpdateFunction { _, _sprite ->
            _sprite.setPosition(body.getBottomCenterPoint(), Position.BOTTOM_CENTER)
            _sprite.hidden = damageBlink
        }
        return spritesComponent
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        TODO()
    }

    private fun buildStateMachine(): StateMachine<GlacierManState> {
        val builder = StateMachineBuilder<GlacierManState>()
        GlacierManState.entries.forEach { state -> builder.state(state.name, state) }
        builder.initialState(GlacierManState.STAND.name)
            // transitions from STAND state
            .transition(GlacierManState.STAND.name, GlacierManState.DUCK.name) { randBool }
            .transition(GlacierManState.STAND.name, GlacierManState.JUMP.name) { !randBool }
            // transitions from DUCK state

        return builder.build()
    }
}
