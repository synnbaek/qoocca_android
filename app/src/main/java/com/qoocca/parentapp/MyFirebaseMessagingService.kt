package com.qoocca.parentapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.qoocca.parentapp.data.network.ApiResult
import com.qoocca.parentapp.data.repository.FcmRepository
import com.qoocca.parentapp.presentation.common.AppLogger
import com.qoocca.parentapp.presentation.common.NotificationDeduplicator
import com.qoocca.parentapp.presentation.common.NotificationRouter

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val tag = "FCM_SERVICE"
    private val channelId = "payment_channel"
    private val fcmRepository = FcmRepository()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        AppLogger.d(tag, "New token generated: ${AppLogger.maskToken(token)}")
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        AppLogger.d(tag, "Message received from: ${message.from}")
        AppLogger.d(tag, "Payload keys: ${message.data.keys}")

        if (message.data.isEmpty()) return

        val title = message.data["title"] ?: "결제 요청"
        val body = message.data["body"] ?: "새로운 결제 요청이 도착했습니다."
        val receiptId = NotificationRouter.parseReceiptId(message.data["receiptId"])

        if (receiptId != null && NotificationDeduplicator.shouldSuppress(receiptId)) {
            AppLogger.d(tag, "Duplicate notification suppressed for receiptId=$receiptId")
            return
        }

        sendNotification(title, body, receiptId)
    }

    private fun sendNotification(title: String, body: String, receiptId: Long?) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(notificationManager)

        val pendingIntent = NotificationRouter.createPaymentPendingIntent(this, receiptId)
        val notificationId = (receiptId ?: System.currentTimeMillis()).toInt()

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "결제 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "결제 요청 및 영수증 관련 알림을 수신합니다."
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendTokenToServer(token: String) {
        val authManager = AuthManager(applicationContext)
        val parentId = authManager.getParentId()

        if (parentId == -1L) {
            AppLogger.e(tag, "parentId not found, skip FCM registration")
            return
        }

        fcmRepository.registerToken(parentId, token) { result ->
            when (result) {
                is ApiResult.Success -> AppLogger.d(tag, "FCM token registered")
                is ApiResult.HttpError -> AppLogger.e(tag, "FCM register failed: ${result.code}")
                is ApiResult.NetworkError -> AppLogger.e(tag, "FCM register network error: ${result.exception.message}")
                is ApiResult.UnknownError -> AppLogger.e(tag, "FCM register unknown error: ${result.exception.message}")
            }
        }
    }
}
