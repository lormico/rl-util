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

public class Parser {

    public static Map<String, List<LocalTime>> parseChanges(String s) {

        Map<String, List<LocalTime>> result = new HashMap<>();
        result.put(NORTHBOUND, new ArrayList<LocalTime>());
        result.put(SOUTHBOUND, new ArrayList<LocalTime>());

        Pattern pRegolare = Pattern.compile("regolare", Pattern.CASE_INSENSITIVE);
        Pattern pCifre = Pattern.compile("[0-9]+");
        Pattern pColomboGroup = Pattern.compile("(.*)[c][a-z]+(.*)", Pattern.CASE_INSENSITIVE);
        Pattern pPsp = Pattern.compile("[s]}", Pattern.CASE_INSENSITIVE);

        Matcher mRegolare;
        Matcher mCifre;
        Matcher mColomboGroup;
        Matcher mPsp;

        int countRegolare = 0;
        int countCifre = 0;
        int countPsp = 0;

        try {


            // Cerca di dividere PSP e Colombo, non sapendo se venga prima uno o l'altro
            Boolean pspThenColombo = null;
            mColomboGroup = pColomboGroup.matcher(s);
            List<String> groups = new ArrayList<>();
            while (mColomboGroup.find()) { groups.add(mColomboGroup.group()); }

            for (int i = 0; i < groups.size(); i++) {

                mPsp = pPsp.matcher(groups.get(i));
                while (mPsp.find()) {
                    countPsp++;
                }

                if (countPsp != 0) {
                    if (i == 0) {
                        pspThenColombo = true;
                    } else {
                        pspThenColombo = false;
                    }
                }

            }

            String groupPsp = "";
            String groupColombo = "";
            if (pspThenColombo == true) {
                groupPsp = groups.get(0);
                groupColombo = groups.get(1);
            } else if (pspThenColombo == false) {
                groupPsp = groups.get(1);
                groupColombo = groups.get(0);
            } else if (pspThenColombo == null) {
                // non sono nominati colombo o psp, mi aspetto regolare
                mRegolare = pRegolare.matcher(s);
                while (mRegolare.find()) { countRegolare++; }
                if (countRegolare == 1) {
                    return result;
                } else {
                    // Non gestito
                }
            }

            for (String group : Arrays.asList(groupPsp, groupColombo)) {

                mRegolare = pRegolare.matcher(group);
                while (mRegolare.find()) { countRegolare++; }

                mCifre = pCifre.matcher(group);
                while (mCifre.find()) { countCifre++; }

                if (countRegolare == 1) {

                    if (countCifre == 0) {
                        return result;
                    } else {
                        // Regolare in parte




                        countRegolare = 0;
                        countCifre = 0;

                        mCifre = pCifre.matcher(tempRes);
                        while (mCifre.find()) { countCifre++; }

                        if (countCifre != 0) {

                        }
                    }

                }

            }



        } catch (Exception e) {

        }

        return result;
    }

}
