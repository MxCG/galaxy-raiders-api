package galaxyraiders.core.physics

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

const val HALF_CIRCLE_DEGREES = 180.0
const val HALF_CIRCLE_RADIANS = Math.PI
const val RAD_TO_DEGREE = HALF_CIRCLE_DEGREES / HALF_CIRCLE_RADIANS

@JsonIgnoreProperties("unit", "normal", "degree", "magnitude")
data class Vector2D(val dx: Double, val dy: Double) {
  override fun toString(): String {
    return "Vector2D(dx=$dx, dy=$dy)"
  }

  val magnitude: Double
    get() = Math.sqrt(this * this)

  val radiant: Double
    get() = kotlin.math.sign(dy) * Math.acos(this.unit * Vector2D(1.0, 0.0))

  val degree: Double
    get() = RAD_TO_DEGREE * this.radiant

  val unit: Vector2D
    get() = this / this.magnitude

  val normal: Vector2D
    get() = Vector2D(dy, -dx).unit

  operator fun times(scalar: Double): Vector2D {
    return Vector2D(scalar * dx, scalar * dy)
  }

  operator fun div(scalar: Double): Vector2D {
    return (1 / scalar) * this
  }

  operator fun times(v: Vector2D): Double {
    return dx * v.dx + dy * v.dy
  }

  operator fun plus(v: Vector2D): Vector2D {
    return Vector2D(dx + v.dx, dy + v.dy)
  }

  operator fun plus(p: Point2D): Point2D {
    return Point2D(p.x + dx, p.y + dy)
  }

  operator fun unaryMinus(): Vector2D {
    return -1.0 * this
  }

  operator fun minus(v: Vector2D): Vector2D {
    return this + (-v)
  }

  fun scalarProject(target: Vector2D): Double {
    return this * target.unit
  }

  fun vectorProject(target: Vector2D): Vector2D {
    return target.unit * this.scalarProject(target)
  }
}

operator fun Double.times(v: Vector2D): Vector2D {
  return v * this
}
