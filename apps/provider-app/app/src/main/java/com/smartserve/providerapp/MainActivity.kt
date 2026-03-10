package com.smartserve.providerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.smartserve.providerapp.navigation.AppNavigation
import com.smartserve.providerapp.ui.theme.ProviderAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProviderAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    AppNavigation()
                }
            }
        }
    }
}