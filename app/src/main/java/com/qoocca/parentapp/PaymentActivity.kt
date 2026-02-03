package com.qoocca.parentapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.qoocca.parentapp.ui.theme.QooccaParentsTheme
import com.qoocca.parentapp.ui.theme.payboocFontFamily
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.IOException
import java.text.NumberFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PaymentActivity : ComponentActivity() {
    private val TAG = "PAYMENT_ACTIVITY_DEBUG"
    private val BASE_URL = ApiConfig.API_BASE_URL
    private val client = OkHttpClient()
    private lateinit var authManager: AuthManager

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = AuthManager(this)

        val receiptId = intent.getLongExtra("receiptId", -1L)
        val receiptIds = intent.getLongArrayExtra("receiptIds")

        setContent {
            QooccaParentsTheme {
                val coroutineScope = rememberCoroutineScope()
                var receipts by remember { mutableStateOf<List<ParentReceiptResponse>>(emptyList()) }
                var isLoading by remember { mutableStateOf(false) }
                var infoMessage by remember { mutableStateOf<String?>(null) }
                var errorMessage by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(receiptId, receiptIds) {
                    isLoading = true
                    val idsToFetch = receiptIds?.toList() ?: (if (receiptId != -1L) listOf(receiptId) else emptyList())

                    if (idsToFetch.isNotEmpty()) {
                        fetchAllReceiptDetails(idsToFetch) {
                            receipts = it
                            isLoading = false
                        }
                    } else {
                        isLoading = false
                        errorMessage = "결제 정보를 찾을 수 없습니다."
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("결제하기", fontWeight = FontWeight.Bold, fontFamily = payboocFontFamily) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로가기", tint = Color.White)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                titleContentColor = Color.White
                            )
                        )
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        if (isLoading && receipts.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (receipts.isNotEmpty()) {
                            PaymentScreen(
                                modifier = Modifier,
                                receipts = receipts,
                                isLoading = isLoading,
                                onConfirmPayment = {
                                    isLoading = true
                                    coroutineScope.launch {
                                        val results = completeAllPayments(receipts.map { it.receiptId })
                                        val failedCount = results.count { !it }
                                        isLoading = false
                                        if (failedCount == 0) {
                                            infoMessage = "결제가 성공적으로 완료되었습니다."
                                            setResult(RESULT_OK)
                                            delay(2000)
                                            finish()
                                        } else {
                                            errorMessage = "${failedCount}건의 결제에 실패했습니다. 다시 시도해주세요."
                                            delay(3000)
                                            errorMessage = null
                                        }
                                    }
                                }
                            )
                        } else {
                             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(errorMessage ?: "결제 정보를 불러오는데 실패했습니다.")
                            }
                        }

                        val message = infoMessage ?: errorMessage
                        val isError = errorMessage != null

                        AnimatedVisibility(
                            visible = message != null,
                            modifier = Modifier.align(Alignment.TopCenter),
                            enter = slideInVertically(initialOffsetY = { -it }),
                            exit = slideOutVertically(targetOffsetY = { -it })
                        ) {
                            val backgroundColor = when {
                                isError -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.tertiaryContainer
                            }
                            val textColor = when {
                                isError -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onTertiaryContainer
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = backgroundColor
                            ) {
                                Text(
                                    text = message.orEmpty(),
                                    modifier = Modifier.padding(16.dp),
                                    color = textColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fetchAllReceiptDetails(receiptIds: List<Long>, onResult: (List<ParentReceiptResponse>) -> Unit) {
        val token = authManager.getToken() ?: return
        val coroutineScope = CoroutineScope(Dispatchers.IO)

        coroutineScope.launch {
            val deferreds = receiptIds.map { id ->
                async {
                    val request = Request.Builder()
                        .url("$BASE_URL/api/parent/receipt/$id")
                        .header("Authorization", "Bearer $token")
                        .build()

                    try {
                        val response = client.newCall(request).execute()
                        val responseData = response.body?.string()
                        if (response.isSuccessful && responseData != null) {
                            Gson().fromJson(responseData, ParentReceiptResponse::class.java)
                        } else {
                            null
                        }
                    } catch (e: IOException) {
                        null
                    }
                }
            }
            val results = deferreds.awaitAll().filterNotNull()
            withContext(Dispatchers.Main) {
                onResult(results)
            }
        }
    }


    private suspend fun completePayment(receiptId: Long): Boolean = suspendCoroutine {
        val token = authManager.getToken()
        if (token == null) {
            it.resume(false)
            return@suspendCoroutine
        }

        val request = Request.Builder()
            .url("$BASE_URL/api/receipt/$receiptId/pay")
            .header("Authorization", "Bearer $token")
            .post(FormBody.Builder().build())
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "결제 실패: ${e.message}")
                it.resume(false)
            }

            override fun onResponse(call: Call, response: Response) {
                it.resume(response.isSuccessful)
            }
        })
    }

    private suspend fun completeAllPayments(receiptIds: List<Long>): List<Boolean> = coroutineScope {
        receiptIds.map { id ->
            async { completePayment(id) }
        }.awaitAll()
    }
}


data class CardInfo(
    val name: String,
    val gradient: Brush,
    val textColor: Color
)

@Composable
fun PaymentScreen(
    modifier: Modifier = Modifier,
    receipts: List<ParentReceiptResponse>,
    isLoading: Boolean,
    onConfirmPayment: () -> Unit
) {
    var selectedCard by remember { mutableStateOf("KB국민카드") }
    val cards = listOf(
        CardInfo(
            "KB국민카드",
            Brush.horizontalGradient(listOf(Color(0xFF6A737C), Color(0xFFF6BE00))),
            Color.White
        ),
        CardInfo(
            "신한카드",
            Brush.horizontalGradient(listOf(Color(0xFF2055A8), Color(0xFFFFFFFF))),
            Color.White
        ),
        CardInfo(
            "삼성카드",
            Brush.horizontalGradient(listOf(Color(0xFF007BC3), Color(0xFFFFFFFF))),
            Color.White
        ),
         CardInfo(
            "우리카드",
            Brush.horizontalGradient(listOf(Color(0xFF007BC2), Color(0xFFFFFFFF))),
            Color.White
        )
    )

    val totalAmount = receipts.sumOf { it.amount }
    val academyName = receipts.firstOrNull()?.academyName ?: ""
    val studentNames = receipts.map { it.studentName }.distinct().joinToString(", ")


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "아래 정보를 확인하고 결제를 완료해주세요.",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = payboocFontFamily
        )
        Spacer(modifier = Modifier.height(32.dp))

        // 결제 정보 요약
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = if (receipts.size > 1) "$academyName 외 ${receipts.size -1}건" else "$academyName 결제",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = payboocFontFamily
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("학생", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(studentNames, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("총 결제 금액", style = MaterialTheme.typography.titleMedium, fontFamily = payboocFontFamily)
                    Text(
                        text = "${NumberFormat.getNumberInstance(Locale.KOREA).format(totalAmount)}원",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = payboocFontFamily
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 결제 수단 선택
        Text(
            text = "결제 수단 선택",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = payboocFontFamily
        )
        Spacer(modifier = Modifier.height(16.dp))

        cards.forEach { card ->
            val isSelected = selectedCard == card.name
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { selectedCard = card.name },
                shape = RoundedCornerShape(8.dp),
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(card.gradient)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = card.name,
                        color = card.textColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = payboocFontFamily
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // 결제 버튼
        Button(
            onClick = onConfirmPayment,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
            } else {
                Text("결제하기", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = payboocFontFamily)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PaymentScreenPreview() {
    val previewReceipts = listOf(
        ParentReceiptResponse(1, 101, "김민준", 1, "심화반", "명인학원", 350000, "", "PENDING"),
        ParentReceiptResponse(2, 102, "김민준", 2, "오후반", "명인학원", 180000, "", "PENDING")
    )

    QooccaParentsTheme {
        PaymentScreen(
            receipts = previewReceipts, 
            isLoading = false, 
            onConfirmPayment = {}
        )
    }
}
