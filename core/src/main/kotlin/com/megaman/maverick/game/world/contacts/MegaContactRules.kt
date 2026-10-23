package com.megaman.maverick.game.world.contacts

import com.maverick.game.engine.common.enums.ProcessState.BEGIN
import com.maverick.game.engine.common.enums.ProcessState.CONTINUE
import com.maverick.game.engine.common.enums.ProcessState.END
import com.maverick.game.engine.world.body.FixtureTypeSet
import com.maverick.game.engine.world.body.IFixture
import com.maverick.game.engine.world.contacts.ContactRuleTable
import com.maverick.game.engine.world.contacts.ContactRulesBuilder
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.world.body.FixtureType.*
import com.megaman.maverick.game.world.body.FixtureType
import com.megaman.maverick.game.world.contacts.rules.*

fun megaContactRules(game: MegamanMaverickGame, debugFilter: (IFixture, IFixture) -> Boolean): ContactRuleTable =
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
        pair(ContactRuleIds.FEET_SAND, FEET, SAND, FeetSandRule(game), BEGIN, CONTINUE, END)
        pair(ContactRuleIds.FEET_SNOW, FEET, SNOW, FeetSnowRule, BEGIN, CONTINUE, END)
        sets(ContactRuleIds.BOUNCER, setOf(BOUNCER), setOf(BODY, FEET, HEAD, SIDE), BouncerRule, BEGIN)
        pair(ContactRuleIds.HEAD_BLOCK, HEAD, BLOCK, HeadBlockRule, BEGIN, CONTINUE, END)
        pair(ContactRuleIds.HEAD_FEET, HEAD, FEET, HitByFeetRule, BEGIN, CONTINUE, END)
        pair(ContactRuleIds.WATER, WATER_LISTENER, WATER, WaterRule, BEGIN, CONTINUE, END)
        pair(ContactRuleIds.HEAD_LADDER, HEAD, LADDER, HeadLadderRule, BEGIN, CONTINUE, END)
        pair(ContactRuleIds.FEET_LADDER, FEET, LADDER, FeetLadderRule, BEGIN, CONTINUE, END)
        pair(ContactRuleIds.BODY_FORCE, BODY, FORCE, BodyForceRule, BEGIN, CONTINUE, END)
        pair(ContactRuleIds.BODY_GRAVITY_CHANGE, BODY, GRAVITY_CHANGE, BodyGravityChangeRule, BEGIN, CONTINUE)
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
