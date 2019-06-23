package it.lmico.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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
import java.util.Date;
import java.util.List;
import java.util.Map;

import it.lmico.myapplication.departures.DeparturesUtil;

public class MainActivity extends Activity {

    private Button getBtn;
    private TextView result;
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    private DeparturesUtil departuresUtil;

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

        departuresUtil = new DeparturesUtil(getResources().getXml(R.xml.departures));

    }

    private void getWebsite() {
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

                    builder.append(title).append("\n");

                    int i = 0;
                    for (Element row : rows) {

                        i++;
                        Log.d("getWebsite", "parsing tablerow" + i + "/" + rows.size());
                        Elements cols = row.select("td");

                        if (cols.size() == 3 && cols.get(0).text().contains("LIDO")) {

                            String partenzeRaw = cols.get(1).text();
                            Map<String, ArrayList<LocalTime>> partenze = Parser.parsePartenze(partenzeRaw);

                        }
                        //builder.append("\n").append("Link : ").append(row.attr("href"))
                        //        .append("\n").append("Text : ").append(row.text());7
                    }

                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
                    for (String direction : Arrays.asList("northbound", "southbound")) {
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