package top.krushiler.tracktalk.location

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class LocationSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = LocationSyncPreferences(applicationContext)
        if (!prefs.isEnabled || !prefs.hasCredentials()) {
            LocationSyncScheduler.cancel(applicationContext)
            return Result.success()
        }

        val token = prefs.token ?: return Result.failure()
        val baseUrl = prefs.baseUrl ?: return Result.failure()

        return try {
            val location = LocationSyncClient.syncOnce(
                context = applicationContext,
                token = token,
                baseUrl = baseUrl,
            )
            if (location != null) Result.success() else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

object LocationSyncScheduler {
    private const val UNIQUE_WORK_NAME = "location_sync_periodic"
    private const val REPEAT_INTERVAL_MINUTES = 15L

    fun enqueue(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<LocationSyncWorker>(
            REPEAT_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .addTag(UNIQUE_WORK_NAME)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
