package com.megaman.maverick.game.utils

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.maps.MapProperties
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.OrderedMap
import com.engine.common.extensions.getMusic
import com.engine.common.extensions.getSound
import com.engine.common.objects.Properties
import com.engine.common.shapes.GameRectangle
import com.engine.entities.IGameEntity
import com.megaman.maverick.game.ConstKeys
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.MegamanMaverickGame
import com.megaman.maverick.game.assets.MusicAsset
import com.megaman.maverick.game.assets.SoundAsset

fun IGameEntity.getMegamanMaverickGame() = game as MegamanMaverickGame

fun MapObject.toProps(): Properties {
  val props = Properties()
  props.put(ConstKeys.NAME, name)
  val objProps = properties.toProps()
  props.putAll(objProps)
  return props
}

fun MapProperties.toProps(): Properties {
  val props = Properties()
  keys.forEach { key -> props.put(key, get(key)) }
  return props
}

fun Camera.toGameRectangle(): GameRectangle {
  val rectangle = GameRectangle()
  rectangle.setSize(viewportWidth, viewportHeight)
  rectangle.setCenter(position.x, position.y)
  return rectangle
}

fun getDefaultCameraPosition(): Vector3 {
  val v = Vector3()
  v.x = ConstVals.VIEW_WIDTH * ConstVals.PPM / 2f
  v.y = ConstVals.VIEW_HEIGHT * ConstVals.PPM / 2f
  return v
}

fun Camera.setToDefaultPosition() {
  val v = getDefaultCameraPosition()
  position.set(v)
}

fun AssetManager.getSounds(): OrderedMap<SoundAsset, Sound> {
  val sounds = OrderedMap<SoundAsset, Sound>()
  for (ass in SoundAsset.values()) sounds.put(ass, getSound(ass.source))
  return sounds
}

fun AssetManager.getMusics(): OrderedMap<MusicAsset, Music> {
  val music = OrderedMap<MusicAsset, Music>()
  for (ass in MusicAsset.values()) music.put(ass, getMusic(ass.source))
  return music
}
