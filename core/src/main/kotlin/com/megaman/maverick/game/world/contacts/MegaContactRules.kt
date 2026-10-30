package com.megaman.maverick.game.world.contacts

import com.maverick.game.engine.common.enums.ProcessState.BEGIN
import com.maverick.game.engine.common.enums.ProcessState.CONTINUE
import com.maverick.game.engine.common.enums.ProcessState.END
import com.maverick.game.engine.entities.body
import com.maverick.game.engine.entities.direction
import com.maverick.game.engine.players.any
import com.maverick.game.engine.world.body.FixtureTypeSet
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.ContactRuleTable
import com.maverick.game.engine.world.contacts.ContactRulesBuilder
import com.megaman.maverick.game.entities.state.inPlay
import com.megaman.maverick.game.players.MegaPlayers
import com.megaman.maverick.game.players.WorldGravity
import com.megaman.maverick.game.players.asPlayer
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.body.FixtureType.*
import com.megaman.maverick.game.world.contacts.rules.*

fun megaContactRules(
    players: MegaPlayers,
    gravity: WorldGravity,
    debugFilter: (IFixture, IFixture) -> Boolean
): ContactRuleTable =
    ContactRulesBuilder(FixtureTypeSet.of(FixtureType.entries)).apply {
        wildcard(ContactRuleIds.CONSUMER, CONSUMER, ConsumerRule, BEGIN, CONTINUE, END)
        pair(ContactRuleIds.DAMAGE, DAMAGER, DAMAGEABLE, DamageRule(debugFilter), BEGIN, CONTINUE)
        sets(ContactRuleIds.DEATH, setOf(DEATH), setOf(FEET, SIDE, HEAD, BODY), DeathRule, BEGIN, CONTINUE)
        pair(ContactRuleIds.BLOCK_BODY, BLOCK, BODY, BlockBodyRule, BEGIN, CONTINUE, END)
        pair(ContactRuleIds.BODY_BODY, BODY, BODY, BodyBodyRule, BEGIN)
        pair(ContactRuleIds.BODY_FEET, BODY, FEET, HitByFeetRule, BEGIN, CONTINUE, END)
        pair(ContactRuleIds.BODY_EXPLOSION, BODY, EXPLOSION, HitByExplosionRule, BEGIN, CONTINUE)
        pair(ContactRuleIds.BLOCK_EXPLOSION, BLOCK, EXPLOSION, HitByExplosionRule, BEGIN, CONTINUE)
        pair(ContactRuleIds.BODY_SIDE, BODY, SIDE, BodySideRule, BEGIN, CONTINUE, END)
        pair(ContactRuleIds.SIDE_SIDE, SIDE, SIDE, SideSideRule, BEGIN, CONTINUE, END)
        pair(ContactRuleIds.SHIELD_SIDE, SHIELD, SIDE, ShieldSideRule, BEGIN)
        pair(ContactRuleIds.BLOCK_SIDE, BLOCK, SIDE, BlockSideRule, BEGIN, CONTINUE, END)
        sets(ContactRuleIds.GATE, setOf(SIDE, FEET, HEAD), setOf(GATE), GateRule, BEGIN)
        pair(ContactRuleIds.SIDE_ICE, SIDE, ICE, SideIceRule, BEGIN, CONTINUE, END)
        pair(ContactRuleIds.FEET_BLOCK, FEET, BLOCK, FeetBlockRule, BEGIN, CONTINUE, END)
        pair(ContactRuleIds.FEET_ICE, FEET, ICE, FeetIceRule, BEGIN, CONTINUE, END)
        // TODO: sand splashes for every feet fixture while any player is in play, as it did for the one player; whether
        //  the feet's own player should count is a co-op rule for item 047
        pair(ContactRuleIds.FEET_SAND, FEET, SAND, FeetSandRule { players.any { it.inPlay } }, BEGIN, CONTINUE, END)
        pair(ContactRuleIds.FEET_SNOW, FEET, SNOW, FeetSnowRule, BEGIN, CONTINUE, END)
        sets(ContactRuleIds.BOUNCER, setOf(BOUNCER), setOf(BODY, FEET, HEAD, SIDE), BouncerRule, BEGIN)
        pair(ContactRuleIds.HEAD_BLOCK, HEAD, BLOCK, HeadBlockRule, BEGIN, CONTINUE, END)
        pair(ContactRuleIds.HEAD_FEET, HEAD, FEET, HitByFeetRule, BEGIN, CONTINUE, END)
        pair(ContactRuleIds.WATER, WATER_LISTENER, WATER, WaterRule, BEGIN, CONTINUE, END)
        pair(ContactRuleIds.HEAD_LADDER, HEAD, LADDER, HeadLadderRule, BEGIN, CONTINUE, END)
        pair(ContactRuleIds.FEET_LADDER, FEET, LADDER, FeetLadderRule, BEGIN, CONTINUE, END)
        pair(ContactRuleIds.BODY_FORCE, BODY, FORCE, BodyForceRule, BEGIN, CONTINUE, END)
        // TODO: a player in a gravity change area sets the world gravity, and the player's direction setter rotates the
        //  camera, so gravity and camera rotation are forced into sync; camera rotation should have its own mechanism
        //  so that gravity and camera can be configured separately per entity and level (backlog candidate in the
        //  029d branch TODO.md)
        pair(
            ContactRuleIds.BODY_GRAVITY_CHANGE, BODY, GRAVITY_CHANGE,
            BodyGravityChangeRule { entity, direction ->
                if (entity.asPlayer != null) gravity.set(direction) else entity.direction = direction
            },
            BEGIN, CONTINUE
        )
        pair(ContactRuleIds.BLOCK_GRAVITY_CHANGE, BLOCK, GRAVITY_CHANGE, BlockGravityChangeRule, BEGIN, CONTINUE)
        pair(ContactRuleIds.BODY_PLAYER, BODY, PLAYER, BodyPlayerRule, BEGIN)
        sets(
            ContactRuleIds.PROJECTILE,
            setOf(PROJECTILE),
            setOf(BLOCK, BODY, SHIELD, WATER, SAND, EXPLOSION),
            ProjectileRule,
            BEGIN
        )
        pair(ContactRuleIds.PROJECTILE, PROJECTILE, PROJECTILE, ProjectileRule, BEGIN)
        pair(ContactRuleIds.PLAYER_ITEM, PLAYER, ITEM, PlayerItemRule, BEGIN)
        pair(ContactRuleIds.FEET_CART, FEET, CART, FeetCartRule, BEGIN, END)
        pair(ContactRuleIds.TELEPORTER, TELEPORTER_LISTENER, TELEPORTER, TeleporterRule, BEGIN, CONTINUE)
        pair(ContactRuleIds.LASER_BLOCK, LASER, BLOCK, LaserBlockRule(), BEGIN, CONTINUE)
        pair(ContactRuleIds.LASER_BODY, LASER, BODY, LaserBodyRule, BEGIN, CONTINUE)
        pair(ContactRuleIds.LASER_SHIELD, LASER, SHIELD, LaserShieldRule(), BEGIN, CONTINUE)
    }.build()
