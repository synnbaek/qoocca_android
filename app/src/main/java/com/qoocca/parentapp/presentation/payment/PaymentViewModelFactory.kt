package com.qoocca.parentapp.presentation.payment

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qoocca.parentapp.AuthManager
import com.qoocca.parentapp.domain.usecase.GetReceiptDetailsUseCase
import com.qoocca.parentapp.domain.usecase.PayReceiptsUseCase

class PaymentViewModelFactory(
    private val application: Application,
    private val authManager: AuthManager,
    private val getReceiptDetailsUseCase: GetReceiptDetailsUseCase,
    private val payReceiptsUseCase: PayReceiptsUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaymentViewModel::class.java)) {
            return PaymentViewModel(application, authManager, getReceiptDetailsUseCase, payReceiptsUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
