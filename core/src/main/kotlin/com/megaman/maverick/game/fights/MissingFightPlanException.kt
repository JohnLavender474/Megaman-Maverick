package com.megaman.maverick.game.fights

class MissingFightPlanException(bossName: String) : IllegalStateException(
    "No fight plan for the boss \"$bossName\". Add the boss to FightPlans.find with a plan, " +
        "or change the boss object of its boss room to a boss that has a plan."
)
