package com.qoocca.parentapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.qoocca.parentapp.data.network.ApiResult
import com.qoocca.parentapp.data.repository.FcmRepository

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FCM_SERVICE"
    private val CHANNEL_ID = "payment_channel"
    private val fcmRepository = FcmRepository()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New token generated: $token")
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "Message received from: ${message.from}")
        Log.d(TAG, "Payload data: ${message.data}")

        if (message.data.isNotEmpty()) {
            val title = message.data["title"] ?: "결제 요청"
            val body = message.data["body"] ?: "새로운 결제 요청이 도착했습니다."
            val receiptId = message.data["receiptId"]

            sendNotification(title, body, receiptId)
        }
    }

    private fun sendNotification(title: String, body: String, receiptId: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "결제 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "결제 요청 및 영수증 관련 알림을 수신합니다."
            }
            notificationManager.createNotificationChannel(channel)
        }

        // MainActivity를 먼저 열고 PaymentActivity로 이동하도록 설정
        val paymentIntent = Intent(this, PaymentActivity::class.java).apply {
            putExtra("receiptId", receiptId?.toLongOrNull() ?: -1L)
        }

        val pendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(paymentIntent)
            getPendingIntent(
                System.currentTimeMillis().toInt(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun sendTokenToServer(token: String) {
        val authManager = AuthManager(applicationContext)
        val parentId = authManager.getParentId()

        if (parentId == -1L) {
            Log.e(TAG, "parentId not found, skip FCM registration")
            return
        }

        fcmRepository.registerToken(parentId, token) { result ->
            when (result) {
                is ApiResult.Success -> Log.d(TAG, "서버 전송 결과: success")
                is ApiResult.HttpError -> Log.e(TAG, "서버 전송 실패: ${result.code} - ${result.body}")
                is ApiResult.NetworkError -> Log.e(TAG, "서버 전송 실패: ${result.exception.message}")
                is ApiResult.UnknownError -> Log.e(TAG, "서버 전송 실패: ${result.exception.message}")
            }
        }
    }
}
