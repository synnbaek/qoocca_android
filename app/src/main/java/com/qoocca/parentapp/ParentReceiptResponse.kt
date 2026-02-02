package com.qoocca.parentapp

import java.time.LocalDateTime

data class ParentReceiptResponse(
    val receiptId: Long,
    val studentId: Long,
    val studentName: String,
    val classId: Long,
    val className: String,
    val academyName: String,
    val amount: Long,
    val receiptDate: String,
    val receiptStatus: String
)
