package com.qoocca.parentapp.domain.port

import com.qoocca.parentapp.domain.result.AppResult

interface PaymentDataSource {
    fun payReceipt(token: String, receiptId: Long, onResult: (AppResult<Unit>) -> Unit)
}
