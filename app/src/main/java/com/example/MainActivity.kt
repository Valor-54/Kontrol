package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.GamepadRepository
import com.example.input.InputDispatchEngine
import com.example.shizuku.ShizukuManager
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: GamepadRepository
    private lateinit var inputEngine: InputDispatchEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = GamepadRepository.getInstance(this)
        inputEngine = InputDispatchEngine.getInstance(this)

        // Initialize Shizuku manager listeners
        ShizukuManager.initialize(this)

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) { innerPadding ->
                    HomeScreen(
                        repository = repository,
                        engine = inputEngine
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ShizukuManager.checkStatus()
    }
}
