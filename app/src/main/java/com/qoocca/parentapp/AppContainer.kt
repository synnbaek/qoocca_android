package com.qoocca.parentapp

import com.google.gson.Gson
import com.qoocca.parentapp.data.network.ApiClient
import com.qoocca.parentapp.data.repository.AuthRepository
import com.qoocca.parentapp.data.repository.FcmRepository
import com.qoocca.parentapp.data.repository.PaymentRepository
import com.qoocca.parentapp.data.repository.ReceiptRepository
import com.qoocca.parentapp.domain.usecase.GetReceiptDetailsUseCase
import com.qoocca.parentapp.domain.usecase.GetReceiptListUseCase
import com.qoocca.parentapp.domain.usecase.LoginUseCase
import com.qoocca.parentapp.domain.usecase.PayReceiptsUseCase
import com.qoocca.parentapp.domain.usecase.RegisterFcmTokenUseCase
import okhttp3.OkHttpClient

class AppContainer(application: ParentAppApplication) {
    private val okHttpClient = OkHttpClient()
    private val gson = Gson()
    private val apiClient = ApiClient(okHttpClient)

    val authManager = AuthManager(application)
    val authRepository = AuthRepository(apiClient)
    val fcmRepository = FcmRepository(apiClient)
    val receiptRepository = ReceiptRepository(apiClient, gson)
    val paymentRepository = PaymentRepository(apiClient)

    val loginUseCase = LoginUseCase(authRepository)
    val registerFcmTokenUseCase = RegisterFcmTokenUseCase(fcmRepository)
    val getReceiptListUseCase = GetReceiptListUseCase(receiptRepository)
    val getReceiptDetailsUseCase = GetReceiptDetailsUseCase(receiptRepository)
    val payReceiptsUseCase = PayReceiptsUseCase(paymentRepository)
}
