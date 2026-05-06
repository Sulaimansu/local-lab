package com.aiagent.local.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.aiagent.local.data.InferenceSettings
import com.aiagent.local.data.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)

    // We'll access settings via ChatViewModel for simplicity
    // In a real app, you'd have dedicated ViewModels
}