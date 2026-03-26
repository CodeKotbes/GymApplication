package com.example.gymapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.gymapplication.data.GymDatabase
import com.example.gymapplication.gymUI.GymApp
import com.example.gymapplication.gymUI.GymViewModel
import com.example.gymapplication.gymUI.GymViewModelFactory
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        super.onCreate(savedInstanceState)

        val database = GymDatabase.getDatabase(this)

        val viewModel: GymViewModel by viewModels {
            GymViewModelFactory(database.gymDao())
        }

        enableEdgeToEdge()

        setContent {
            GymApp(viewModel)
        }
    }
}