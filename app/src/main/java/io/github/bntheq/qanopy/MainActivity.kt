package io.github.bntheq.qanopy

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val yazi = TextView(this)
        yazi.text = getString(R.string.hello)
        yazi.textSize = 28f
        yazi.gravity = Gravity.CENTER
        yazi.setTextColor(Color.WHITE)
        yazi.setBackgroundColor(Color.parseColor("#1B1B2F"))
        setContentView(yazi)
    }
}
