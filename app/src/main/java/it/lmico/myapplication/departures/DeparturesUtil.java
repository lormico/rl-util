package it.lmico.myapplication.departures;

import android.content.res.XmlResourceParser;
import android.util.Log;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static it.lmico.myapplication.Constants.NORTHBOUND;
import static it.lmico.myapplication.Constants.SOUTHBOUND;


public class DeparturesUtil {

    private static Map<String, Map<String, List<LocalTime>>> defaultDeparturesMap;
    private Map<String, Map<String, List<LocalTime>>> departuresMap;

    public DeparturesUtil(XmlResourceParser departuresParser) {

        Log.v("DeparturesUtil", "initializing");
        defaultDeparturesMap = DeparturesXMLParser.parseDepartures(departuresParser);
        this.departuresMap = defaultDeparturesMap;

    }

    public List<LocalTime> getNextNDepartures(String direction, LocalDateTime dateTime, int n) {

        List<LocalTime> deptList = new ArrayList<>();
        LocalTime dept = getNextDeparture(direction, dateTime);
        int i = 0;

        while (!(dept == null) && i<n) {

            deptList.add(dept);

            dateTime = dateTime.with(dept);
            dept = getNextDeparture(direction, dateTime);
            i++;

        }

        return deptList;

    }

    public List<LocalTime> getLastNDepartures(String direction, LocalDateTime dateTime, int n) {

        List<LocalTime> deptList = new ArrayList<>();
        LocalTime dept = getLastDeparture(direction, dateTime);
        int i = 0;

        while (!(dept == null) && i<n) {

            deptList.add(dept);

            dateTime = dateTime.with(dept);
            dept = getLastDeparture(direction, dateTime);
            i++;

        }

        return deptList;

    }

    public LocalTime getNextDeparture(String direction, LocalDateTime dateTime) {

        List<LocalTime> depts = this.defaultDeparturesMap.get(direction).get(getDayType(dateTime));
        return getNextTime(depts, dateTime.toLocalTime());

    }

    public LocalTime getLastDeparture(String direction, LocalDateTime dateTime) {

        List<LocalTime> depts = this.defaultDeparturesMap.get(direction).get(getDayType(dateTime));
        return getLastTime(depts, dateTime.toLocalTime());

    }

    public LocalTime getNextTime(List<LocalTime> timeList, LocalTime inputTime) {

        Collections.sort(timeList);

        for (LocalTime time : timeList) {
            if (time.compareTo(inputTime) > 0) {
                    return time;
            }
        }

        return null;
    }

    public LocalTime getLastTime(List<LocalTime> timeList, LocalTime inputTime) {

        Collections.sort(timeList);
        Collections.reverse(timeList);

        for (LocalTime time : timeList) {
            if (time.compareTo(inputTime) < 0) {
                return time;
            }
        }

        return null;

    }

    private static boolean isNationalHoliday(LocalDateTime dateTime) {
        return false;
    }

    public String getDayType(LocalDateTime dateTime) {

        DayOfWeek dow = dateTime.getDayOfWeek();

        String day;
        if (isNationalHoliday(dateTime) || dow == DayOfWeek.SUNDAY) {
            day = "holiday";
        } else if (dow == DayOfWeek.SATURDAY) {
            day = "saturday";
        } else {
            day = "weekday";
        }
        return day;
    }

    public void applyChanges(Map<String, List<LocalTime>> changes) {

        for (String direction : Arrays.asList(NORTHBOUND, SOUTHBOUND)) {

            List<LocalTime> changeTimes = changes.get(direction);
            if (changeTimes.size() > 0) {

            }

        }

    }
}
