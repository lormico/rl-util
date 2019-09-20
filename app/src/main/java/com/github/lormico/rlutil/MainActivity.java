package com.github.lormico.rlutil;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.lormico.rlutil.departures.DeparturesUtil;

import static com.github.lormico.rlutil.Constants.HMF;
import static com.github.lormico.rlutil.Constants.NORTHBOUND;
import static com.github.lormico.rlutil.Constants.ONE_LINER;
import static com.github.lormico.rlutil.Constants.SOUTHBOUND;
import static com.github.lormico.rlutil.departures.DeparturesUtil.CHANGED;
import static com.github.lormico.rlutil.departures.DeparturesUtil.DEFAULT;

public class MainActivity extends Activity {

    private Button getBtn, btnStartService, btnStopService;
    private TextView result;
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    private DeparturesUtil departuresUtil;
    private NotificationManager notificationManager;
    private BroadcastReceiver mBroadcastReceiver;

    public static final String RECEIVER_INTENT = "RECEIVER_INTENT";
    public static final String RECEIVER_MESSAGE = "RECEIVER_MESSAGE";
    public static final String UPDATE_DEPARTURES = "UPDATE_DEPARTURES";
    public static final String UPDATE_NOTIFICATION_ONLY = "UPDATE_NOTIFICATION_ONLY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        result = (TextView) findViewById(R.id.result);
        getBtn = (Button) findViewById(R.id.getBtn);
        getBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateDepartures();
            }
        });

        btnStartService = findViewById(R.id.buttonStartService);
        btnStopService = findViewById(R.id.buttonStopService);

        btnStartService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService();
            }
        });

        btnStopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService();
            }
        });

        // TODO: parametrizzare la scelta del file xml delle partenze
        departuresUtil = new DeparturesUtil(getResources().getXml(R.xml.departures_20w));
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("MainActivity","received broadcast!");
                String message = intent.getStringExtra(RECEIVER_MESSAGE);
                Log.d("MainActivity", "requested: " + message);
                switch (message) {
                    case UPDATE_DEPARTURES: updateDepartures();
                    case UPDATE_NOTIFICATION_ONLY: updateNotification();
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((mBroadcastReceiver),
                new IntentFilter(RECEIVER_INTENT)
        );
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onStop();
    }

    public void startService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        serviceIntent.putExtra("inputExtra", "Foreground Service Example in Android");

        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        stopService(serviceIntent);
    }

    public void updateNotification() {
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, MainActivity.class), 0);
        Map<String, Spanned> notificationContent = departuresUtil.getNotificationContent();
        Notification notification = new NotificationCompat.Builder(this, ForegroundService.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentIntent(contentIntent)
                .setContentText(notificationContent.get(ONE_LINER))
                .setStyle(new NotificationCompat.InboxStyle()
                        .addLine(notificationContent.get(SOUTHBOUND))
                        .addLine(notificationContent.get(NORTHBOUND))
                        .setBigContentTitle("Servizio regolare")
                        .setSummaryText("Prossime partenze"))
                .build();
        notificationManager.notify(1, notification);
    }

    public void updateApp() {

        final StringBuilder builder = new StringBuilder();
        for (int set : Arrays.asList(DEFAULT, CHANGED)) {

            for (String direction : Arrays.asList(NORTHBOUND, SOUTHBOUND)) {
                List<LocalTime> lastDeptList = departuresUtil.getLastNDepartures(direction, LocalDateTime.now(), 2, set);
                List<LocalTime> nextDeptList = departuresUtil.getNextNDepartures(direction, LocalDateTime.now(), 3, set);

                Collections.sort(lastDeptList);

                builder.append("\n").append(direction).append(": ");
                for (LocalTime dept : lastDeptList) {
                    builder.append(dept.format(HMF)).append(", ");
                }

                for (LocalTime dept : nextDeptList) {
                    builder.append(dept.format(HMF)).append(", ");
                }

            }

        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                result.setText(builder.toString());
            }
        });

    }

    public void updateDepartures() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        result.append("\nAggiornamento...");
                    }
                });
                departuresUtil.updateDepartures();

                updateNotification();
                updateApp();
//                updateWidget();

            }
        }).start();
    }

}