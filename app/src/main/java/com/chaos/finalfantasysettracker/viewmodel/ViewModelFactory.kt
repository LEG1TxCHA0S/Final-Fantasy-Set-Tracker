package com.chaos.finalfantasysettracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.chaos.finalfantasysettracker.TrackerApplication
import com.chaos.finalfantasysettracker.repository.CollectionRepository

object ViewModelFactory {
    fun repository(extras: CreationExtras): CollectionRepository {
        val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as TrackerApplication
        return app.container.repository
    }
}
