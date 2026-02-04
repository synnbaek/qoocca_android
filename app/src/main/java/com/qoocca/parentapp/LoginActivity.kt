package com.qoocca.parentapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qoocca.parentapp.presentation.login.LoginEvent
import com.qoocca.parentapp.presentation.login.LoginViewModel
import com.qoocca.parentapp.presentation.login.LoginViewModelFactory
import com.qoocca.parentapp.ui.theme.QooccaParentsTheme

class LoginActivity : ComponentActivity() {

    private val appContainer by lazy { (application as ParentAppApplication).appContainer }
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(
            application = application,
            authManager = appContainer.authManager,
            loginUseCase = appContainer.loginUseCase,
            registerFcmTokenUseCase = appContainer.registerFcmTokenUseCase
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            QooccaParentsTheme {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is LoginEvent.ShowMessage -> {
                                Toast.makeText(this@LoginActivity, event.message, Toast.LENGTH_SHORT).show()
                            }

                            LoginEvent.NavigateMain -> {
                                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                finish()
                            }
                        }
                    }
                }

                LoginScreen(
                    phone = uiState.phone,
                    isLoading = uiState.isLoading,
                    onPhoneChanged = viewModel::onPhoneChanged,
                    onLoginClick = viewModel::login
                )
            }
        }
    }

    @Composable
    fun LoginScreen(
        phone: String,
        isLoading: Boolean,
        onPhoneChanged: (String) -> Unit,
        onLoginClick: () -> Unit
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Qoocca",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "parents",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    )
                }
                Spacer(modifier = Modifier.height(48.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChanged,
                    label = { Text("전화번호 (예: 01012345678)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isLoading,
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("로그인", fontSize = 16.sp)
                    }
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun LoginScreenPreview() {
        QooccaParentsTheme {
            LoginScreen(
                phone = "01012345678",
                isLoading = false,
                onPhoneChanged = {},
                onLoginClick = {}
            )
        }
    }
}
