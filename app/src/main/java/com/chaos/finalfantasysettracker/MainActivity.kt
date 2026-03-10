package com.chaos.finalfantasysettracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.chaos.finalfantasysettracker.navigation.FinalFantasyTrackerApp
import com.chaos.finalfantasysettracker.ui.theme.FinalFantasySetTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinalFantasySetTrackerTheme {
                FinalFantasyTrackerApp()
            }
        }
    }
}
