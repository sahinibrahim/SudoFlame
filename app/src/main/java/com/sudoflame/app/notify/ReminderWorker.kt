package com.sudoflame.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sudoflame.app.MainActivity
import com.sudoflame.app.R
import com.sudoflame.app.data.ProgressRepository
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Her akşam 19:00 civarı: bugün oynamadıysan seri hatırlatması gönderir. */
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        ProgressRepository.init(applicationContext)
        ProgressRepository.refreshDay()
        val p = ProgressRepository.current
        if (!p.reminder || p.playedToday) return Result.success()

        val (title, text) = when {
            p.streak >= 1 -> "🔥 ${p.streak} günlük serin tehlikede!" to "Bir bulmaca çöz, alevi söndürme. Sadece birkaç dakika!"
            p.canRepairStreak -> "😢 Serin söndü…" to "Hemen gel, bugün serini geri kurtarabilirsin!"
            else -> "🧩 Günün bulmacası seni bekliyor" to "Yeni bir seri başlatmanın tam zamanı."
        }
        show(applicationContext, title, text)
        return Result.success()
    }

    companion object {
        const val CHANNEL = "streak"
        private const val WORK = "daily_reminder"

        fun createChannel(ctx: Context) {
            val ch = NotificationChannel(CHANNEL, ctx.getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT)
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }

        fun schedule(ctx: Context) {
            val now = LocalDateTime.now()
            var next = now.withHour(19).withMinute(0).withSecond(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val delay = Duration.between(now, next).toMinutes()
            val req = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
        }

        fun show(ctx: Context, title: String, text: String) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
                android.os.Build.VERSION.SDK_INT >= 33
            ) return
            val intent = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val n = NotificationCompat.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(0xFFFF7A1A.toInt())
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(intent)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(ctx).notify(1, n)
        }
    }
}
