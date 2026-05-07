package com.aiagent.local.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiagent.local.data.InferenceSettings
import com.aiagent.local.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsState(initial = InferenceSettings())
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Inference Parameters", style = MaterialTheme.typography.titleMedium)

            // Temperature
            Text("Temperature: ${settings.temperature}")
            Slider(
                value = settings.temperature,
                onValueChange = { 
                    scope.launch { viewModel.saveTemperature(it) } 
                },
                valueRange = 0f..2f
            )

            // Top-P
            Text("Top-P: ${settings.topP}")
            Slider(
                value = settings.topP,
                onValueChange = { 
                    scope.launch { viewModel.saveTopP(it) } 
                },
                valueRange = 0f..1f
            )

            // Top-K
            Text("Top-K: ${settings.topK}")
            Slider(
                value = settings.topK.toFloat(),
                onValueChange = { 
                    scope.launch { viewModel.saveTopK(it.toInt()) } 
                },
                valueRange = 1f..100f,
                steps = 98
            )

            // Max Tokens
            Text("Max Tokens: ${settings.maxTokens}")
            Slider(
                value = settings.maxTokens.toFloat(),
                onValueChange = { 
                    scope.launch { viewModel.saveMaxTokens(it.toInt()) } 
                },
                valueRange = 64f..2048f,
                steps = 30
            )

            // Repeat Penalty
            Text("Repeat Penalty: ${settings.repeatPenalty}")
            Slider(
                value = settings.repeatPenalty,
                onValueChange = { 
                    scope.launch { viewModel.saveRepeatPenalty(it) } 
                },
                valueRange = 1f..2f
            )

            // GPU Layers
            Text("GPU Layers: ${settings.gpuLayers}")
            Slider(
                value = settings.gpuLayers.toFloat(),
                onValueChange = { 
                    scope.launch { viewModel.saveGpuLayers(it.toInt()) } 
                },
                valueRange = 0f..99f,
                steps = 98
            )

            // Context Size
            Text("Context Size: ${settings.contextSize}")
            Slider(
                value = settings.contextSize.toFloat(),
                onValueChange = { 
                    scope.launch { viewModel.saveContextSize(it.toInt()) } 
                },
                valueRange = 256f..4096f,
                steps = 29
            )

            Divider()
            Text("About", style = MaterialTheme.typography.titleMedium)
            Text("Local AI Agent v1.0")
            Text("Fully offline AI with GGUF models, tool calling, and RAG.")
            Text("Powered by llama.cpp (de.kherud:llama)")
            Text("Inspired by Off-Grid, Jandal AI, ToolNeuron, and SmolChat.")
        }
    }
}