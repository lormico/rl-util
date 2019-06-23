package it.lmico.myapplication.departures;

import android.content.res.XmlResourceParser;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeparturesXMLParser {

    public static Map<String, Map<String, List<LocalTime>>> parseDepartures(XmlResourceParser departuresParser) {

        HashMap<String, Map<String, List<LocalTime>>> outMap = new HashMap<>();
        String tag = null;
        String text;
        String direction = null;
        String day = null;
        try {
            int eventType = departuresParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                    Log.v("parseDepartures","Start parsing xml");
                } else if (eventType == XmlPullParser.START_TAG) {
                    tag = departuresParser.getName();
                    Log.v("parseDepartures","Start tag " + tag);
                    if (tag.equals("departures-set")) {
                        direction = departuresParser.getAttributeValue(null, "direction");
                        day = departuresParser.getAttributeValue(null, "day");

                        if (outMap.get(direction) == null) {
                            outMap.put(direction, new HashMap<String, List<LocalTime>>());
                        }

                        if (outMap.get(direction).get(day) == null) {
                            outMap.get(direction).put(day, new ArrayList<LocalTime>());
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    Log.v("parseDepartures","End tag " + tag);
                } else if (eventType == XmlPullParser.TEXT) {
                    text = departuresParser.getText();

                    Log.v("parseDepartures","Text " + text);
                    Integer hour = Integer.valueOf(text.substring(0,2));
                    Integer minute = Integer.valueOf(text.substring(3,5));
                    outMap.get(direction).get(day).add(LocalTime.of(hour, minute));
                }
                eventType = departuresParser.next();
            }
        } catch (XmlPullParserException xppe) {
            xppe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return outMap;
    }
}
