package com.megaman.maverick.game.entities.contracts

enum class SidewaysValue {
  LEFT,
  RIGHT
}

interface ISidewaysable {
  var sidewaysValue: SidewaysValue?
}
