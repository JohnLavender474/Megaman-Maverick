package com.megaman.maverick.game.entities.enemies

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.ObjectMap
import com.mega.game.engine.animations.AnimationsComponent
import com.mega.game.engine.common.enums.Facing
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.getTextureAtlas
import com.mega.game.engine.common.extensions.objectMapOf
import com.mega.game.engine.common.interfaces.IFaceable
import com.mega.game.engine.common.shapes.GameRectangle
import com.mega.game.engine.damage.IDamager
import com.mega.game.engine.drawables.sprites.SpritesComponent
import com.mega.game.engine.entities.contracts.IAnimatedEntity
import com.mega.game.engine.state.StateMachine
import com.mega.game.engine.state.StateMachineBuilder
import com.mega.game.engine.world.body.Body
import com.mega.game.engine.world.body.BodyComponent
import com.mega.game.engine.world.body.BodyType
import com.mega.game.engine.world.body.Fixture
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.damage.DamageNegotiation
import com.megaman.maverick.game.entities.contracts.AbstractEnemy
import com.megaman.maverick.game.world.body.BodyComponentCreator
import com.megaman.maverick.game.world.body.FixtureType
import kotlin.reflect.KClass

class BombPenguinBot(game: MegamanMaverickGame) : AbstractEnemy(game), IAnimatedEntity, IFaceable {

    companion object {
        const val TAG = "BombPenguinBot"
        private val regions = ObjectMap<String, TextureRegion>()
    }

    private enum class BPBState { STAND, THROW_BOMB, WADDLE, RISE, FLY, FALL }

    override val damageNegotiations = objectMapOf<KClass<out IDamager>, DamageNegotiation>()
    override lateinit var facing: Facing

    private lateinit var stateMachine: StateMachine<BPBState>


    override fun init() {
        if (regions.isEmpty) {
            val atlas = game.assMan.getTextureAtlas(TextureAsset.ENEMIES_2.source)
            regions.put("fly", atlas.findRegion("$TAG/fly"))
            regions.put("rise", atlas.findRegion("$TAG/rise"))
            regions.put("throw_bomb", atlas.findRegion("$TAG/throw_bomb"))
            regions.put("waddle", atlas.findRegion("$TAG/waddle"))
        }
        stateMachine = buildStateMachine()
        super.init()
        addComponent(defineAnimationsComponent())
    }

    override fun defineBodyComponent(): BodyComponent {
        val body = Body(BodyType.DYNAMIC)
        body.setSize(0.75f * ConstVals.PPM, 1.5f * ConstVals.PPM)

        val bodyFixture = Fixture(body, FixtureType.BODY)
        body.addFixture(bodyFixture)

        val damagerFixture = Fixture(body, FixtureType.DAMAGER)
        body.addFixture(damagerFixture)

        val damageableFixture = Fixture(body, FixtureType.DAMAGEABLE)
        body.addFixture(damageableFixture)

        val leftSideFixture = Fixture(body, FixtureType.SIDE, GameRectangle().setSize(0.1f * ConstVals.PPM))
        leftSideFixture.offsetFromBodyCenter.x = -0.375f * ConstVals.PPM
        leftSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.LEFT)
        body.addFixture(leftSideFixture)

        val rightSideFixture = Fixture(body, FixtureType.FEET, GameRectangle().setSize(0.1f * ConstVals.PPM))
        rightSideFixture.offsetFromBodyCenter.x = 0.375f * ConstVals.PPM
        rightSideFixture.putProperty(ConstKeys.SIDE, ConstKeys.RIGHT)
        body.addFixture(rightSideFixture)

        val fixtureToSizeToBody = gdxArrayOf(bodyFixture, damagerFixture, damageableFixture)
        body.preProcess.put(ConstKeys.DEFAULT) {
            // val bodySize = if (s)
        }

        return BodyComponentCreator.create(this, body)

    }

    override fun defineSpritesComponent(): SpritesComponent {
        TODO("Not yet implemented")
    }

    private fun defineAnimationsComponent(): AnimationsComponent {
        TODO()
    }

    private fun buildStateMachine(): StateMachine<BPBState> {
        val builder = StateMachineBuilder<BPBState>()
            .initialState(BPBState.STAND.name)
            .transition(BPBState.STAND.name, BPBState.WADDLE.name) { true }
            .transition(BPBState.WADDLE.name, BPBState.STAND.name) { true }
        // TODO: set logic// for conditional waddling
        BPBState.values().forEach { builder.state(it.name, it) }
        return builder.build()
    }
}