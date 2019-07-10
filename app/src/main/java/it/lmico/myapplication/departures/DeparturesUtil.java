package it.lmico.myapplication.departures;

import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.lmico.myapplication.Parser;

import static it.lmico.myapplication.Constants.HMF;
import static it.lmico.myapplication.Constants.NORTHBOUND;
import static it.lmico.myapplication.Constants.ONE_LINER;
import static it.lmico.myapplication.Constants.SOUTHBOUND;


public class DeparturesUtil {

    public static final int DEFAULT = 0;
    public static final int CHANGED = 1;
    public static final List<String> DIRECTIONS = Arrays.asList(NORTHBOUND, SOUTHBOUND);

    private static DeparturesMap defaultDeparturesMap;
    private DeparturesMap departuresMap;

    private Map<String, LocalTime> lastChangedDepartures;

    public DeparturesUtil(XmlResourceParser departuresParser) {

        Log.v("DeparturesUtil", "initializing");
        defaultDeparturesMap = DeparturesXMLParser.parseDepartures(departuresParser);
        this.departuresMap = defaultDeparturesMap.clone();
        this.lastChangedDepartures = new HashMap<>();
        this.lastChangedDepartures.put(NORTHBOUND, null);
        this.lastChangedDepartures.put(SOUTHBOUND, null);

    }

    public List<LocalTime> getNextNDepartures(String direction, LocalDateTime dateTime, int n, int set) {

        List<LocalTime> deptList = new ArrayList<>();
        LocalTime dept = getNextDeparture(direction, dateTime, set);
        int i = 0;

        while (!(dept == null) && i<n) {

            deptList.add(dept);

            dateTime = dateTime.with(dept);
            dept = getNextDeparture(direction, dateTime, set);
            i++;

        }

        return deptList;

    }

    public List<LocalTime> getLastNDepartures(String direction, LocalDateTime dateTime, int n, int set) {

        List<LocalTime> deptList = new ArrayList<>();
        LocalTime dept = getLastDeparture(direction, dateTime, set);
        int i = 0;

        while (!(dept == null) && i<n) {

            deptList.add(dept);

            dateTime = dateTime.with(dept);
            dept = getLastDeparture(direction, dateTime, set);
            i++;

        }

        return deptList;

    }

    public LocalTime getNextDeparture(String direction, LocalDateTime dateTime, int set) {

        DeparturesMap departuresMap = null;
        switch (set) {
            case DEFAULT: departuresMap = defaultDeparturesMap;
            case CHANGED: departuresMap = this.departuresMap;
        }
        List<LocalTime> depts = departuresMap.get(direction).get(getDayType(dateTime));
        return getNextTime(depts, dateTime.toLocalTime());

    }

    public LocalTime getLastDeparture(String direction, LocalDateTime dateTime, int set) {

        DeparturesMap departuresMap = null;
        switch (set) {
            case DEFAULT: departuresMap = defaultDeparturesMap;
            case CHANGED: departuresMap = this.departuresMap;
        }
        List<LocalTime> depts = departuresMap.get(direction).get(getDayType(dateTime));
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

    public void updateDepartures() {

        String status = DeparturesWebParser.getStatus();
        Log.i("DeparturesUtil","Letto status:\n" + status);
        this.applyChanges(Parser.parseChanges(status));

    }

    public void applyChanges(Map<String, List<LocalTime>> changes) {
        /* Si assume che i cambiamenti siano riferiti al giorno stesso */
        String day = getDayType(LocalDateTime.now());

        for (String direction : DIRECTIONS) {

            List<LocalTime> changeTimes = changes.get(direction);
            if (changeTimes.size() > 0) {

                Collections.sort(changeTimes);
                LocalTime first = changeTimes.get(0);
                LocalTime last = changeTimes.get(changeTimes.size() - 1);

                List<LocalTime> workingList = new ArrayList<>();
                boolean appliedChanges = false;
                for (LocalTime origDept : this.departuresMap.get(direction).get(day)) {
                    if (origDept.compareTo(first) < 0) {
                        workingList.add(origDept);
                    } else if (origDept.compareTo(first) > 0 && origDept.compareTo(last) < 0) {
                        if (!appliedChanges) {
                            workingList.addAll(changeTimes);
                            appliedChanges = true;
                        }
                    } else {
                        workingList.add(origDept);
                    }
                }
                this.departuresMap.get(direction).put(day, workingList);

                this.lastChangedDepartures.put(direction, last);


            }

        }

    }

    public boolean hasChanges() {
        boolean hasChanges = false;
        for (String direction : DIRECTIONS) {
            LocalTime last = this.lastChangedDepartures.get(direction);
            if (last != null) {
                hasChanges |= last.compareTo(LocalTime.now()) > 0;
            }
        }
        return hasChanges;
    }

    public Spannable formatFollowingDepartures(String direction, List<LocalTime> deptList) {

        SpannableStringBuilder sb = new SpannableStringBuilder();
        int index;
        for (LocalTime dept : deptList) {
            sb.append(", ").append(dept.format(HMF));

            LocalTime lastDept = getLastDeparture(direction, LocalDateTime.now().with(dept), DEFAULT);
            long delta = Duration.between(lastDept, dept).toMinutes();
            String sDelta = String.valueOf(delta) + "'";
            index = sb.length() + 3;
            sb.append(" (+").append(sDelta).append(")");

            ForegroundColorSpan fcs;
            if (delta > 15) {
                fcs = new ForegroundColorSpan(Color.rgb(255,96,37));
            } else if (delta > 5) {
                fcs = new ForegroundColorSpan(Color.rgb(160,160,16));
            } else {
                fcs = new ForegroundColorSpan(Color.rgb(22,83,158));
            }

            sb.setSpan(fcs, index, index + sDelta.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            sb.setSpan(new StyleSpan(Typeface.BOLD), index, index + sDelta.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }

        return sb;
    }

    private Spannable formatNextDeparture(LocalTime dept) {

        SpannableStringBuilder sb = new SpannableStringBuilder();

        sb.append(" ").append(dept.format(HMF));
        long delta = Duration.between(LocalTime.now(), dept).toMinutes();
        String sDelta = delta + "'";
        sb.append(" (fra ").append(sDelta).append(")");

        return sb;
    }

    public Map<String, Spanned> getNotificationContent() {

        Map<String, Spanned> content = new HashMap<>();
        Map<String, Spannable> temp = new HashMap<>();

        temp.put(SOUTHBOUND, new SpannableStringBuilder("da PSP:"));
        temp.get(SOUTHBOUND).setSpan(new StyleSpan(Typeface.BOLD), 0, 6, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        temp.put(NORTHBOUND, new SpannableStringBuilder("da CC:"));
        temp.get(NORTHBOUND).setSpan(new StyleSpan(Typeface.BOLD), 0, 5, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

        // Formatta le partenze immediate
        LocalDateTime now = LocalDateTime.now();

        for (String direction : DIRECTIONS) {
            LocalTime nextDept = this.getNextDeparture(direction, now, DEFAULT);
            Spannable sNextDept = formatNextDeparture(nextDept);

            List<LocalTime> followingDepts = getNextNDepartures(direction, now.with(nextDept),2, DEFAULT);
            Spannable sFollowingDepts = formatFollowingDepartures(direction, followingDepts);

            content.put(direction, (Spanned) TextUtils.concat(
                    temp.get(direction),
                    sNextDept,
                    sFollowingDepts)
            );
        }

        SpannableStringBuilder oneLiner = new SpannableStringBuilder();
//        oneLiner.append(sbSouthbound).append("\n").append(sbNorthbound);

//        content.put(NORTHBOUND, sbNorthbound);
//        content.put(SOUTHBOUND, spannedSB);
        content.put(ONE_LINER, oneLiner);
        return content;
    }
}
