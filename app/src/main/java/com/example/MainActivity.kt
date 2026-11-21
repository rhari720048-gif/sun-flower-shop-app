package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.SunFlowersApp
import com.example.ui.SunFlowersViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: SunFlowersViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SunFlowersApp(viewModel = viewModel)
        }
    }
}
