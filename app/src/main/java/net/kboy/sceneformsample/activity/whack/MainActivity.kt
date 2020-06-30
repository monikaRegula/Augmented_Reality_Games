package net.kboy.sceneformsample.activity.whack

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Light
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import net.kboy.sceneformsample.R
import net.kboy.sceneformsample.activity.whack.KingPosition.*
import net.kboy.sceneformsample.activity.whack.Configuration.Companion.ROW_NUM
import net.kboy.sceneformsample.activity.whack.Configuration.Companion.COL_NUM
import net.kboy.sceneformsample.activity.whack.Configuration.Companion.START_LIVES
import net.kboy.sceneformsample.activity.whack.Configuration.Companion.MIN_MOVE_DELAY_MS
import net.kboy.sceneformsample.activity.whack.Configuration.Companion.MAX_MOVE_DELAY_MS
import net.kboy.sceneformsample.activity.whack.Configuration.Companion.MIN_PULL_DOWN_DELAY_MS
import net.kboy.sceneformsample.activity.whack.Configuration.Companion.MAX_PULL_DOWN_DELAY_MS
import net.kboy.sceneformsample.activity.whack.Configuration.Companion.MOVES_PER_TIME


class MainActivity : AppCompatActivity() {

  private lateinit var arFragment: ArFragment
  private var droidRenderable: ModelRenderable? = null
  private var scoreboardRenderable: ViewRenderable? = null
  private var failLight: Light? = null

  private lateinit var scoreboard: ScoreboardView

  private var grid = Array(ROW_NUM) { arrayOfNulls<TranslatableNode>(COL_NUM) }
  private var initialized = false

  private var gameHandler = Handler()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment

    initResources()

    arFragment.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, _: MotionEvent ->
      if (initialized) {
        // Already inizialized!
        // When the game is initialized and user touches without
        // hitting a droid, remove 50 points
        failHit()
        return@setOnTapArPlaneListener
      }

      if (plane.type != Plane.Type.HORIZONTAL_UPWARD_FACING) {
        // Only HORIZONTAL_UPWARD_FACING planes are good to play the game
        // Notify the user and return
        "Find an HORIZONTAL and UPWARD FACING plane!".toast(this)
        return@setOnTapArPlaneListener
      }

      if(droidRenderable == null || scoreboardRenderable == null || failLight == null){
        // Every renderable object must be initialized
        // On a real world/complex application
        // it can be useful to add a visual loading
        return@setOnTapArPlaneListener
      }

      val spacing = 0.3F

      val anchorNode = AnchorNode(hitResult.createAnchor())

      anchorNode.setParent(arFragment.arSceneView.scene)

      // Add N droid to the plane (N = COL x ROW)
      grid.matrixIndices { col, row ->
        val renderableModel = droidRenderable?.makeCopy() ?: return@matrixIndices
        TranslatableNode().apply {
              setParent(anchorNode)
              localScale = Vector3(0.2F,0.2F,0.2F)
              localScale = Vector3(3F,3F,3F)
              renderable = renderableModel
              addOffset(x = row * spacing, z = col * spacing)
              grid[col][row] = this

              this.setOnTapListener { _, _ ->
                if (this.position != DOWN) {
                  // Droid hitted! assign 100 points
                  scoreboard.score += 100
                  var sound = MediaPlayer.create(this@MainActivity,R.raw.score)
                  sound.start()
                  this.pullDown()
                } else {
                  // When player hits a droid that is not up
                  // it's like a "miss", so remove 50 points
                  failHit()
                }
              }
            }
      }

      // Add the scoreboard view to the plane
      val renderableView = scoreboardRenderable ?: return@setOnTapArPlaneListener
      TranslatableNode()
          .also {
            it.setParent(anchorNode)
            it.renderable = renderableView
            it.addOffset(x = spacing, y = .6F)
          }

      // Add a light
      Node().apply {
        setParent(anchorNode)
        light = failLight
        localPosition = Vector3(.3F, .3F, .3F)

      }

      initialized = true
    }
  }

  private val pullUpRunnable: Runnable by lazy {
    Runnable {
      if (scoreboard.life > 0) {
        grid.flatMap { it.toList() }
            .filter { it?.position == DOWN }
            .run { takeIf { size > 0 }?.getOrNull((0..size).random()) }
            ?.apply {
              pullUp()
              val pullDownDelay = (MIN_PULL_DOWN_DELAY_MS..MAX_PULL_DOWN_DELAY_MS).random()
              gameHandler.postDelayed({ pullDown() }, pullDownDelay)
            }

        // Delay between this move and the next one
        val nextMoveDelay = (MIN_MOVE_DELAY_MS..MAX_MOVE_DELAY_MS).random()
        gameHandler.postDelayed(pullUpRunnable, nextMoveDelay)
      }
    }
  }

  private fun failHit() {
    scoreboard.score -= 50
    scoreboard.life -= 1
    var sound = MediaPlayer.create(this@MainActivity,R.raw.failed_hit)
    sound.start()
    failLight?.blink()
    if (scoreboard.life <= 0) {
      // Game over
      gameHandler.removeCallbacksAndMessages(null)
      grid.flatMap { it.toList() }
          .filterNotNull()
          .filter { it.position != DOWN && it.position != MOVING_DOWN }
          .forEach { it.pullDown() }
    }
  }

  private fun initResources() {
    // Create a droid renderable (asynchronous operation,
    // result is delivered to `thenAccept` method)
    ModelRenderable.builder()
        .setSource(this, Uri.parse("kingWhite.sfb"))
        .build()
        .thenAccept { droidRenderable = it }
        .exceptionally { it.toast(this) }

    scoreboard = ScoreboardView(this)

    scoreboard.onStartTapped = {
      // Reset counters
      scoreboard.life = START_LIVES
      scoreboard.score = 0
      // Start the game!
      gameHandler.post {
        repeat(MOVES_PER_TIME) {
          gameHandler.post(pullUpRunnable)
        }
      }
    }

    // create a scoreboard renderable (asynchronous operation,
    // result is delivered to `thenAccept` method)
    ViewRenderable.builder()
        .setView(this, scoreboard)
        .build()
        .thenAccept {
          it.isShadowReceiver = true
          scoreboardRenderable = it
        }
        .exceptionally { it.toast(this) }

    // Creating a light is NOT asynchronous
    failLight = Light.builder(Light.Type.POINT)
        .setColor(Color(android.graphics.Color.RED))
        .setShadowCastingEnabled(true)
        .setIntensity(0F)
        .build()

  }
}