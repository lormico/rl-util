package it.lmico.myapplication;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import it.lmico.myapplication.departures.DeparturesUtil;

import static it.lmico.myapplication.Constants.NORTHBOUND;
import static it.lmico.myapplication.Constants.ONE_LINER;
import static it.lmico.myapplication.Constants.SOUTHBOUND;

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
                getWebsite();
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
                    case UPDATE_DEPARTURES: getWebsite();
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

    public void getWebsite() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        result.append("\nAggiornamento...");
                    }
                });

                final StringBuilder builder = new StringBuilder();

                try {
                    Log.d("getWebsite", "fetching html...");
                    Document doc = Jsoup.connect("https://www.atac.roma.it/function/pg_news.asp?act=3&r=16616&p=159").get();
                    Log.d("getWebsite", "done!");
                    String title = doc.title();
                    Elements rows = doc.select("tr");
                    String status = "";
                    boolean regolare = true;

                    builder.append(title).append("\n");
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");

                    int i = 0;
                    for (Element row : rows) {

                        i++;
                        Log.d("getWebsite", "parsing tablerow" + i + "/" + rows.size());
                        Elements cols = row.select("td");

                        if (cols.size() == 3 && cols.get(0).text().contains("LIDO")) {

                            status = cols.get(1).text();
                            Map<String, List<LocalTime>> changes = Parser.parseChanges(status);

                            // Estrarre questo blocco in altra funzione "estetica"
                            builder.append("\n").append("Letto da Atac:");
                            for (String direction : Arrays.asList(NORTHBOUND, SOUTHBOUND)) {
                                List<String> strDeptList = new ArrayList<>();
                                List<LocalTime> changesList = changes.get(direction);
                                regolare = regolare && changesList.size() == 0;
                                for (LocalTime dept : changesList) {
                                    strDeptList.add(dept.format(dtf));
                                }
                                builder.append("\n").append(direction).append(": ").append(String.join(", ", strDeptList));
                            }
                            //

                            departuresUtil.applyChanges(changes);

                        }

                        updateNotification();
                    }

                    // Estrarre questo blocco in altra funzione "estetica"
                    builder.append("\n").append("Orari regolari:");
                    for (String direction : Arrays.asList(NORTHBOUND, SOUTHBOUND)) {
                        StringBuilder s = new StringBuilder();
                        List<LocalTime> lastDeptList = departuresUtil.getLastNDepartures(direction, LocalDateTime.now(), 2);
                        List<LocalTime> nextDeptList = departuresUtil.getNextNDepartures(direction, LocalDateTime.now(), 3);

                        Collections.sort(lastDeptList);
                        List<String> strDeptList = new ArrayList<>();

//                        if (lastDeptList.size() > 0) {
                            for (LocalTime dept : lastDeptList) {
                                strDeptList.add(dept.format(dtf));
                            }
//                        }
//                        if (nextDeptList.size() > 0) {
                        for (LocalTime dept : nextDeptList) {
                            strDeptList.add(dept.format(dtf));
                        }
//                        }
                        builder.append("\n").append(direction + ": ").append(String.join(", ", strDeptList));
                    }

                    builder.append("\nStringa grezza:\n").append(status);
                    //

                } catch (IOException e) {
                    builder.append("Error : ").append(e.getMessage()).append("\n");
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        result.setText(builder.toString());
                    }
                });
            }
        }).start();
    }

}