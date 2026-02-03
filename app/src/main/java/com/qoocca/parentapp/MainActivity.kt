package com.qoocca.parentapp

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.qoocca.parentapp.ui.theme.QooccaParentsTheme
import com.qoocca.parentapp.ui.theme.payboocFontFamily
import kotlinx.coroutines.launch
import okhttp3.*
import java.io.IOException
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val TAG = "MAIN_ACTIVITY_DEBUG"
    private val BASE_URL = ApiConfig.API_BASE_URL
    private lateinit var authManager: AuthManager
    private val client = OkHttpClient()

    private lateinit var paymentLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    private val refreshTrigger = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authManager = AuthManager(this)

        paymentLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d(TAG, "결제 성공 신호 수신 - 목록 새로고침")
                refreshTrigger.value = !refreshTrigger.value
            }
        }

        if (!authManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        checkNotificationPermission()

        setContent {
            QooccaParentsTheme {
                ReceiptListScreen(
                    refreshTrigger = refreshTrigger,
                    onLaunchPayment = { intent -> paymentLauncher.launch(intent) }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    @Composable
    fun ReceiptListScreen(
        refreshTrigger: State<Boolean>,
        onLaunchPayment: (Intent) -> Unit
    ) {
        var receipts by remember { mutableStateOf<List<ParentReceiptResponse>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var isRefreshing by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        val coroutineScope = rememberCoroutineScope()

        fun refresh() {
            coroutineScope.launch {
                isRefreshing = true
                fetchReceipts(
                    onSuccess = {
                        receipts = it
                        error = null
                        isRefreshing = false
                        isLoading = false
                    },
                    onError = {
                        error = it
                        isRefreshing = false
                        isLoading = false
                    }
                )
            }
        }

        LaunchedEffect(refreshTrigger.value) {
            refresh()
        }

        // 최초 진입 로딩
        LaunchedEffect(Unit) {
            fetchReceipts(
                onSuccess = {
                    receipts = it
                    isLoading = false
                },
                onError = {
                    error = it
                    isLoading = false
                }
            )
        }

        val pullRefreshState = rememberPullRefreshState(isRefreshing, onRefresh = ::refresh)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Qoocca",
                            fontFamily = payboocFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = ::refresh) {
                            Icon(Icons.Filled.Refresh, contentDescription = "새로고침")
                        }
                        IconButton(onClick = { /* TODO: 알림 화면 이동 */ }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "알림")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (!isLoading && error == null) {
                    ParentStatusHeader(receipts = receipts)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pullRefresh(pullRefreshState)
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        error != null -> {
                            Text(
                                text = "오류: $error",
                                modifier = Modifier.align(Alignment.Center),
                                color = Color.Red
                            )
                        }

                        receipts.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 50.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "모든 요청을 처리했어요",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        else -> {
                            val groupedReceipts = receipts.groupBy { it.academyName }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 20.dp,
                                    top = 8.dp,
                                    end = 20.dp,
                                    bottom = 16.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                groupedReceipts.forEach { (academyName, academyReceipts) ->
                                    item(key = academyName) {
                                        AcademyGroup(
                                            academyName = academyName,
                                            academyReceipts = academyReceipts,
                                            onLaunchPayment = onLaunchPayment
                                        )
                                    }
                                }
                            }
                        }
                    }

                    PullRefreshIndicator(
                        refreshing = isRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }

    private fun fetchReceipts(
        onSuccess: (List<ParentReceiptResponse>) -> Unit,
        onError: (String) -> Unit
    ) {
        val token = authManager.getToken()
        if (token == null) {
            onError("로그인 토큰을 찾을 수 없습니다. 다시 로그인해주세요.")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val request = Request.Builder()
            .url("$BASE_URL/api/parent/receipt/requests")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "결제 목록 조회 실패: ${e.message}")
                runOnUiThread { onError("서버 연결에 실패했습니다.") }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (response.isSuccessful && responseData != null) {
                    try {
                        val gson = Gson()
                        val receiptListType =
                            object : TypeToken<List<ParentReceiptResponse>>() {}.type
                        val receiptList: List<ParentReceiptResponse> =
                            gson.fromJson(responseData, receiptListType)
                        runOnUiThread { onSuccess(receiptList) }
                    } catch (e: Exception) {
                        Log.e(TAG, "응답 파싱 에러: ${e.message}")
                        runOnUiThread { onError("데이터를 처리하는 중 오류가 발생했습니다.") }
                    }
                } else {
                    Log.e(TAG, "결제 목록 조회 에러: ${response.code} - $responseData")
                    if (response.code == 403) {
                        runOnUiThread { onError("인증에 실패했습니다. 다시 로그인 해주세요.") }
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        runOnUiThread { onError("결제 목록을 가져오지 못했습니다. (코드: ${response.code})") }
                    }
                }
            }
        })
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }
}

@Composable
fun ParentStatusHeader(receipts: List<ParentReceiptResponse>) {
    val studentNames = receipts.map { it.studentName }.distinct().joinToString(", ")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (receipts.isNotEmpty()) "$studentNames 학부모님," else "학부모님, 안녕하세요!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (receipts.isNotEmpty()) {
                        "새로운 결제 요청이 ${receipts.size}건 도착했습니다."
                    } else {
                        "현재 확인하실 결제 내역이 없어요."
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = payboocFontFamily,
                        lineHeight = 30.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.4f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (receipts.isNotEmpty()) "🔔" else "✨",
                        fontSize = 28.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AcademyGroup(
    academyName: String,
    academyReceipts: List<ParentReceiptResponse>,
    onLaunchPayment: (Intent) -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = academyName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )

                if (academyReceipts.size > 1) {
                    Button(
                        onClick = {
                            val receiptIds = academyReceipts.map { it.receiptId }.toLongArray()
                            val intent = Intent(context, PaymentActivity::class.java).apply {
                                putExtra("receiptIds", receiptIds)
                            }
                            onLaunchPayment(intent)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("전체 결제", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))

            Column {
                academyReceipts.forEachIndexed { index, receipt ->
                    ReceiptItem(
                        receipt = receipt,
                        onLaunchPayment = onLaunchPayment
                    )

                    if (index < academyReceipts.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptItem(
    receipt: ParentReceiptResponse,
    onLaunchPayment: (Intent) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = receipt.studentName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Surface(
                color = Color(0xFFFFF4F4),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "결제 대기",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE53935),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = null,
                    tint = Color(0xFF999999),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = receipt.className, color = Color(0xFF666666), fontSize = 13.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AccessTime,
                    contentDescription = null,
                    tint = Color(0xFF999999),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val formattedTime = try {
                    LocalDateTime.parse(receipt.receiptDate)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                } catch (e: Exception) {
                    receipt.receiptDate
                }
                Text(text = "$formattedTime 요청", color = Color(0xFF999999), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${NumberFormat.getNumberInstance(Locale.KOREA).format(receipt.amount)}원",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = payboocFontFamily
            )

            OutlinedButton(
                onClick = {
                    val intent = Intent(context, PaymentActivity::class.java).apply {
                        putExtra("receiptId", receipt.receiptId)
                    }
                    onLaunchPayment(intent)
                },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("결제", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReceiptListScreenPreview() {
    val previewReceipts = listOf(
        ParentReceiptResponse(1, 101, "김민준", 1, "심화반", "명인학원", 350000, LocalDateTime.now().toString(), "PENDING"),
        ParentReceiptResponse(2, 102, "이서연", 2, "오후반", "예일음악학원", 180000, LocalDateTime.now().minusDays(1).toString(), "PENDING")
    )
    QooccaParentsTheme {
        Column {
            ParentStatusHeader(receipts = previewReceipts)
            LazyColumn(contentPadding = PaddingValues(16.dp)) {
                item {
                    AcademyGroup(
                        academyName = "명인학원",
                        academyReceipts = previewReceipts.filter { it.academyName == "명인학원" },
                        onLaunchPayment = {}
                    )
                }
            }
        }
    }
}
