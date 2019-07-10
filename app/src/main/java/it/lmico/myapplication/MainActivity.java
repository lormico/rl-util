package it.lmico.myapplication;

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

import it.lmico.myapplication.departures.DeparturesUtil;

import static it.lmico.myapplication.Constants.HMF;
import static it.lmico.myapplication.Constants.NORTHBOUND;
import static it.lmico.myapplication.Constants.ONE_LINER;
import static it.lmico.myapplication.Constants.SOUTHBOUND;
import static it.lmico.myapplication.departures.DeparturesUtil.CHANGED;
import static it.lmico.myapplication.departures.DeparturesUtil.DEFAULT;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        result = (TextView) findViewById(R.id.result);
        getBtn = (Button) findViewById(R.id.getBtn);
        getBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                update();
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

        departuresUtil = new DeparturesUtil(getResources().getXml(R.xml.departures));
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("MainActivity","received broadcast!");
                String message = intent.getStringExtra(RECEIVER_MESSAGE);
                Log.d("MainActivity", "message: " + message);
                switch (message) {
                    case UPDATE_DEPARTURES: update();
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
                List<LocalTime> lastDeptList = departuresUtil.getLastNDepartures(direction, LocalDateTime.now(), 2, DEFAULT);
                List<LocalTime> nextDeptList = departuresUtil.getNextNDepartures(direction, LocalDateTime.now(), 3, DEFAULT);

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

    public void update() {
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