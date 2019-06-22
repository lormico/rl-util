package it.lmico.myapplication;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.XmlRes;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import it.lmico.myapplication.Parser;

public class MainActivity extends Activity {

    private Button getBtn;
    private TextView result;
    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm");
    private Map<String, Map<String, List<Date>>> departuresMap;

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

        departuresMap = populateDepartures(getResources().getXml(R.xml.departures));

    }

    private Map<String, Map<String, List<Date>>> populateDepartures(XmlResourceParser departuresParser) {

        HashMap<String, Map<String, List<Date>>> outMap = new HashMap<>();
        String tag = null;
        String text;
        String direction = null;
        String day = null;
        try {
            int eventType = departuresParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                    Log.d(this.getLocalClassName(),"Start document");
                } else if (eventType == XmlPullParser.START_TAG) {
                    tag = departuresParser.getName();
                    Log.d(this.getLocalClassName(),"Start tag " + tag);
                    if (tag.equals("departures-set")) {
                        direction = departuresParser.getAttributeValue(null, "direction");
                        day = departuresParser.getAttributeValue(null, "day");

                        if (outMap.get(direction) == null) {
                            outMap.put(direction, new HashMap<String, List<Date>>());
                        }

                        if (outMap.get(direction).get(day) == null) {
                            outMap.get(direction).put(day, new ArrayList<Date>());
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    Log.d(this.getLocalClassName(),"End tag " + tag);
                } else if (eventType == XmlPullParser.TEXT) {
                    text = departuresParser.getText();

                    Log.d(this.getLocalClassName(),"Text " + text);
                    outMap.get(direction).get(day).add(sdf.parse(text));
                }
                eventType = departuresParser.next();
            }
        } catch (XmlPullParserException xppe) {
            xppe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (ParseException pe) {
            pe.printStackTrace();
        }

        return outMap;
    }

    private void getWebsite() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final StringBuilder builder = new StringBuilder();

                try {
                    Document doc = Jsoup.connect("https://www.atac.roma.it/function/pg_news.asp?act=3&r=16616&p=159").get();
                    String title = doc.title();
                    Elements rows = doc.select("tr");

                    builder.append(title).append("\n");

                    for (Element row : rows) {

                        Elements cols = row.select("td");

                        if (cols.size() == 3 && cols.get(0).text().contains("LIDO")) {

                            String partenzeRaw = cols.get(1).text();
                            Map<String, ArrayList<Date>> partenze = Parser.parsePartenze(partenzeRaw);

                        }
                        //builder.append("\n").append("Link : ").append(row.attr("href"))
                        //        .append("\n").append("Text : ").append(row.text());7
                    }

                    List<Date> depts = departuresMap.get("northbound").get(DeparturesUtil.getDayType(new Date()));
                    for (Date date : depts) {
                        builder.append("\n").append("Da Colombo: ").append(sdf.format(date));
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