package com.qoocca.parentapp

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.qoocca.parentapp.presentation.main.MainEvent
import com.qoocca.parentapp.presentation.main.MainUiState
import com.qoocca.parentapp.presentation.main.MainViewModel
import com.qoocca.parentapp.ui.theme.QooccaParentsTheme
import com.qoocca.parentapp.ui.theme.payboocFontFamily
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var paymentLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paymentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                viewModel.refresh()
            }
        }

        if (!viewModel.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        checkNotificationPermission()

        setContent {
            QooccaParentsTheme {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        if (event is MainEvent.NavigateLogin) {
                            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                            finish()
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.loadInitial()
                }

                ReceiptListScreen(
                    uiState = uiState,
                    onRefresh = viewModel::refresh,
                    onLaunchPayment = { intent -> paymentLauncher.launch(intent) }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    @Composable
    fun ReceiptListScreen(
        uiState: MainUiState,
        onRefresh: () -> Unit,
        onLaunchPayment: (Intent) -> Unit
    ) {
        val pullRefreshState = rememberPullRefreshState(uiState.isRefreshing, onRefresh = onRefresh)

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
                        IconButton(onClick = onRefresh) {
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
                if (!uiState.isLoading && uiState.error == null) {
                    ParentStatusHeader(receipts = uiState.receipts)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pullRefresh(pullRefreshState)
                ) {
                    when {
                        uiState.isLoading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        uiState.error != null -> {
                            Text(
                                text = "오류: ${uiState.error}",
                                modifier = Modifier.align(Alignment.Center),
                                color = Color.Red
                            )
                        }

                        uiState.receipts.isEmpty() -> {
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
                            val groupedReceipts = uiState.receipts.groupBy { it.academyName }

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
                        refreshing = uiState.isRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
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
