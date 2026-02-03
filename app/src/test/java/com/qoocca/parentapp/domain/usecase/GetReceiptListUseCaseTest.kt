package com.qoocca.parentapp.domain.usecase

import com.qoocca.parentapp.ParentReceiptResponse
import com.qoocca.parentapp.domain.port.ReceiptDataSource
import com.qoocca.parentapp.domain.result.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetReceiptListUseCaseTest {

    @Test
    fun execute_delegatesToReceiptDataSource() {
        var capturedToken: String? = null
        val expected = listOf(sampleReceipt(1L))
        val fakeDataSource = object : ReceiptDataSource {
            override fun fetchReceiptRequests(
                token: String,
                onResult: (AppResult<List<ParentReceiptResponse>>) -> Unit
            ) {
                capturedToken = token
                onResult(AppResult.Success(expected))
            }

            override fun fetchReceiptDetail(
                token: String,
                receiptId: Long,
                onResult: (AppResult<ParentReceiptResponse>) -> Unit
            ) {
                error("Not used")
            }
        }

        val useCase = GetReceiptListUseCase(fakeDataSource)
        var result: AppResult<List<ParentReceiptResponse>>? = null

        useCase.execute("abc") { result = it }

        assertEquals("abc", capturedToken)
        assertTrue(result is AppResult.Success)
        assertEquals(expected, (result as AppResult.Success).data)
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
