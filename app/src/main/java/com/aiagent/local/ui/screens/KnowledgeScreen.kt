package com.aiagent.local.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiagent.local.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen(
    chatViewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = uri.lastPathSegment ?: "document.txt"
            chatViewModel.addDocumentToKnowledge(uri, fileName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Knowledge Base") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { documentPicker.launch(arrayOf("text/*", "application/pdf")) }
            ) {
                Icon(Icons.Default.Add, "Add Document")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.LibraryBooks,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Knowledge Base",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "Add documents (.txt, .pdf) to build a searchable knowledge base.\n" +
                            "The AI will retrieve relevant context when answering.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Text(
                    "Requires an embedding model (MiniLM) to be loaded.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Button(onClick = { documentPicker.launch(arrayOf("text/*", "application/pdf")) }) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Document")
                }
            }
        }
    }
}