package com.github.lormico.rlutil.departures;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

import static com.github.lormico.rlutil.Constants.WEBPAGE;

public abstract class DeparturesWebParser {

    public static String getStatus() {

        Elements rows = new Elements();
        try {
            Log.d("getStatus", "fetching html...");
            Document doc = Jsoup.connect(WEBPAGE).get();
            Log.d("getStatus", "done!");
            rows = doc.select("tr");
        } catch (IOException e) {
            Log.e("getStatus", e.toString());
        }
        String status = "";
        boolean regolare = true;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");

        int i = 0;
        for (Element row : rows) {

            i++;
            Log.d("update", "parsing tablerow" + i + "/" + rows.size());
            Elements cols = row.select("td");

            if (cols.size() == 3 && cols.get(0).text().contains("LIDO")) {
                status = cols.get(1).text();
            }
        }

        return status;
    }
}
