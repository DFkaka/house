
package com.example.house

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.house.ui.navigation.HouseNavGraph
import com.example.house.ui.theme.HouseUtilityTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as HouseApp).container
        setContent {
            HouseUtilityTheme {
                HouseNavGraph(container)
            }
        }
    }
}
