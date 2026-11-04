package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.LiquidationRepository
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LiquidationViewModel
import com.example.ui.viewmodel.LiquidationViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = LiquidationRepository(
                liquidationDao = database.liquidationDao(),
                unitDao = database.unitDao(),
                configDao = database.configDao()
            )
            val viewModelFactory = LiquidationViewModelFactory(repository)
            val viewModel = ViewModelProvider(this, viewModelFactory)[LiquidationViewModel::class.java]

            setContent {
                MyApplicationTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        HomeScreen(viewModel = viewModel)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            setContent {
                MyApplicationTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Uygulama yüklenirken bir sorun oluştu: ${e.localizedMessage ?: "Bilinmeyen hata"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
