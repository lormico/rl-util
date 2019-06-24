package it.lmico.myapplication;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.lmico.myapplication.Constants.NORTHBOUND;
import static it.lmico.myapplication.Constants.SOUTHBOUND;

class Parser {

    private static int countRegolare = 0;
    private static int countCifre = 0;
    private static int countPsp = 0;

    private static Pattern pRegolare = Pattern.compile("regolare", Pattern.CASE_INSENSITIVE);
    private static Pattern pCifre = Pattern.compile("[0-9]+");
    private static Pattern pColomboGroup = Pattern.compile("(.*)([c].*)", Pattern.CASE_INSENSITIVE);
    private static Pattern pPsp = Pattern.compile("[s]", Pattern.CASE_INSENSITIVE);
    private static Pattern pTime = Pattern.compile("[0-9]([0-9])?[:\\.][0-9]{2}");

    private static Matcher mRegolare;
    private static Matcher mCifre;
    private static Matcher mColomboGroup;
    private static Matcher mPsp;
    private static Matcher mTime;

    static Map<String, List<LocalTime>> parseChanges(String s) {

        Map<String, List<LocalTime>> result = new HashMap<>();
        result.put(NORTHBOUND, new ArrayList<LocalTime>());
        result.put(SOUTHBOUND, new ArrayList<LocalTime>());

        try {

            // Cerca di dividere PSP e Colombo, non sapendo se venga prima uno o l'altro
            Boolean pspThenColombo = null;
            mColomboGroup = pColomboGroup.matcher(s);
            List<String> groups = new ArrayList<>();
            if (mColomboGroup.matches()) {
                groups.add(mColomboGroup.group(1));
                groups.add(mColomboGroup.group(2));
            }

            for (int i = 0; i < groups.size(); i++) {

                countPsp = 0;
                mPsp = pPsp.matcher(groups.get(i));
                while (mPsp.find()) {
                    countPsp++;
                }

                if (countPsp != 0) {
                    pspThenColombo = i == 0;
                }

            }

            String groupPsp = "";
            String groupColombo = "";
            if (pspThenColombo == null) {
                // non sono nominati colombo o psp, mi aspetto regolare
                mRegolare = pRegolare.matcher(s);
                while (mRegolare.find()) { countRegolare++; }
                if (countRegolare == 1) {
                    return result;
                } else {
                    // Non gestito
                }
            } else if (pspThenColombo == true) {
                groupPsp = groups.get(0);
                groupColombo = groups.get(1);
            } else if (pspThenColombo == false) {
                groupPsp = groups.get(1);
                groupColombo = groups.get(0);
            }

            for (String group : Arrays.asList(groupPsp, groupColombo)) {



            }



        } catch (Exception e) {

        }

        return result;
    }

    private List<LocalTime> getLocalTimesFromString(String s) throws Exception {

        List<LocalTime> outList = new ArrayList<>();
        StringBuilder temp = new StringBuilder();
        countRegolare = 0;
        countCifre = 0;

        mRegolare = pRegolare.matcher(s);
        while (mRegolare.find()) { countRegolare++; }

        mCifre = pCifre.matcher(s);
        while (mCifre.find()) { countCifre++; }

        if (countRegolare == 1) {
            if (countCifre != 0) {
                throw new Exception("Il gruppo contiene 'REGOLARE' e contemporaneamente delle cifre!");
            }
        } else if (countRegolare == 0){
            mTime = pTime.matcher(s);
            while (mTime.find()) {
                char[] fill = new char[5-mTime.group().length()];
                Arrays.fill(fill, '0');
                temp.append(fill).append(mTime.group());
            }
        } else {
            throw new Exception("Più di un'occorrenza di 'REGOLARE' nel gruppo!");
        }

        return outList;
    }
}
