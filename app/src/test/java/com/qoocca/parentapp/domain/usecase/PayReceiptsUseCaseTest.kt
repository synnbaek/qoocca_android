package com.qoocca.parentapp.domain.usecase

import com.qoocca.parentapp.domain.error.AppError
import com.qoocca.parentapp.domain.port.PaymentDataSource
import com.qoocca.parentapp.domain.result.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PayReceiptsUseCaseTest {

    @Test
    fun execute_aggregatesFailuresAndAuthFailure() {
        val fakeDataSource = object : PaymentDataSource {
            override fun payReceipt(token: String, receiptId: Long, onResult: (AppResult<Unit>) -> Unit) {
                when (receiptId) {
                    1L -> onResult(AppResult.Success(Unit))
                    2L -> onResult(AppResult.Failure(AppError.Forbidden))
                    3L -> onResult(AppResult.Failure(AppError.Network))
                    else -> onResult(AppResult.Success(Unit))
                }
            }
        }

        val useCase = PayReceiptsUseCase(fakeDataSource)
        var outcome: PayReceiptsOutcome? = null

        useCase.execute("token", listOf(1L, 2L, 3L)) { outcome = it }

        val result = outcome ?: error("outcome should not be null")
        assertEquals(2, result.failedCount)
        assertTrue(result.hasAuthFailure)
        assertEquals(2, result.errors.size)
    }

    @Test
    fun execute_withEmptyIds_returnsZeroFailure() {
        val fakeDataSource = object : PaymentDataSource {
            override fun payReceipt(token: String, receiptId: Long, onResult: (AppResult<Unit>) -> Unit) {
                error("Not used")
            }
        }

        val useCase = PayReceiptsUseCase(fakeDataSource)
        var outcome: PayReceiptsOutcome? = null

        useCase.execute("token", emptyList()) { outcome = it }

        val result = outcome ?: error("outcome should not be null")
        assertEquals(0, result.failedCount)
        assertFalse(result.hasAuthFailure)
        assertTrue(result.errors.isEmpty())
    }
}
