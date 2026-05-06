package com.aiagent.local.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.aiagent.local.data.ChatMessage
import com.aiagent.local.ui.components.ChatBubble
import com.aiagent.local.ui.components.ModelPickerDialog
import com.aiagent.local.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToKnowledge: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isModelLoaded by viewModel.isModelLoaded.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val statusText by viewModel.statusText.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showModelPicker by remember { mutableStateOf(false) }
    var showEmbeddingPicker by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // File picker for GGUF model
    val modelFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadModelFromUri(it) }
    }

    // File picker for embedding model
    val embeddingFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadEmbeddingModelFromUri(it) }
    }

    // Scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local AI Agent") },
                actions = {
                    if (!isModelLoaded) {
                        IconButton(onClick = { modelFilePicker.launch(arrayOf("*/*")) }) {
                            Icon(Icons.Default.FolderOpen, "Load Model")
                        }
                    }
                    IconButton(onClick = onNavigateToKnowledge) {
                        Icon(Icons.Default.LibraryBooks, "Knowledge Base")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(Icons.Default.Delete, "Clear Chat")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        enabled = isModelLoaded && !isGenerating,
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val msg = inputText
                                inputText = ""
                                viewModel.sendMessage(msg)
                            }
                        },
                        enabled = isModelLoaded && !isGenerating && inputText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, "Send")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status bar
            if (statusText.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Loading indicator
            if (!isModelLoaded) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No model loaded",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { modelFilePicker.launch(arrayOf("*/*")) }) {
                            Icon(Icons.Default.FolderOpen, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select GGUF Model")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Or load an embedding model for RAG:",
                            style = MaterialTheme.typography.labelSmall
                        )
                        TextButton(onClick = { embeddingFilePicker.launch(arrayOf("*/*")) }) {
                            Text("Load Embedding Model (MiniLM)")
                        }
                    }
                }
            }

            // Chat messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages) { message ->
                    when (message) {
                        is ChatMessage.User -> ChatBubble(
                            text = message.content,
                            isUser = true
                        )
                        is ChatMessage.Bot -> ChatBubble(
                            text = message.content,
                            isUser = false,
                            isStreaming = message.isStreaming
                        )
                        is ChatMessage.System -> ChatBubble(
                            text = message.content,
                            isUser = false,
                            isSystem = true
                        )
                        is ChatMessage.ToolCall -> ChatBubble(
                            text = message.content,
                            isUser = false,
                            isTool = true
                        )
                    }
                }
            }
        }
    }
}