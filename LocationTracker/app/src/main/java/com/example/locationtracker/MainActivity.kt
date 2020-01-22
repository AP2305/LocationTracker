package com.example.locationtracker

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener(View.OnClickListener { v: View? ->

            var acc = accuracy.showText
            var mapActivity = Intent(this,MapsActivity::class.java)
            intent.putExtra("accuracy",acc)
            startActivity(mapActivity)
        })

    }
}
