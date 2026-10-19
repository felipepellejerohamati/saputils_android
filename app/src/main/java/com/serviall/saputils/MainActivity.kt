package com.serviall.saputils

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.serviall.saputils.ui.login.LoginScreen
import com.serviall.saputils.ui.main.MainScreen
import com.serviall.saputils.ui.main.WarehouseSelectionDialog
import com.serviall.saputils.ui.theme.SaputilsTheme
import com.serviall.saputils.ui.viewmodel.ServialViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SaputilsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val servialViewModel: ServialViewModel = viewModel()
                    var isLoggedIn by remember { mutableStateOf(false) }
                    var loggedInUser by remember { mutableStateOf("") }
                    var showWarehouseSelectionDialog by remember { mutableStateOf(false) }

                    if (!isLoggedIn) {
                        LoginScreen(
                            onLoginSuccess = { username ->
                                loggedInUser = username
                                showWarehouseSelectionDialog = true
                            }
                        )
                    } else {
                        MainScreen(
                            username = loggedInUser.ifBlank { "Administrador" },
                            viewModel = servialViewModel,
                            onLogout = {
                                isLoggedIn = false
                                loggedInUser = ""
                            }
                        )
                    }

                    if (showWarehouseSelectionDialog) {
                        WarehouseSelectionDialog(
                            currentWarehouse = "Bodega Central",
                            onWarehouseSelected = { warehouse ->
                                servialViewModel.setWarehouse(warehouse)
                                showWarehouseSelectionDialog = false
                                isLoggedIn = true
                            }
                        )
                    }
                }
            }
        }
    }
}
