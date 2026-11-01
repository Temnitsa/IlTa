package com.iltatrainer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.iltatrainer.app.data.AppContainer
import com.iltatrainer.app.ui.IlTaApp

class MainActivity : ComponentActivity() {

    private lateinit var container: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        container = AppContainer(applicationContext)
        setContent {
            IlTaApp(container)
        }
    }
}
