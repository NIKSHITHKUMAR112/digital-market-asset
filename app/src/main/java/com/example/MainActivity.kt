package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.DigitalMarketAppScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val viewModel = ViewModelProvider(this, MainViewModelFactory(application))[MainViewModel::class.java]

    setContent {
      DigitalMarketAppScreen(viewModel = viewModel)
    }
  }
}
