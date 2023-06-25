package galaxyraiders.core.game

import galaxyraiders.Config
import galaxyraiders.core.physics.Point2D
import galaxyraiders.core.physics.Vector2D

object ExplosionConfig {
  private val config = Config(prefix = "GR__CORE__GAME__EXPLOSION__")

  val lifetime = config.get<Int>("LIFETIME")
}

class Explosion(
  position: Point2D,
  radius: Double,
) :
  SpaceObject("Explosion", '*', position, Vector2D(0.0, 0.0), radius, 0.0) {
  var lifetime = ExplosionConfig.lifetime
}
