package com.iot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.iot.app.ui.navigation.SmartHomeNavHost
import com.iot.app.ui.theme.IotAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IotAppTheme {
                SmartHomeNavHost()
            }
        }
    }
}
