package com.megaman.maverick.game.assets

/** An asset. */
interface IAsset {

  // The source of the asset
  val source: String

  // The class of the asset
  val assClass: Class<*>
}
