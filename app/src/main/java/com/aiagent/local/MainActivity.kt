package com.aiagent.local

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aiagent.local.ui.screens.ChatScreen
import com.aiagent.local.ui.screens.KnowledgeScreen
import com.aiagent.local.ui.screens.SettingsScreen
import com.aiagent.local.ui.theme.LocalAIAgentTheme
import com.aiagent.local.ui.viewmodel.ChatViewModel
import com.aiagent.local.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocalAIAgentTheme {
                val navController = rememberNavController()
                val chatViewModel: ChatViewModel = viewModel()
                val settingsViewModel: SettingsViewModel = viewModel()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "chat",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("chat") {
                            ChatScreen(
                                viewModel = chatViewModel,
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                onNavigateToKnowledge = {
                                    navController.navigate("knowledge")
                                }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("knowledge") {
                            KnowledgeScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}