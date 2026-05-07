package com.aiagent.local.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.aiagent.local.data.InferenceSettings
import com.aiagent.local.data.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    
    // Expose settings flow for UI to observe
    val settings = settingsRepo.settingsFlow
    
    // Save functions to be called from UI
    suspend fun saveTemperature(temp: Float) {
        settingsRepo.updateTemperature(temp)
    }
    
    suspend fun saveTopP(topP: Float) {
        settingsRepo.updateTopP(topP)
    }
    
    suspend fun saveTopK(topK: Int) {
        settingsRepo.updateTopK(topK)
    }
    
    suspend fun saveMaxTokens(maxTokens: Int) {
        settingsRepo.updateMaxTokens(maxTokens)
    }
    
    suspend fun saveRepeatPenalty(penalty: Float) {
        settingsRepo.updateRepeatPenalty(penalty)
    }
    
    suspend fun saveGpuLayers(layers: Int) {
        settingsRepo.updateGpuLayers(layers)
    }
    
    suspend fun saveContextSize(size: Int) {
        settingsRepo.updateContextSize(size)
    }
}