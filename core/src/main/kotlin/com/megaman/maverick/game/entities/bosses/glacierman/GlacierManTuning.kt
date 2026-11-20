package com.megaman.maverick.game.entities.bosses.glacierman

class GlacierManTuning(
    val standDurMin: Float,
    val standDurMax: Float,
    val standShotMin: Float,
    val standShotMax: Float,
    val duckDur: Float,
    val duckShotMin: Float,
    val duckShotMax: Float,
    val sledDur: Float,
    val sledShotJitter: Float,
    val stopDur: Float,
    val sledTellDur: Float,
    val jumpTellDur: Float,
    val iceBlastTellDur: Float,
    val iceBlastWeight: Float,
    val duckWeight: Float,
    val sledWeight: Float,
    val jumpWeight: Float,
    val lastMoveScale: Float,
    val nearJumpScale: Float,
    val aboveJumpScale: Float,
    val farIceBlastScale: Float,
    val sledJumpChainChance: Float,
    val duckSledChainChance: Float,
    val iceBlastCount: Int,
    val throwFrameDur: Float,
    val throwRest: Float,
    val chunkChance: Float,
    val cubeLeadTime: Float,
    val snowballAimChance: Float,
    val shootUpChance: Float,
    val icicleRainWeight: Float,
    val icicleRainWaves: Int,
    val lowHealthRainWaves: Int,
    val icicleCount: Int,
    val icicleStagger: Float,
    val icicleStillDur: Float,
    val icicleShakeDur: Float,
    val icicleLeadTime: Float,
    val icicleWaveRest: Float,
    val sledRamWeight: Float,
    val sledRamSpeed: Float,
    val sledRamTellDur: Float,
    val phaseStartDur: Float
) {

    companion object {
        const val ICE_BLAST_WEIGHT = 4f
        const val DUCK_WEIGHT = 3f
        const val SLED_WEIGHT = 3f
        const val JUMP_WEIGHT = 2f
        const val LAST_MOVE_SCALE = 0.25f
        const val NEAR_JUMP_SCALE = 2f
        const val ABOVE_JUMP_SCALE = 2f
        const val FAR_ICE_BLAST_SCALE = 1.5f
        const val SLED_DUR = 1.5f
        const val SLED_SHOT_JITTER = 0.1f
        const val CHUNK_CHANCE = 0.5f
        const val CUBE_LEAD_TIME = 0.6f
        const val SHOOT_UP_CHANCE = 0.4f
        const val ICICLE_RAIN_WAVES = 1
        const val LOW_HEALTH_RAIN_WAVES = 2
        const val ICICLE_LEAD_TIME = 0.8f

        val NORMAL = GlacierManTuning(
            standDurMin = 0.8f, standDurMax = 1.2f, standShotMin = 0.4f, standShotMax = 0.6f,
            duckDur = 0.75f, duckShotMin = 0.2f, duckShotMax = 0.3f,
            sledDur = SLED_DUR, sledShotJitter = SLED_SHOT_JITTER, stopDur = 0.8f,
            sledTellDur = 0.35f, jumpTellDur = 0.3f, iceBlastTellDur = 0.4f,
            iceBlastWeight = ICE_BLAST_WEIGHT, duckWeight = DUCK_WEIGHT, sledWeight = SLED_WEIGHT,
            jumpWeight = JUMP_WEIGHT, lastMoveScale = LAST_MOVE_SCALE, nearJumpScale = NEAR_JUMP_SCALE,
            aboveJumpScale = ABOVE_JUMP_SCALE, farIceBlastScale = FAR_ICE_BLAST_SCALE,
            sledJumpChainChance = 0.4f, duckSledChainChance = 0.5f,
            iceBlastCount = 6, throwFrameDur = 0.1f, throwRest = 0.2f, chunkChance = CHUNK_CHANCE,
            cubeLeadTime = CUBE_LEAD_TIME, snowballAimChance = 0.3f, shootUpChance = SHOOT_UP_CHANCE,
            icicleRainWeight = 0f, icicleRainWaves = ICICLE_RAIN_WAVES, lowHealthRainWaves = LOW_HEALTH_RAIN_WAVES,
            icicleCount = 6, icicleStagger = 0.12f, icicleStillDur = 0.35f, icicleShakeDur = 0.35f,
            icicleLeadTime = ICICLE_LEAD_TIME, icicleWaveRest = 1.2f,
            sledRamWeight = 0f, sledRamSpeed = 12f, sledRamTellDur = 0.45f, phaseStartDur = 0.8f
        )
        val NORMAL_PHASE_2 = GlacierManTuning(
            standDurMin = 0.55f, standDurMax = 0.85f, standShotMin = 0.3f, standShotMax = 0.45f,
            duckDur = 0.6f, duckShotMin = 0.15f, duckShotMax = 0.25f,
            sledDur = SLED_DUR, sledShotJitter = SLED_SHOT_JITTER, stopDur = 0.6f,
            sledTellDur = 0.25f, jumpTellDur = 0.2f, iceBlastTellDur = 0.3f,
            iceBlastWeight = ICE_BLAST_WEIGHT, duckWeight = DUCK_WEIGHT, sledWeight = SLED_WEIGHT,
            jumpWeight = JUMP_WEIGHT, lastMoveScale = LAST_MOVE_SCALE, nearJumpScale = NEAR_JUMP_SCALE,
            aboveJumpScale = ABOVE_JUMP_SCALE, farIceBlastScale = FAR_ICE_BLAST_SCALE,
            sledJumpChainChance = 0.5f, duckSledChainChance = 0.6f,
            iceBlastCount = 6, throwFrameDur = 0.09f, throwRest = 0.15f, chunkChance = CHUNK_CHANCE,
            cubeLeadTime = CUBE_LEAD_TIME, snowballAimChance = 0.5f, shootUpChance = SHOOT_UP_CHANCE,
            icicleRainWeight = 3f, icicleRainWaves = ICICLE_RAIN_WAVES, lowHealthRainWaves = LOW_HEALTH_RAIN_WAVES,
            icicleCount = 6, icicleStagger = 0.12f, icicleStillDur = 0.35f, icicleShakeDur = 0.35f,
            icicleLeadTime = ICICLE_LEAD_TIME, icicleWaveRest = 1.2f,
            sledRamWeight = 2.5f, sledRamSpeed = 12f, sledRamTellDur = 0.45f, phaseStartDur = 0.8f
        )
        val HARD = GlacierManTuning(
            standDurMin = 0.7f, standDurMax = 1.05f, standShotMin = 0.35f, standShotMax = 0.5f,
            duckDur = 0.65f, duckShotMin = 0.2f, duckShotMax = 0.3f,
            sledDur = SLED_DUR, sledShotJitter = SLED_SHOT_JITTER, stopDur = 0.7f,
            sledTellDur = 0.3f, jumpTellDur = 0.25f, iceBlastTellDur = 0.35f,
            iceBlastWeight = ICE_BLAST_WEIGHT, duckWeight = DUCK_WEIGHT, sledWeight = SLED_WEIGHT,
            jumpWeight = JUMP_WEIGHT, lastMoveScale = LAST_MOVE_SCALE, nearJumpScale = NEAR_JUMP_SCALE,
            aboveJumpScale = ABOVE_JUMP_SCALE, farIceBlastScale = FAR_ICE_BLAST_SCALE,
            sledJumpChainChance = 0.5f, duckSledChainChance = 0.5f,
            iceBlastCount = 12, throwFrameDur = 0.075f, throwRest = 0f, chunkChance = CHUNK_CHANCE,
            cubeLeadTime = CUBE_LEAD_TIME, snowballAimChance = 0.4f, shootUpChance = SHOOT_UP_CHANCE,
            icicleRainWeight = 0f, icicleRainWaves = ICICLE_RAIN_WAVES, lowHealthRainWaves = LOW_HEALTH_RAIN_WAVES,
            icicleCount = 8, icicleStagger = 0.08f, icicleStillDur = 0.25f, icicleShakeDur = 0.3f,
            icicleLeadTime = ICICLE_LEAD_TIME, icicleWaveRest = 1f,
            sledRamWeight = 0f, sledRamSpeed = 14f, sledRamTellDur = 0.35f, phaseStartDur = 0.6f
        )
        val HARD_PHASE_2 = GlacierManTuning(
            standDurMin = 0.45f, standDurMax = 0.7f, standShotMin = 0.25f, standShotMax = 0.4f,
            duckDur = 0.5f, duckShotMin = 0.15f, duckShotMax = 0.25f,
            sledDur = SLED_DUR, sledShotJitter = SLED_SHOT_JITTER, stopDur = 0.5f,
            sledTellDur = 0.2f, jumpTellDur = 0.15f, iceBlastTellDur = 0.25f,
            iceBlastWeight = ICE_BLAST_WEIGHT, duckWeight = DUCK_WEIGHT, sledWeight = SLED_WEIGHT,
            jumpWeight = JUMP_WEIGHT, lastMoveScale = LAST_MOVE_SCALE, nearJumpScale = NEAR_JUMP_SCALE,
            aboveJumpScale = ABOVE_JUMP_SCALE, farIceBlastScale = FAR_ICE_BLAST_SCALE,
            sledJumpChainChance = 0.6f, duckSledChainChance = 0.6f,
            iceBlastCount = 12, throwFrameDur = 0.07f, throwRest = 0f, chunkChance = CHUNK_CHANCE,
            cubeLeadTime = CUBE_LEAD_TIME, snowballAimChance = 0.6f, shootUpChance = SHOOT_UP_CHANCE,
            icicleRainWeight = 3.5f, icicleRainWaves = ICICLE_RAIN_WAVES, lowHealthRainWaves = LOW_HEALTH_RAIN_WAVES,
            icicleCount = 8, icicleStagger = 0.08f, icicleStillDur = 0.25f, icicleShakeDur = 0.3f,
            icicleLeadTime = ICICLE_LEAD_TIME, icicleWaveRest = 1f,
            sledRamWeight = 3f, sledRamSpeed = 14f, sledRamTellDur = 0.35f, phaseStartDur = 0.6f
        )

        fun of(hard: Boolean, phase: Int) = when {
            hard && phase >= 2 -> HARD_PHASE_2
            hard -> HARD
            phase >= 2 -> NORMAL_PHASE_2
            else -> NORMAL
        }
    }
}
