package com.smartserve.customerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.smartserve.customerapp.navigation.AppNavigation
import com.smartserve.customerapp.ui.theme.CustomerAppTheme
import dagger.hilt.android.AndroidEntryPoint   // ADD THIS

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CustomerAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    AppNavigation()
                }
            }
        }
    }
}
