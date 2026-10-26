package com.megaman.maverick.game.players

import com.maverick.game.engine.components.IGameComponent

class PlayerLightComponent(val light: () -> PlayerLight) : IGameComponent
