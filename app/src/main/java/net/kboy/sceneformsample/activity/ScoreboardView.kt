package net.kboy.sceneformsample.activity

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import net.kboy.sceneformsample.R

class ScoreboardView(context: Context, attrs: AttributeSet? = null, defStyle: Int = -1)
  : FrameLayout(context, attrs, defStyle) {

  init {
    inflate(context, R.layout.scoreboard_view, this)
  }

}
