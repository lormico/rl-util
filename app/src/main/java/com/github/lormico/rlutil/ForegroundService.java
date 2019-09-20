package com.github.lormico.rlutil;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.time.LocalTime;
import java.util.Timer;
import java.util.TimerTask;

import static com.github.lormico.rlutil.Constants.DEPARTURES_UPDATE_INTERVAL;
import static com.github.lormico.rlutil.Constants.NOTIFICATION_UPDATE_INTERVAL;

public class ForegroundService extends Service {

    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    Timer timer = new Timer();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                askUpdate(MainActivity.UPDATE_DEPARTURES);
            }
        }, 0, DEPARTURES_UPDATE_INTERVAL);

        // Wait for the end of the current minute, then update every minute
        askUpdate(MainActivity.UPDATE_NOTIFICATION_ONLY);
        int delay = (60 - LocalTime.now().getSecond()) * 1000;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                askUpdate(MainActivity.UPDATE_NOTIFICATION_ONLY);
            }
        }, delay, NOTIFICATION_UPDATE_INTERVAL);

        // stopSelf();

        return START_STICKY;

    }

    public void askUpdate(String extra) {
        Log.d("askUpdate", "sending broadcast '" + extra + "'...");
        final Intent receiverIntent = new Intent(MainActivity.RECEIVER_INTENT);
        receiverIntent.putExtra(MainActivity.RECEIVER_MESSAGE, extra);
        LocalBroadcastManager.getInstance(this).sendBroadcast(receiverIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timer.cancel();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
