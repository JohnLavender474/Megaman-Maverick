package com.megaman.maverick.game.smoketests

import com.badlogic.gdx.graphics.g2d.TextureAtlas

class RecordingTextureAtlas(
    private val atlasName: String,
    data: TextureAtlasData,
    private val misses: MutableCollection<String>
) : TextureAtlas(data) {

    private companion object {
        const val GUARD_CLASS = "com.maverick.game.engine.drawables.sprites.TextureAtlasExtensionsKt"
        const val GUARD_METHOD = "containsRegion"
        val STACK_WALKER: StackWalker = StackWalker.getInstance()
    }

    override fun findRegion(name: String): AtlasRegion? {
        val region = super.findRegion(name)
        if (region == null && !isGuardedProbe()) misses.add("$atlasName:$name")
        return region
    }

    private fun isGuardedProbe() = STACK_WALKER.walk { frames ->
        frames.limit(4).anyMatch { it.className == GUARD_CLASS && it.methodName == GUARD_METHOD }
    }

    override fun findRegion(name: String, index: Int): AtlasRegion? {
        val region = super.findRegion(name, index)
        if (region == null) misses.add("$atlasName:$name#$index")
        return region
    }
}
