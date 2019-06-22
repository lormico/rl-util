package it.lmico.myapplication;

import android.content.res.Resources;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import it.lmico.myapplication.departures.DeparturesXMLParser;


public class DeparturesUtil {

    public Date getNextDeparture(String direction, Date date) {



        /*Date nextDeparture = getNextDate(DeparturesFactory(direction, getDayType(date)), date);
        List<Date> list = DeparturesXMLParser.parse(Resources.getXml(R.xml.departures));
        Date nextDeparture = Collections.min();*/
        Date nextDeparture = new Date();

        return nextDeparture;
    }

    private static boolean isNationalHoliday(Calendar cal) {
        return false;
    }

    public static String getDayType(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dow = calendar.get(Calendar.DAY_OF_WEEK);

        String day;
        if (isNationalHoliday(calendar) || dow == Calendar.SUNDAY) {
            day = "holiday";
        } else if (dow == Calendar.SATURDAY) {
            day = "saturday";
        } else {
            day = "weekday";
        }
        return day;
    }

}
