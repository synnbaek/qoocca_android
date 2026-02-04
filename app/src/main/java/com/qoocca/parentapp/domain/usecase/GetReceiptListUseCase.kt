package com.qoocca.parentapp.domain.usecase

import com.qoocca.parentapp.ParentReceiptResponse
import com.qoocca.parentapp.domain.port.ReceiptDataSource
import com.qoocca.parentapp.domain.result.AppResult

class GetReceiptListUseCase(
    private val receiptDataSource: ReceiptDataSource
) {
    fun execute(token: String, onResult: (AppResult<List<ParentReceiptResponse>>) -> Unit) {
        receiptDataSource.fetchReceiptRequests(token, onResult)
    }
}
