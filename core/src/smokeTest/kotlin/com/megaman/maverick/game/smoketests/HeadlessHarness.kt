package com.megaman.maverick.game.smoketests

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.assets.loaders.SynchronousAssetLoader
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Array
import com.megaman.maverick.game.assets.TextureAsset
import com.megaman.maverick.game.entities.MegaEntityFactory
import com.megaman.maverick.game.testServices
import io.mockk.mockk

class HeadlessHarness {

    val regionMisses = LinkedHashSet<String>()

    val assets: AssetManager

    private val application = HeadlessApplication(object : ApplicationAdapter() {})

    init {
        val gl = mockk<GL20>(relaxed = true)
        Gdx.gl = gl
        Gdx.gl20 = gl

        val resolver = InternalFileHandleResolver()
        assets = AssetManager(resolver)
        assets.setLoader(TextureAtlas::class.java, RecordingAtlasLoader(resolver, regionMisses))
        TextureAsset.entries.forEach { assets.load(it.source, TextureAtlas::class.java) }
        assets.finishLoading()

        MegaEntityFactory.init(testServices(assets = assets))
        check(regionMisses.isEmpty()) { "Region misses in the preloaded entities: $regionMisses" }
    }

    fun dispose() = application.exit()

    private class RecordingAtlasLoader(
        resolver: FileHandleResolver,
        private val misses: MutableCollection<String>
    ) : SynchronousAssetLoader<TextureAtlas, AssetLoaderParameters<TextureAtlas>>(resolver) {

        override fun load(
            assetManager: AssetManager,
            fileName: String,
            file: FileHandle,
            parameter: AssetLoaderParameters<TextureAtlas>?
        ): TextureAtlas {
            val data = TextureAtlas.TextureAtlasData(file, file.parent(), false)
            return RecordingTextureAtlas(file.name(), data, misses)
        }

        override fun getDependencies(
            fileName: String,
            file: FileHandle,
            parameter: AssetLoaderParameters<TextureAtlas>?
        ): Array<AssetDescriptor<*>>? = null
    }
}
