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
import com.aiagent.local.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
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

            var temperature by remember { mutableFloatStateOf(0.7f) }
            var topP by remember { mutableFloatStateOf(0.9f) }
            var topK by remember { mutableIntStateOf(40) }
            var maxTokens by remember { mutableIntStateOf(512) }
            var repeatPenalty by remember { mutableFloatStateOf(1.1f) }
            var gpuLayers by remember { mutableIntStateOf(20) }
            var contextSize by remember { mutableIntStateOf(2048) }

            // Temperature
            Text("Temperature: $temperature")
            Slider(
                value = temperature,
                onValueChange = { temperature = it },
                valueRange = 0f..2f
            )

            // Top-P
            Text("Top-P: $topP")
            Slider(
                value = topP,
                onValueChange = { topP = it },
                valueRange = 0f..1f
            )

            // Top-K
            Text("Top-K: $topK")
            Slider(
                value = topK.toFloat(),
                onValueChange = { topK = it.toInt() },
                valueRange = 1f..100f,
                steps = 98
            )

            // Max Tokens
            Text("Max Tokens: $maxTokens")
            Slider(
                value = maxTokens.toFloat(),
                onValueChange = { maxTokens = it.toInt() },
                valueRange = 64f..2048f,
                steps = 30
            )

            // Repeat Penalty
            Text("Repeat Penalty: $repeatPenalty")
            Slider(
                value = repeatPenalty,
                onValueChange = { repeatPenalty = it },
                valueRange = 1f..2f
            )

            // GPU Layers
            Text("GPU Layers: $gpuLayers")
            Slider(
                value = gpuLayers.toFloat(),
                onValueChange = { gpuLayers = it.toInt() },
                valueRange = 0f..99f,
                steps = 98
            )

            // Context Size
            Text("Context Size: $contextSize")
            Slider(
                value = contextSize.toFloat(),
                onValueChange = { contextSize = it.toInt() },
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