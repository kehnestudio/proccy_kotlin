package com.kehnestudio.procrastinator_proccy.services

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.kehnestudio.procrastinator_proccy.Constants.ACTION_START_SERVICE
import com.kehnestudio.procrastinator_proccy.Constants.ACTION_STOP_SERVICE
import com.kehnestudio.procrastinator_proccy.Constants.EXTRA_TIMER_LENGTH
import com.kehnestudio.procrastinator_proccy.Constants.NOTIFICATION_CHANNEL_ID
import com.kehnestudio.procrastinator_proccy.Constants.NOTIFICATION_CHANNEL_NAME
import com.kehnestudio.procrastinator_proccy.Constants.NOTIFICATION_ID
import com.kehnestudio.procrastinator_proccy.R
import com.kehnestudio.procrastinator_proccy.utilities.TimerUtility
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : LifecycleService() {

    private val timeLeftInMinutes = MutableLiveData<Long>()

    private var passedAction: String? = null

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    lateinit var currentNotificationBuilder: NotificationCompat.Builder

    companion object {
        var mTimerIsRunning = MutableLiveData<Boolean>(false)
        val timeLeftInMillies = MutableLiveData<Long>()
    }

    override fun onCreate() {
        super.onCreate()
        currentNotificationBuilder = baseNotificationBuilder
        postInitialValues()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent != null && intent.extras != null) {
            passedAction = intent.getStringExtra(ACTION_START_SERVICE)
            timerDuration = intent.getLongExtra(EXTRA_TIMER_LENGTH, 0)
            when (passedAction) {
                ACTION_START_SERVICE -> {
                    startForeGroundService()
                    Timber.d("onStartCommand: Service started")
                }
                else -> Timber.d("onStartCommand: Service did nothing")
            }
        } else {
            intent?.let {
                when (it.action) {
                    ACTION_STOP_SERVICE -> {
                        stopService()
                        Timber.d("onStartCommand: Service stopped")
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForeGroundService() {

        startTimer()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())

        timeLeftInMillies.observe(this, Observer {
            var timeLeft = TimerUtility.getFormattedTimerTime(it, false)
            val text = if (timeLeft == "0") {
                getString(R.string.timer_service_notification_message_2)
            } else {
                getString(R.string.timer_service_notification_message, timeLeft)
            }
            val notification = currentNotificationBuilder
                .setContentText(text)
            notificationManager.notify(NOTIFICATION_ID, notification.build())
        })
    }

    private fun postInitialValues() {
        mTimerIsRunning.postValue(false)
        timeLeftInMillies.postValue(300000L)
    }




/*
    private fun startTimer() {
        mTimerIsRunning.postValue(true)
        var postedTime: Long
        val totalSeconds = TimeUnit.MINUTES.toSeconds(timerDuration)
        val tickSeconds = 0
        GlobalScope.launch(Dispatchers.Main) {
            for (second in totalSeconds downTo tickSeconds) {
                val time = String.format(
                    "%02d:%02d",
                    TimeUnit.SECONDS.toMinutes(second),
                    second - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(second))
                )
                Timber.d(time)
                postedTime = TimeUnit.SECONDS.toMillis(second)
                timeLeftInMillies.postValue(postedTime)
                delay(1000)
            }
            Timber.d("Done")
            stopService()
        }
    }

 */

    private var timerDuration = 1L
    private var timer: CountDownTimer? =null

    private fun startTimer() {
        Timber.d("Running%s", timerDuration)
        mTimerIsRunning.postValue(true)


        timer = object: CountDownTimer(timerDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillies.postValue(millisUntilFinished)
                Timber.d(TimerUtility.getFormattedTimerTime(millisUntilFinished, true))
            }

            override fun onFinish() {
                stopService()
            }
        }.start()
    }

    private fun stopService() {
        mTimerIsRunning.postValue(false)
        timer?.cancel()
        stopSelf()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

}
