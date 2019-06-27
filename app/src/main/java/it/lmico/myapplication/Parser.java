package it.lmico.myapplication;

import android.util.Log;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
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

    private static String sTime = "[0-9]([0-9])?[:\\.][0-9]{2}";
    private static Pattern pRegolare = Pattern.compile("regolare", Pattern.CASE_INSENSITIVE);
    private static Pattern pCifre = Pattern.compile("[0-9]+");
    private static Pattern pTwoNumericBlocks = Pattern.compile("([\\D]*)(("+sTime+")+)([\\D])("+sTime+")([\\D]*)");
    private static Pattern pOneNumericBlock = Pattern.compile("([\\D]*)(("+sTime+")+)([\\D])");
    private static Pattern pColomboGroup = Pattern.compile("(.*)(([c][a-z\\. ]*)?(colombo).*)", Pattern.CASE_INSENSITIVE);
    private static Pattern pColombo = Pattern.compile("colombo|ostia", Pattern.CASE_INSENSITIVE);
    private static Pattern pPsp = Pattern.compile("[s]", Pattern.CASE_INSENSITIVE);
    private static Pattern pTime = Pattern.compile(sTime);

    private static Matcher mRegolare;
    private static Matcher mCifre;
    private static Matcher mColomboGroup;
    private static Matcher mColombo;
    private static Matcher mPsp;
    private static Matcher mTime;


    static Map<String, List<LocalTime>> parseChanges(String s) {

        Map<String, List<LocalTime>> result = new HashMap<>();
        result.put(NORTHBOUND, new ArrayList<LocalTime>());
        result.put(SOUTHBOUND, new ArrayList<LocalTime>());

        try {

            Matcher mat = Pattern.compile("(([0-9]([0-9])?[:\\.][0-9]{2})[^a-z]*)", Pattern.CASE_INSENSITIVE).matcher(s);

            List<String> foundTextGroups = new ArrayList<>();
            List<String> foundTimeGroups = new ArrayList<>();
            while (mat.find()) {
                foundTimeGroups.add(mat.group());
            }

            if (foundTimeGroups.size() == 0) {

                // non ci sono blocchi di orari
                // testare che sia regolare

            } else if (foundTimeGroups.size() == 1) {

                // solo uno regolare
                mat = Pattern.compile(
                        "(.*?)(" + foundTimeGroups.get(0) + ")(.*)").matcher(s);

                if (mat.matches()) {
                    foundTextGroups.add(mat.group(1));
                    foundTextGroups.add(mat.group(3));
                } else {
                    throw new Exception("AAAA");
                }

                int i = 0;
                for (String textGroup : foundTextGroups) {

                    mat = pRegolare.matcher(textGroup);
                    if (mat.find()) {

                        if (i == 0) {

                            // Il blocco prima degli orari contiene "regolare"

                            // >> PSP: regolare; CC: %%% <<
                            //     Partenze da Porta San Paolo ore regolare da Colombo ore: 8.15-8.25-8.35-8.45-9.05  

                            Pattern pRegolareGroup = Pattern.compile("(.*?)(regolare)(.*)", Pattern.CASE_INSENSITIVE);
                            mRegolare = pRegolareGroup.matcher(textGroup);

                            Matcher mPspFirst;
                            Matcher mColomboFirst;
                            Matcher mPspLast;
                            Matcher mColomboLast;

                            if (mRegolare.matches()) {
                                mPspFirst = pPsp.matcher(mRegolare.group(1));
                                mColomboFirst = pColombo.matcher(mRegolare.group(1));
                                mPspLast = pPsp.matcher(mRegolare.group(3));
                                mColomboLast = pColombo.matcher(mRegolare.group(3));
                            } else {
                                throw new Exception("AAAA");
                            }

                            if (mPspFirst.find() && mColomboLast.find()) {
                                result.put(NORTHBOUND, getLocalTimesFromString(foundTimeGroups.get(0)));
                            } else if (mColomboFirst.find() && mPspLast.find()) {
                                result.put(SOUTHBOUND, getLocalTimesFromString(foundTimeGroups.get(0)));
                            } else {
                                throw (new Exception("Non so che roba sia: " + s));
                            }

                        } else {

                            // Il blocco dopo gli orari contiene "regolare"

                            // >> xxx: %%%; yyy: regolare <<
                            mPsp = pPsp.matcher(textGroup);
                            mColombo = pColombo.matcher(textGroup);
                            if (mPsp.find() && !mColombo.find()) {
                                // >> CC: %%%; PSP: regolare <<
                                result.put(NORTHBOUND, getLocalTimesFromString(foundTimeGroups.get(0)));
                            } else if (mColombo.find() && !mPsp.find()) {
                                // >> PSP: %%%; CC: regolare <<
                                result.put(SOUTHBOUND, getLocalTimesFromString(foundTimeGroups.get(0)));
                            }

                        }

                    }

                    i++;

                }

            } else if (foundTimeGroups.size() == 2){

                // doppia rimodulazione
                mat = Pattern.compile(
                        "(.*?)(" +
                        foundTimeGroups.get(0) + ")(.*?)(" +
                        foundTimeGroups.get(1) + ")(.*)").matcher(s);

                if (mat.matches()) {
                    foundTextGroups.add(mat.group(1));
                    foundTextGroups.add(mat.group(3));
                    foundTextGroups.add(mat.group(5));
                } else {
                    throw new Exception("AAAA");
                }

                mPsp = pPsp.matcher(foundTextGroups.get(0));
                mColombo = pColombo.matcher(foundTextGroups.get(0));

                if (mPsp.find() && !mColombo.find()) {
                    result.put(NORTHBOUND, getLocalTimesFromString(foundTimeGroups.get(1)));
                    result.put(SOUTHBOUND, getLocalTimesFromString(foundTimeGroups.get(0)));
                } else if (!mPsp.find() && mColombo.find()) {
                    result.put(NORTHBOUND, getLocalTimesFromString(foundTimeGroups.get(0)));
                    result.put(SOUTHBOUND, getLocalTimesFromString(foundTimeGroups.get(1)));
                } else {
                    throw new Exception("AAAA");
                }

            } else {
                // più di 2 gruppi, errore
                throw new Exception("AAAA");
            }



        } catch (Exception e) {
            Log.e("Parser","problema nel parsing: " + e.getMessage());
        }

        return result;
    }


    static Map<String, List<LocalTime>> parseChanges_old(String s) {

        Map<String, List<LocalTime>> result = new HashMap<>();
        result.put(NORTHBOUND, new ArrayList<LocalTime>());
        result.put(SOUTHBOUND, new ArrayList<LocalTime>());

        /*
        STRATEGIA 2)
            - se ci sono parole chiave tipo "sciopero" arresta l'esecuzione (non ancora gestito)
            - cerca due blocchi di cifre in cui non ci sono caratteri a-z in mezzo
                - se li trova, sono rimodulate entrambe le direzioni:
                    - parsa il testo prima del primo blocco e fra il primo e il secondo
                    - assegna le direzioni dal testo parsato
                - se non li trova, è rimodulata solo una direzione:
                    - si aspetta 'regolare' fuori dal gruppo di orari
         */


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
                    throw new Exception("Impossibile identificare la stringa: " + s);
                }
            } else if (pspThenColombo == true) {
                groupPsp = groups.get(0);
                groupColombo = groups.get(1);
            } else if (pspThenColombo == false) {
                groupPsp = groups.get(1);
                groupColombo = groups.get(0);
            }

            result.put(NORTHBOUND, getLocalTimesFromString(groupColombo));
            result.put(SOUTHBOUND, getLocalTimesFromString(groupPsp));

        } catch (Exception e) {
            // Gestire
        }

        return result;
    }

    public static List<LocalTime> getLocalTimesFromString(String s) throws Exception {

        List<LocalTime> outList = new ArrayList<>();
        StringBuilder temp;
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
                temp = new StringBuilder();
                char[] fill = new char[5-mTime.group().length()];
                Arrays.fill(fill, '0');
                temp.append(fill).append(mTime.group());
                String sep = temp.substring(2,3);

                outList.add(LocalTime.parse(temp, DateTimeFormatter.ofPattern("HH"+sep+"mm")));
            }
        } else {
            throw new Exception("Più di un'occorrenza di 'REGOLARE' nel gruppo!");
        }

        return outList;
    }
}
