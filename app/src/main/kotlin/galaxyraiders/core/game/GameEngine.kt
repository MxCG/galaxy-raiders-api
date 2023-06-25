package galaxyraiders.core.game

import galaxyraiders.Config
import galaxyraiders.ports.RandomGenerator
import galaxyraiders.ports.ui.Controller
import galaxyraiders.ports.ui.Controller.PlayerCommand
import galaxyraiders.ports.ui.Visualizer
import kotlin.system.measureTimeMillis
import java.time.Instant
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

const val MILLISECONDS_PER_SECOND: Int = 1000

data class Score(val timestamp: Long, var score: Double)

object GameEngineConfig {
  private val config = Config(prefix = "GR__CORE__GAME__GAME_ENGINE__")

  val frameRate = config.get<Int>("FRAME_RATE")
  val spaceFieldWidth = config.get<Int>("SPACEFIELD_WIDTH")
  val spaceFieldHeight = config.get<Int>("SPACEFIELD_HEIGHT")
  val asteroidProbability = config.get<Double>("ASTEROID_PROBABILITY")
  val coefficientRestitution = config.get<Double>("COEFFICIENT_RESTITUTION")

  val msPerFrame: Int = MILLISECONDS_PER_SECOND / this.frameRate
}

@Suppress("TooManyFunctions")
class GameEngine(
  val generator: RandomGenerator,
  val controller: Controller,
  val visualizer: Visualizer,
) {
  val field = SpaceField(
    width = GameEngineConfig.spaceFieldWidth,
    height = GameEngineConfig.spaceFieldHeight,
    generator = generator
  )

  val score: Score

	private val scoreboardFile: File = File("src/main/kotlin/galaxyraiders/core/score/Scoreboard.json")
	private val leaderboardFile: File = File("src/main/kotlin/galaxyraiders/core/score/Leaderboard.json")

  private var scoreboard: List<Score>
		get() = readScoresFromFile(scoreboardFile)
		set(value) {
			writeScoresToFile(scoreboardFile, value)
		}

  private var leaderboard: List<Score>
		get() = readScoresFromFile(leaderboardFile)
		set(value) {
			writeScoresToFile(leaderboardFile, value)
		}

  var playing = true

  init {
		score = Score(Instant.now().getEpochSecond(), 0.0)
  }

  fun execute() {
    while (true) {
      val duration = measureTimeMillis { this.tick() }

      Thread.sleep(
        maxOf(0, GameEngineConfig.msPerFrame - duration)
      )
    }
  }

  fun execute(maxIterations: Int) {
    repeat(maxIterations) {
      this.tick()
    }
  }

  fun tick() {
    this.processPlayerInput()
    this.updateSpaceObjects()
    this.renderSpaceField()
  }

  fun processPlayerInput() {
    this.controller.nextPlayerCommand()?.also {
      when (it) {
        PlayerCommand.MOVE_SHIP_UP ->
          this.field.ship.boostUp()
        PlayerCommand.MOVE_SHIP_DOWN ->
          this.field.ship.boostDown()
        PlayerCommand.MOVE_SHIP_LEFT ->
          this.field.ship.boostLeft()
        PlayerCommand.MOVE_SHIP_RIGHT ->
          this.field.ship.boostRight()
        PlayerCommand.LAUNCH_MISSILE ->
          this.field.generateMissile()
        PlayerCommand.PAUSE_GAME ->
          this.playing = !this.playing
      }
    }
  }

  fun updateSpaceObjects() {
    if (!this.playing) return
    this.handleCollisions()
    this.moveSpaceObjects()
    this.trimSpaceObjects()
    this.generateAsteroids()
  }

  fun handleCollisions() {
    this.field.spaceObjects.forEachPair {
        (first, second) ->
      if (first.impacts(second)) {
        if (first is Asteroid && second is Missile) {
          this.destroyAsteroid(first)
        }

        if (first is Missile && second is Asteroid) {
          this.destroyAsteroid(second)
        }

        first.collideWith(second, GameEngineConfig.coefficientRestitution)
      }
    }
  }

  fun destroyAsteroid(asteroid: Asteroid) {
    this.field.explodeAsteroid(asteroid)
    this.score.score += asteroid.radius * asteroid.mass
    this.saveScore()
  }

  fun moveSpaceObjects() {
    this.field.moveShip()
    this.field.moveAsteroids()
    this.field.moveMissiles()
  }

  fun trimSpaceObjects() {
    this.field.trimAsteroids()
    this.field.trimMissiles()
    this.field.trimExplosions()
  }

  fun generateAsteroids() {
    val probability = generator.generateProbability()

    if (probability <= GameEngineConfig.asteroidProbability) {
      this.field.generateAsteroid()
    }
  }

  fun renderSpaceField() {
    this.visualizer.renderSpaceField(this.field)
  }

  fun saveScore() {
		var scoreboard = this.scoreboard

		scoreboard = scoreboard.filter { it.timestamp != this.score.timestamp }
		scoreboard += this.score

		this.scoreboard = scoreboard

		this.leaderboard = scoreboard.sortedByDescending { it.score }.take(3)
  }

  private fun readScoresFromFile(file: File): List<Score> {
		if (!file.exists()) {
			return emptyList()
		}

		val fileText = file.readText()

		if (fileText.isEmpty()) {
			return emptyList()
		}

		val mapper = jacksonObjectMapper()

		return mapper.readValue(fileText)
  }

  private fun writeScoresToFile(file: File, list: List<Score>) {
		if (!file.exists()) {
			file.createNewFile()
		}

		val mapper = jacksonObjectMapper()

		file.writeText(mapper.writeValueAsString(list))
  }
}

fun <T> List<T>.forEachPair(action: (Pair<T, T>) -> Unit) {
  for (i in 0 until this.size) {
    for (j in i + 1 until this.size) {
      action(Pair(this[i], this[j]))
    }
  }
}
