package it.lmico.myapplication;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.SimpleDateFormat;
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
import static it.lmico.myapplication.Constants.SOUTHBOUND;

public class MainActivity extends Activity {

    private Button getBtn, btnStartService, btnStopService;
    private TextView result;
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    private DeparturesUtil departuresUtil;
    private NotificationManager notificationManager;

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
/*
        // gestione delle notifiche
        NotificationChannel channel = new NotificationChannel(
                "NOTIF_CHAN_ID",
                "NOTIF_CHAN_NAME",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("NOTIF_CHAN_DESCRIPTION");

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);

        Notification notification = new Notification.Builder(this, "NOTIF_CHAN_ID")
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle("NOTIF_CONTENT_TITLE")
                .setContentText("NOTIF_CONTENT_TEXT")
                .build();
*/

        // notificationManager.notify("aoh", notification);

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

    public void updateNotification(String title, String text) {
        PendingIntent contentIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, MainActivity.class), 0);
        Notification notification = new NotificationCompat.Builder(this, ForegroundService.CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentIntent(contentIntent)
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
                            departuresUtil.applyChanges(changes);

                        }

                        String notifTitle;
                        if (regolare) {
                            notifTitle = "Servizio Regolare";
                        } else {
                            notifTitle = "Servizio Rimodulato";
                        }
                        updateNotification(notifTitle, status);
                        //builder.append("\n").append("Link : ").append(row.attr("href"))
                        //        .append("\n").append("Text : ").append(row.text());7
                    }

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

/*
    public void sendNotification(View view) {

        // BEGIN_INCLUDE(build_action)
        */
/** Create an intent that will be fired when the user clicks the notification.
         * The intent needs to be packaged into a {@link android.app.PendingIntent} so that the
         * notification service can fire it on our behalf.
         *//*

        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://developer.android.com/reference/android/app/Notification.html"));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        // END_INCLUDE(build_action)

        // BEGIN_INCLUDE (build_notification)
        */
/**
         * Use NotificationCompat.Builder to set up our notification.
         *//*

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        */
/** Set the icon that will appear in the notification bar. This icon also appears
         * in the lower right hand corner of the notification itself.
         *
         * Important note: although you can use any drawable as the small icon, Android
         * design guidelines state that the icon should be simple and monochrome. Full-color
         * bitmaps or busy images don't render well on smaller screens and can end up
         * confusing the user.
         *//*

        builder.setSmallIcon(R.drawable.ic_stat_notification);

        // Set the intent that will fire when the user taps the notification.
        builder.setContentIntent(pendingIntent);

        // Set the notification to auto-cancel. This means that the notification will disappear
        // after the user taps it, rather than remaining until it's explicitly dismissed.
        builder.setAutoCancel(true);

        */
/**
         *Build the notification's appearance.
         * Set the large icon, which appears on the left of the notification. In this
         * sample we'll set the large icon to be the same as our app icon. The app icon is a
         * reasonable default if you don't have anything more compelling to use as an icon.
         *//*

        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground));

        */
/**
         * Set the text of the notification. This sample sets the three most commononly used
         * text areas:
         * 1. The content title, which appears in large type at the top of the notification
         * 2. The content text, which appears in smaller text below the title
         * 3. The subtext, which appears under the text on newer devices. Devices running
         *    versions of Android prior to 4.2 will ignore this field, so don't use it for
         *    anything vital!
         *//*

        builder.setContentTitle("BasicNotifications Sample");
        builder.setContentText("Time to learn about notifications!");
        builder.setSubText("Tap to view documentation about notifications.");
        builder.
        // END_INCLUDE (build_notification)

        // BEGIN_INCLUDE(send_notification)
        */
/**
         * Send the notification. This will immediately display the notification icon in the
         * notification bar.
         *//*

        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
        // END_INCLUDE(send_notification)
    }
*/

}