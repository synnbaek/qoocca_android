package com.qoocca.parentapp.domain.usecase

import com.qoocca.parentapp.domain.error.AppError
import com.qoocca.parentapp.domain.port.PaymentDataSource
import com.qoocca.parentapp.domain.result.AppResult
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

data class PayReceiptsOutcome(
    val failedCount: Int,
    val hasAuthFailure: Boolean,
    val errors: List<AppError>
)

class PayReceiptsUseCase(
    private val paymentDataSource: PaymentDataSource
) {
    fun execute(token: String, receiptIds: List<Long>, onResult: (PayReceiptsOutcome) -> Unit) {
        if (receiptIds.isEmpty()) {
            onResult(PayReceiptsOutcome(failedCount = 0, hasAuthFailure = false, errors = emptyList()))
            return
        }

        val failedCount = AtomicInteger(0)
        val authFailed = AtomicInteger(0)
        val remaining = AtomicInteger(receiptIds.size)
        val errors = Collections.synchronizedList(mutableListOf<AppError>())

        receiptIds.forEach { id ->
            paymentDataSource.payReceipt(token, id) { result ->
                when (result) {
                    is AppResult.Success -> Unit
                    is AppResult.Failure -> {
                        errors.add(result.error)
                        failedCount.incrementAndGet()
                        if (result.error == AppError.Unauthorized || result.error == AppError.Forbidden) {
                            authFailed.incrementAndGet()
                        }
                    }
                }

                if (remaining.decrementAndGet() == 0) {
                    onResult(
                        PayReceiptsOutcome(
                            failedCount = failedCount.get(),
                            hasAuthFailure = authFailed.get() > 0,
                            errors = errors.toList()
                        )
                    )
                }
            }
        }
    }
}
