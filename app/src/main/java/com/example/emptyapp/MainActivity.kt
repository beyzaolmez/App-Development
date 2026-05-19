package com.example.emptyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.emptyapp.navigation.MomentumApp
import com.example.emptyapp.ui.theme.MomentumTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MomentumTheme {
                MomentumApp()
            }
        }
    }
}
