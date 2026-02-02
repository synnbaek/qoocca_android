package com.qoocca.parentapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.messaging.FirebaseMessaging
import com.qoocca.parentapp.ui.theme.QooccaParentsTheme
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : ComponentActivity() {
    private val TAG = "LOGIN_DEBUG"
    private val BASE_URL = "http://10.0.2.2:8080"
    private lateinit var authManager: AuthManager
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = AuthManager(this)

        setContent {
            QooccaParentsTheme {
                LoginScreen()
            }
        }
    }

    @Composable
    fun LoginScreen() {
        var phone by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

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
                    onValueChange = { phone = it },
                    label = { Text("전화번호 (예: 01012345678)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { performLogin(phone) { isLoading = it } },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
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

    private fun performLogin(phone: String, setLoading: (Boolean) -> Unit) {
        if (phone.isBlank()) {
            Toast.makeText(this, "전화번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        val json = JSONObject().apply {
            put("parentPhone", phone)
        }
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL/api/parent/auth/login")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    setLoading(false)
                    Log.e(TAG, "로그인 실패: ${e.message}")
                    Toast.makeText(this@LoginActivity, "서버 연결 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    setLoading(false)
                    if (response.isSuccessful && responseData != null) {
                        try {
                            val jsonResponse = JSONObject(responseData)
                            val parentId = jsonResponse.getLong("parentId")
                            val accessToken = jsonResponse.getString("accessToken")
                            val parentName = jsonResponse.getString("parentName")

                            Log.d(TAG, "로그인 성공: $parentName (ID: $parentId)")
                            authManager.saveAuthData(parentId, accessToken)

                            // 로그인 성공 후 즉시 FCM 토큰 등록
                            registerFcmToken(parentId)

                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        } catch (e: Exception) {
                            Log.e(TAG, "응답 파싱 에러: ${e.message}")
                            Toast.makeText(this@LoginActivity, "잘못된 서버 응답", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e(TAG, "로그인 에러: ${response.code} - $responseData")
                        Toast.makeText(this@LoginActivity, "로그인 실패 (번호 확인)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
    
    private fun registerFcmToken(parentId: Long) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                sendTokenToServer(parentId, token)
            }
        }
    }

    private fun sendTokenToServer(parentId: Long, token: String) {
        val url = "$BASE_URL/api/fcm/register".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("parentId", parentId.toString())
            ?.addQueryParameter("fcmToken", token)
            ?.build()

        if (url == null) {
            Log.e(TAG, "URL 파싱 실패")
            return
        }

        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody())
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "FCM 토큰 등록 실패: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                Log.d(TAG, "FCM 토큰 등록 완료: ${response.code}")
            }
        })
    }

    @Preview(showBackground = true)
    @Composable
    fun LoginScreenPreview() {
        QooccaParentsTheme {
            LoginScreen()
        }
    }
}
