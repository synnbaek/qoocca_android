package com.qoocca.parentapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import com.qoocca.parentapp.presentation.payment.PaymentEvent
import com.qoocca.parentapp.presentation.payment.PaymentUiState
import com.qoocca.parentapp.presentation.payment.PaymentViewModel
import com.qoocca.parentapp.ui.theme.QooccaParentsTheme
import com.qoocca.parentapp.ui.theme.payboocFontFamily
import java.text.NumberFormat
import java.util.Locale

class PaymentActivity : ComponentActivity() {

    private val viewModel: PaymentViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val receiptId = intent.getLongExtra("receiptId", -1L)
        val receiptIds = intent.getLongArrayExtra("receiptIds")

        setContent {
            QooccaParentsTheme {
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            PaymentEvent.PaymentSuccess -> {
                                setResult(RESULT_OK)
                                finish()
                            }

                            PaymentEvent.NavigateLogin -> {
                                startActivity(Intent(this@PaymentActivity, LoginActivity::class.java))
                                finish()
                            }
                        }
                    }
                }

                LaunchedEffect(receiptId, receiptIds?.contentHashCode()) {
                    viewModel.initialize(receiptId, receiptIds)
                }

                PaymentContent(
                    uiState = uiState,
                    onBack = { finish() },
                    onConfirmPayment = viewModel::confirmPayments
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PaymentContent(
        uiState: PaymentUiState,
        onBack: () -> Unit,
        onConfirmPayment: () -> Unit
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "결제하기",
                            fontWeight = FontWeight.Bold,
                            fontFamily = payboocFontFamily
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "뒤로가기",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (uiState.isLoading && uiState.receipts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.receipts.isNotEmpty()) {
                    PaymentScreen(
                        modifier = Modifier,
                        receipts = uiState.receipts,
                        isLoading = uiState.isLoading,
                        onConfirmPayment = onConfirmPayment
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(uiState.errorMessage ?: "결제 정보를 불러오는데 실패했습니다.")
                    }
                }

                val message = uiState.infoMessage ?: uiState.errorMessage
                val isError = uiState.errorMessage != null

                AnimatedVisibility(
                    visible = message != null,
                    modifier = Modifier.align(Alignment.TopCenter),
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it })
                ) {
                    val backgroundColor = if (isError) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer
                    }
                    val textColor = if (isError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onTertiaryContainer
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
