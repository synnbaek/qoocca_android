package com.qoocca.parentapp.domain.usecase

import com.qoocca.parentapp.ParentReceiptResponse
import com.qoocca.parentapp.domain.error.AppError
import com.qoocca.parentapp.domain.port.ReceiptDataSource
import com.qoocca.parentapp.domain.result.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetReceiptDetailsUseCaseTest {

    @Test
    fun execute_returnsSortedReceiptsAndAuthFailureFlag() {
        val fakeDataSource = object : ReceiptDataSource {
            override fun fetchReceiptRequests(
                token: String,
                onResult: (AppResult<List<ParentReceiptResponse>>) -> Unit
            ) {
                error("Not used")
            }

            override fun fetchReceiptDetail(
                token: String,
                receiptId: Long,
                onResult: (AppResult<ParentReceiptResponse>) -> Unit
            ) {
                when (receiptId) {
                    10L -> onResult(AppResult.Success(sampleReceipt(10L)))
                    20L -> onResult(AppResult.Failure(AppError.Unauthorized))
                    30L -> onResult(AppResult.Success(sampleReceipt(30L)))
                    else -> onResult(AppResult.Failure(AppError.Unknown(null)))
                }
            }
        }

        val useCase = GetReceiptDetailsUseCase(fakeDataSource)
        var outcome: ReceiptDetailsOutcome? = null

        useCase.execute("token", listOf(30L, 20L, 10L)) { outcome = it }

        val result = outcome ?: error("outcome should not be null")
        assertTrue(result.hasAuthFailure)
        assertEquals(listOf(30L, 10L), result.receipts.map { it.receiptId })
        assertEquals(1, result.errors.size)
    }

    @Test
    fun execute_withEmptyIds_returnsEmptyOutcome() {
        val fakeDataSource = object : ReceiptDataSource {
            override fun fetchReceiptRequests(
                token: String,
                onResult: (AppResult<List<ParentReceiptResponse>>) -> Unit
            ) = error("Not used")

            override fun fetchReceiptDetail(
                token: String,
                receiptId: Long,
                onResult: (AppResult<ParentReceiptResponse>) -> Unit
            ) = error("Not used")
        }

        val useCase = GetReceiptDetailsUseCase(fakeDataSource)
        var outcome: ReceiptDetailsOutcome? = null

        useCase.execute("token", emptyList()) { outcome = it }

        val result = outcome ?: error("outcome should not be null")
        assertFalse(result.hasAuthFailure)
        assertTrue(result.receipts.isEmpty())
        assertTrue(result.errors.isEmpty())
    }

    private fun sampleReceipt(id: Long): ParentReceiptResponse {
        return ParentReceiptResponse(
            receiptId = id,
            studentId = 1L,
            studentName = "학생",
            classId = 2L,
            className = "반",
            academyName = "학원",
            amount = 10000L,
            receiptDate = "2026-01-01T10:00:00",
            receiptStatus = "PENDING"
        )
    }
}
