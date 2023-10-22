package com.megaman.maverick.game.utils

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.maps.MapProperties
import com.badlogic.gdx.utils.OrderedMap
import com.engine.common.extensions.getMusic
import com.engine.common.extensions.getSound
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset

/**
 * Converts a [MapProperties] to a [Properties].
 *
 * @return the [Properties] converted from the [MapProperties].
 */
fun MapProperties.toProps(): Properties {
  val props = Properties()
  keys.forEach { key -> props.put(key, get(key)) }
  return props
}

/**
 * Converts a [Camera] to a [GameRectangle].
 *
 * @return the [GameRectangle] converted from the [Camera].
 */
fun Camera.toGameRectangle(): GameRectangle {
  val rectangle = GameRectangle()
  rectangle.setSize(viewportWidth, viewportHeight)
  rectangle.setCenter(position.x, position.y)
  return rectangle
}

/**
 * Gets all the [Sound]s from the [AssetManager].
 *
 * @return the [OrderedMap] of [SoundAsset]s to [Sound]s.
 */
fun AssetManager.getSounds(): OrderedMap<SoundAsset, Sound> {
  val sounds = OrderedMap<SoundAsset, Sound>()
  for (ass in SoundAsset.values()) sounds.put(ass, getSound(ass.source))
  return sounds
}

/**
 * Gets all the [Music]s from the [AssetManager].
 *
 * @return the [OrderedMap] of [MusicAsset]s to [Music]s.
 */
fun AssetManager.getMusics(): OrderedMap<MusicAsset, Music> {
  val music = OrderedMap<MusicAsset, Music>()
  for (ass in MusicAsset.values()) music.put(ass, getMusic(ass.source))
  return music
}
