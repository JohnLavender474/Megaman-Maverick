package com.maverick.game.engine.world.body

internal enum class TestFixtureType : IFixtureType {
    TYPE,
    OTHER_TYPE,
    TYPE1,
    TYPE2,
    TYPE3,
    F,
    F1,
    F2,
    FIXTURE1,
    FIXTURE2,
    FIXTURE3,
    KEEP,
    SKIP,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
    NEG_FIX,
    TEST_FIXTURE;

    override val index get() = ordinal
}
