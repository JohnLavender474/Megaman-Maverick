package com.megaman.maverick.game

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.MathUtils.random
import com.badlogic.gdx.math.RandomXS128
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.SnapshotArray
import com.maverick.game.engine.common.random.GameRandom
import com.maverick.game.engine.common.random.SeededRandom
import java.security.SecureRandom
import java.util.Collections
import java.util.SplittableRandom
import java.util.concurrent.ThreadLocalRandom
import com.badlogic.gdx.math.MathUtils as MU

class ForbiddenInArraySubclass : Array<Int>() {
    fun mix() = shuffle()
    fun pickOne() = random()
}

enum class FixtureDirection { LEFT, RIGHT }

object RandomRuleFixture {

    fun forbidden(
        gdx: Array<Int>,
        snapshot: SnapshotArray<Int>,
        list: MutableList<Int>,
        kotlinRandom: kotlin.random.Random,
        javaRandom: java.util.Random
    ) {
        MathUtils.random(0f, 1f)
        random(3)
        MathUtils.random.nextInt()
        MathUtils.randomBoolean()
        MU.randomSign()
        java.util.Random(42L)
        RandomXS128(1L)
        SecureRandom()
        SplittableRandom(1L).nextInt(3)
        kotlin.random.Random.nextInt(3)
        kotlin.random.Random(1)
        listOf(1, 2).random()
        FixtureDirection.entries.random()
        with(gdx) { shuffle() }
        list.shuffle(kotlinRandom)
        (0..3).shuffled()
        Math.random()
        Collections.shuffle(list)
        ThreadLocalRandom.current().nextInt(3)
        gdx.random()
        Vector2().setToRandomDirection()
        (0..5).random()
        with(list) { random() }
        snapshot.shuffle()
        javaRandom.nextInt(3)
        kotlinRandom.nextFloat()
        SeededRandom(1L).nextBoolean()
        java.util.random.RandomGeneratorFactory.of<java.util.random.RandomGenerator>("L64X128MixRandom").create()
    }

    fun random() = 4

    fun allowed(random: GameRandom, gdx: Array<Int>) {
        random.nextInt(0, 3)
        random.shuffle(gdx)
        random.pick(gdx)
        random.pickOf(FixtureDirection.LEFT, FixtureDirection.RIGHT)
        random()
    }
}
