package net.kboy.sceneformsample.activity

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.menu_activity.*
import net.kboy.sceneformsample.R
import net.kboy.sceneformsample.activity.whack.MainActivity

class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_activity)
    }


    fun onChessClick(view: View){
        val intent = Intent(this, SceneformActivity::class.java)
        startActivity(intent)
    }

    fun onWhackClick(view: View){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}