package com.qoocca.parentapp.domain.usecase

import com.qoocca.parentapp.ParentReceiptResponse
import com.qoocca.parentapp.domain.error.AppError
import com.qoocca.parentapp.domain.port.ReceiptDataSource
import com.qoocca.parentapp.domain.result.AppResult
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

data class ReceiptDetailsOutcome(
    val receipts: List<ParentReceiptResponse>,
    val hasAuthFailure: Boolean,
    val errors: List<AppError>
)

class GetReceiptDetailsUseCase(
    private val receiptDataSource: ReceiptDataSource
) {
    fun execute(token: String, receiptIds: List<Long>, onResult: (ReceiptDetailsOutcome) -> Unit) {
        if (receiptIds.isEmpty()) {
            onResult(ReceiptDetailsOutcome(emptyList(), hasAuthFailure = false, errors = emptyList()))
            return
        }

        val receipts = Collections.synchronizedList(mutableListOf<ParentReceiptResponse>())
        val errors = Collections.synchronizedList(mutableListOf<AppError>())
        val orderMap = receiptIds.withIndex().associate { it.value to it.index }
        val remaining = AtomicInteger(receiptIds.size)
        val authFailed = AtomicInteger(0)

        receiptIds.forEach { id ->
            receiptDataSource.fetchReceiptDetail(token, id) { result ->
                when (result) {
                    is AppResult.Success -> receipts.add(result.data)
                    is AppResult.Failure -> {
                        errors.add(result.error)
                        if (result.error == AppError.Unauthorized || result.error == AppError.Forbidden) {
                            authFailed.incrementAndGet()
                        }
                    }
                }

                if (remaining.decrementAndGet() == 0) {
                    val sortedReceipts = receipts.sortedBy { orderMap[it.receiptId] ?: Int.MAX_VALUE }
                    onResult(
                        ReceiptDetailsOutcome(
                            receipts = sortedReceipts,
                            hasAuthFailure = authFailed.get() > 0,
                            errors = errors.toList()
                        )
                    )
                }
            }
        }
    }
}
