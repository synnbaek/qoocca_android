package com.qoocca.parentapp.presentation.main

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qoocca.parentapp.AuthManager
import com.qoocca.parentapp.domain.usecase.GetReceiptListUseCase

class MainViewModelFactory(
    private val application: Application,
    private val authManager: AuthManager,
    private val getReceiptListUseCase: GetReceiptListUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(application, authManager, getReceiptListUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
