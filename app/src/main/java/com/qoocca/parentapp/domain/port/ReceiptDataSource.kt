package com.qoocca.parentapp.domain.port

import com.qoocca.parentapp.ParentReceiptResponse
import com.qoocca.parentapp.domain.result.AppResult

interface ReceiptDataSource {
    fun fetchReceiptRequests(token: String, onResult: (AppResult<List<ParentReceiptResponse>>) -> Unit)
    fun fetchReceiptDetail(token: String, receiptId: Long, onResult: (AppResult<ParentReceiptResponse>) -> Unit)
}
