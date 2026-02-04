package com.qoocca.parentapp.presentation.common

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.TaskStackBuilder
import com.qoocca.parentapp.MainActivity
import com.qoocca.parentapp.PaymentActivity

object NotificationRouter {
    fun parseReceiptId(raw: String?): Long? {
        return raw?.toLongOrNull()?.takeIf { it > 0 }
    }

    fun createPaymentPendingIntent(context: Context, receiptId: Long?): PendingIntent? {
        val mainIntent = Intent(context, MainActivity::class.java)
        val paymentIntent = Intent(context, PaymentActivity::class.java).apply {
            putExtra("receiptId", receiptId ?: -1L)
        }

        val requestCode = (receiptId ?: System.currentTimeMillis()).toInt()

        return TaskStackBuilder.create(context)
            .addNextIntent(mainIntent)
            .addNextIntent(paymentIntent)
            .getPendingIntent(
                requestCode,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    }
}
