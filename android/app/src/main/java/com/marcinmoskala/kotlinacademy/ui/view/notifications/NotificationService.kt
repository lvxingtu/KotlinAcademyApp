package com.marcinmoskala.kotlinacademy.ui.view.notifications

import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.support.v4.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.marcinmoskala.kotlinacademy.R
import com.marcinmoskala.kotlinacademy.ui.common.notificationManager
import com.marcinmoskala.kotlinacademy.ui.view.news.NewsActivityStarter


class NotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        sendNotification(remoteMessage.notification?.body.orEmpty())
    }

    private fun sendNotification(message: String) {
        val intent = NewsActivityStarter.getIntent(this).apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val notification = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.icon_notification)
                .setContentTitle(getString(R.string.portal_name))
                .setContentText(message)
                .setAutoCancel(true )
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent)
                .build()

        notificationManager.notify(1234, notification)
    }
}