package cn.moncn.sing_box_windows.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.io.File

/**
 * 应用更新通知助手：
 * 1) 下载中进度通知；
 * 2) 下载完成可点击安装通知；
 * 3) 失败提示通知。
 */
class UpdateNotificationHelper(
    private val context: Context,
    private val installer: AppUpdateInstaller
) {
    fun showDownloadProgress(release: GitHubRelease, progress: DownloadProgress) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("正在下载更新 ${release.tagName}")
            .setContentText("${progress.getPercentageString()}  ${progress.getDownloadedSizeReadable()} / ${progress.getTotalSizeReadable()}")
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (progress.totalBytes > 0) {
            builder.setProgress(100, progress.percentage.coerceIn(0, 100), false)
        } else {
            builder.setProgress(0, 0, true)
        }

        notifySafely(NOTIFICATION_ID_PROGRESS, builder.build())
    }

    fun showDownloadCompleted(release: GitHubRelease, apkFile: File): Boolean {
        cancelProgress()
        val pendingIntent = runCatching {
            installer.buildInstallPendingIntent(
                apkFile = apkFile,
                requestCode = apkFile.absolutePath.hashCode()
            )
        }.getOrNull() ?: return false

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("更新下载完成 ${release.tagName}")
            .setContentText("点击安装新版本")
            .setAutoCancel(true)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.stat_sys_download_done,
                "安装",
                pendingIntent
            )
            .build()

        return notifySafely(NOTIFICATION_ID_COMPLETE, notification)
    }

    fun showDownloadFailed(message: String) {
        cancelProgress()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("更新下载失败")
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notifySafely(NOTIFICATION_ID_COMPLETE, notification)
    }

    private fun cancelProgress() {
        runCatching {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_PROGRESS)
        }
    }

    private fun notifySafely(id: Int, notification: android.app.Notification): Boolean {
        ensureChannel()
        if (!canPostNotification()) return false
        return runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
            true
        }.getOrDefault(false)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "应用更新下载与安装通知"
        }
        manager.createNotificationChannel(channel)
    }

    private fun canPostNotification(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val CHANNEL_ID = "app_update_channel"
        private const val CHANNEL_NAME = "应用更新"
        private const val NOTIFICATION_ID_PROGRESS = 2101
        private const val NOTIFICATION_ID_COMPLETE = 2102
    }
}

