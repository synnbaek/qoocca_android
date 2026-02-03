package com.qoocca.parentapp.presentation.login

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.messaging.FirebaseMessaging
import com.qoocca.parentapp.AuthManager
import com.qoocca.parentapp.data.network.ApiResult
import com.qoocca.parentapp.data.repository.AuthRepository
import com.qoocca.parentapp.data.repository.FcmRepository
import com.qoocca.parentapp.domain.result.AppResult
import com.qoocca.parentapp.presentation.common.AppEventLogger
import com.qoocca.parentapp.presentation.common.UiMessageFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager(application)
    private val authRepository = AuthRepository()
    private val fcmRepository = FcmRepository()

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    fun onPhoneChanged(phone: String) {
        _uiState.value = _uiState.value.copy(phone = phone)
    }

    fun login() {
        val phone = _uiState.value.phone
        if (phone.isBlank()) {
            _events.tryEmit(LoginEvent.ShowMessage(UiMessageFactory.PHONE_REQUIRED))
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)

        authRepository.login(phone) { result ->
            when (result) {
                is AppResult.Success -> {
                    val login = result.data
                    Log.d(TAG, "로그인 성공: ${login.parentName} (ID: ${login.parentId})")
                    AppEventLogger.logEvent(getApplication(), "login_success", mapOf("parent_id" to login.parentId))
                    authManager.saveAuthData(login.parentId, login.accessToken)
                    registerFcmToken(login.parentId)

                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _events.tryEmit(LoginEvent.NavigateMain)
                }

                is AppResult.Failure -> {
                    Log.e(TAG, "로그인 처리 실패: ${result.error}")
                    AppEventLogger.logEvent(getApplication(), "login_failure", mapOf("error" to result.error.toString()))
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _events.tryEmit(LoginEvent.ShowMessage(UiMessageFactory.loginFailure(result.error)))
                }
            }
        }
    }

    private fun registerFcmToken(parentId: Long) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                fcmRepository.registerToken(parentId, token) { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            Log.d(TAG, "FCM 토큰 등록 완료")
                            AppEventLogger.logEvent(getApplication(), "fcm_register_success")
                        }
                        is ApiResult.HttpError -> {
                            Log.e(TAG, "FCM 토큰 등록 실패: ${result.code} - ${result.body}")
                            AppEventLogger.logEvent(getApplication(), "fcm_register_failure", mapOf("code" to result.code))
                        }
                        is ApiResult.NetworkError -> {
                            Log.e(TAG, "FCM 토큰 등록 실패: ${result.exception.message}")
                            AppEventLogger.logEvent(getApplication(), "fcm_register_failure", mapOf("type" to "network"))
                        }
                        is ApiResult.UnknownError -> {
                            Log.e(TAG, "FCM 토큰 등록 실패: ${result.exception.message}")
                            AppEventLogger.recordNonFatal(result.exception, mapOf("screen" to "login", "action" to "fcm_register"))
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "LOGIN_DEBUG"
    }
}
