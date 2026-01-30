package com.qoocca.parentapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class ReceiptDetailActivity : ComponentActivity() {
    private val TAG = "RECEIPT_DEBUG"
    private val BASE_URL = "http://10.0.2.2:8080"
    private lateinit var authManager: AuthManager
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = AuthManager(this)
        
        // Intent에서 receiptId 추출
        val receiptId = intent.getStringExtra("RECEIPT_ID") ?: ""
        Log.d(TAG, "전달받은 ReceiptId: $receiptId")

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ReceiptDetailScreen(receiptId)
                }
            }
        }
    }

    @Composable
    fun ReceiptDetailScreen(receiptId: String) {
        var isProcessing by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("결제 요청 상세", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (receiptId.isBlank()) {
                Text("영수증 정보를 찾을 수 없습니다.", color = MaterialTheme.colorScheme.error)
            } else {
                Text("영수증 ID: $receiptId", style = MaterialTheme.typography.bodyLarge)
            }
            
            Spacer(modifier = Modifier.height(48.dp))

            if (isProcessing) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { handleAction(receiptId, "pay") { isProcessing = it } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = receiptId.isNotBlank()
                ) {
                    Text("결제하기")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = { handleAction(receiptId, "cancel") { isProcessing = it } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = receiptId.isNotBlank()
                ) {
                    Text("취소하기")
                }
            }
        }
    }

    private fun handleAction(receiptId: String, action: String, setLoading: (Boolean) -> Unit) {
        val parentId = authManager.getParentId()
        if (parentId == -1L) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        // API: POST /api/receipt/{receiptId}/{action}?parentId=...
        val url = "$BASE_URL/api/receipt/$receiptId/$action".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("parentId", parentId.toString())
            ?.build() ?: return

        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody())
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    setLoading(false)
                    Toast.makeText(this@ReceiptDetailActivity, "네트워크 오류", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                runOnUiThread {
                    setLoading(false)
                    if (response.isSuccessful) {
                        val msg = if (action == "pay") "결제가 완료되었습니다." else "요청이 취소되었습니다."
                        Toast.makeText(this@ReceiptDetailActivity, msg, Toast.LENGTH_LONG).show()
                        finish() // 처리 성공 시 화면 닫기
                    } else {
                        Log.e(TAG, "에러 응답: ${response.code} - $responseBody")
                        val errorMsg = if (response.code == 400) "이미 처리되었거나 유효하지 않은 요청입니다." else "에러: ${response.code}"
                        Toast.makeText(this@ReceiptDetailActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}
