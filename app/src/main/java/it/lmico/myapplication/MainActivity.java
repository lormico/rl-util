package it.lmico.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import it.lmico.myapplication.Parser;

public class MainActivity extends Activity {

    private Button getBtn;
    private TextView result;

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
                        builder.append("\n").append("Link : ").append(row.attr("href"))
                                .append("\n").append("Text : ").append(row.text());
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