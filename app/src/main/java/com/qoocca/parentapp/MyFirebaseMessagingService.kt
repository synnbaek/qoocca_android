package com.qoocca.parentapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "FCM_SERVICE"
    private val BASE_URL = "http://10.0.2.2:8080/api/fcm/register"
    private val CHANNEL_ID = "payment_channel"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New token generated: $token")
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        // 1. Data-only payload 수신 로그
        Log.d(TAG, "Message received from: ${message.from}")
        Log.d(TAG, "Payload data: ${message.data}")

        if (message.data.isNotEmpty()) {
            // 2. 백엔드에서 보낸 data 필드 추출
            val receiptId = message.data["receiptId"]
            val title = message.data["title"] ?: "결제 요청"
            val body = message.data["body"] ?: "새로운 결제 요청이 도착했습니다."

            Log.d(TAG, "Parsed -> ID: $receiptId, Title: $title, Body: $body")
            
            // 3. 직접 알림 생성 (포그라운드/백그라운드/종료 상태 모두 대응)
            sendNotification(title, body, receiptId)
        }
    }

    private fun sendNotification(title: String, body: String, receiptId: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 4. 알림 채널 설정 (Android 8.0 이상 필수)
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

        // 5. 알림 클릭 시 ReceiptDetailActivity로 이동 (receiptId 포함)
        val intent = Intent(this, ReceiptDetailActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("RECEIPT_ID", receiptId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            System.currentTimeMillis().toInt(), 
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // 6. 알림 빌드 및 표시
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

        val client = OkHttpClient()
        val url = BASE_URL.toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("parentId", parentId.toString())
            ?.addQueryParameter("fcmToken", token)
            ?.build() ?: return

        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody())
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "서버 전송 실패: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                Log.d(TAG, "서버 전송 결과: ${response.code}")
            }
        })
    }
}
