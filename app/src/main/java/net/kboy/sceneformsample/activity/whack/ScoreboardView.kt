package net.kboy.sceneformsample.activity.whack

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.scoreboard_view.view.*
import kotlinx.android.synthetic.main.scoreboard_view_whack.view.*
import net.kboy.sceneformsample.R

class ScoreboardView(context: Context, attrs: AttributeSet? = null, defStyle: Int = -1)
  : FrameLayout(context, attrs, defStyle) {

  init {
    inflate(context, R.layout.scoreboard_view_whack, this)

    start_btn.setOnClickListener {
      it.isEnabled = false
      onStartTapped?.invoke()
    }
  }

  var onStartTapped: (() -> Unit)? = null
  var score: Int = 0
    set(value) {
      field = value
      score_counter.text = value.toString()
    }

  var life: Int = 0
    set(value) {
      if (field == 0 && value > field) {
        // Game has been restarted, hide game over message
        gameover.visibility = GONE
      }
      field = value
      life_counter.text = value.toString()

      // If player has 0 lives, show a game over message,
      //      // re enable start btn and change it's mesasge
      if (value <= 0) {
        gameover.visibility = View.VISIBLE
        start_btn.isEnabled = true
        start_btn.setText(R.string.restart)
        var sound = MediaPlayer.create(this.context, R.raw.game_over)
        sound.start()
      }
    }
}
