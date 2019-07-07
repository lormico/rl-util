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

x                // trovato un solo blocco di orari
                // posso avere tipo "partenze da psp regolari da colombo 10.30 10.40"
                // oppure "partenze da psp 10.20 10.40"

                mat = Pattern.compile(
                        "(.*?)(" + foundTimeGroups.get(0) + ")(.*)").matcher(s);

                if (mat.matches()) {
                    foundTextGroups.add(mat.group(1));
                    foundTextGroups.add(mat.group(3));
                } else {
                    throw new Exception("Impossibile estrarre testo prima e dopo i blocchi di orari per " + s);
                }

                // Controlla la struttura tipo "Partenze da XXX hh:mm hh:mm"
                Matcher mPspFirst;
                Matcher mColomboFirst;
                Matcher mPspLast;
                Matcher mColomboLast;
                Matcher mRegolareFirst;
                Matcher mRegolareLast;


                mPspFirst = pPsp.matcher(foundTextGroups.get(0));
                mColomboFirst = pColombo.matcher(foundTextGroups.get(0));
                mRegolareFirst = pRegolare.matcher(foundTextGroups.get(0));
                mPspLast = pPsp.matcher(foundTextGroups.get(1));
                mColomboLast = pColombo.matcher(foundTextGroups.get(1));
                mRegolareLast = pRegolare.matcher(foundTextGroups.get(1));

                boolean bPspFirst = mPspFirst.find();
                boolean bColomboFirst = mColomboFirst.find();
                boolean bRegolareFirst = mRegolareFirst.find();
                boolean bPspLast = mPspLast.find();
                boolean bColomboLast = mColomboLast.find();
                boolean bRegolareLast = mRegolareLast.find();

                boolean noInfoInLast = !bPspLast && !bColomboLast && !bRegolareLast;
                boolean noRegolareAtAll = !bRegolareFirst && !bRegolareLast;
                boolean onlyPspFirst = bPspFirst && !bColomboFirst;
                boolean onlyColomboFirst = bColomboFirst && !bPspFirst;

                if (noInfoInLast) {
                    // tutte le informazioni sono concentrate nel primo blocco di testo
                    // posso avere "XXX regolare YYY hh:mm"
                    //      oppure "XXX hh:mm"

                    if (noRegolareAtAll) {
                        // "XXX hh:mm"
                        if (onlyPspFirst) {
                            result.put(SOUTHBOUND, getLocalTimesFromString(foundTimeGroups.get(0)));
                        } else if (onlyColomboFirst) {
                            result.put(NORTHBOUND, getLocalTimesFromString(foundTimeGroups.get(0)));
                        } else {
                            throw new Exception("AAAA");
                        }
                    } else {
                        // Il blocco prima degli orari contiene "regolare"; a chi si riferisce?
                        // "XXX regolare YYY hh:mm"

                        Pattern pRegolareGroup = Pattern.compile("(.*?)(regolare)(.*)", Pattern.CASE_INSENSITIVE);
                        mRegolare = pRegolareGroup.matcher(foundTextGroups.get(0));

                        if (mRegolare.matches()) {
                            // Necessario altrimenti non riesco a chiamare .group(int)
                            mPspFirst = pPsp.matcher(mRegolare.group(1));
                            mColomboFirst = pColombo.matcher(mRegolare.group(1));
                            mPspLast = pPsp.matcher(mRegolare.group(3));
                            mColomboLast = pColombo.matcher(mRegolare.group(3));
                        } else {
                            throw new Exception("AAAA");
                        }

                        boolean pspThenColombo = mPspFirst.find() && mColomboLast.find();
                        boolean colomboThenPsp = mColomboFirst.find() && mPspLast.find();
                        if (pspThenColombo) {
                            result.put(NORTHBOUND, getLocalTimesFromString(foundTimeGroups.get(0)));
                        } else if (colomboThenPsp) {
                            result.put(SOUTHBOUND, getLocalTimesFromString(foundTimeGroups.get(0)));
                        } else {
                            throw (new Exception("Non so che roba sia: " + s));
                        }
                    }

                } else {
                    // Ci sono informazioni sia prima che dopo il blocco di orari
                    // XXX regolare YYY hh:mm ---- ESCLUSO A PRIORI
                    // XXX hh:mm YYY regolare
                    // XXX regolare YYY hh:mm poi regolare

                    // Confermo che effettivamente regolare sia dopo
                    //boolean onlyFirstRegolare = mRegolareFirst.find() && !mRegolareLast.find();
                    boolean onlyLastRegolare = !bRegolareFirst && bRegolareLast;
                    boolean twoRegolare = bRegolareFirst && bRegolareLast;
                    if (onlyLastRegolare) {
                        mPsp = pPsp.matcher(foundTextGroups.get(1));
                        mColombo = pColombo.matcher(foundTextGroups.get(1));
                        if (mPsp.find() && !mColombo.find()) {
                            // "CC hh:mm PSP regolare"
                            result.put(NORTHBOUND, getLocalTimesFromString(foundTimeGroups.get(0)));
                        } else if (mColombo.find() && !mPsp.find()) {
                            // "PSP hh:mm CC regolare"
                            result.put(SOUTHBOUND, getLocalTimesFromString(foundTimeGroups.get(0)));
                        }
                    } else if (twoRegolare) {
                        // XXX regolare YYY hh:mm poi regolare
                        // ignoro il secondo regolare, tratto come il caso regolare

                        // TODO duplicato da sopra, accorpare in una funzione
                        Pattern pRegolareGroup = Pattern.compile("(.*?)(regolare)(.*)", Pattern.CASE_INSENSITIVE);
                        mRegolare = pRegolareGroup.matcher(foundTextGroups.get(0));

                        if (mRegolare.matches()) {
                            // Necessario altrimenti non riesco a chiamare .group(int)
                            mPspFirst = pPsp.matcher(mRegolare.group(1));
                            mColomboFirst = pColombo.matcher(mRegolare.group(1));
                            mPspLast = pPsp.matcher(mRegolare.group(3));
                            mColomboLast = pColombo.matcher(mRegolare.group(3));
                        } else {
                            throw new Exception("AAAA");
                        }

                        boolean pspThenColombo = mPspFirst.find() && mColomboLast.find();
                        boolean colomboThenPsp = mColomboFirst.find() && mPspLast.find();
                        if (pspThenColombo) {
                            result.put(NORTHBOUND, getLocalTimesFromString(foundTimeGroups.get(0)));
                        } else if (colomboThenPsp) {
                            result.put(SOUTHBOUND, getLocalTimesFromString(foundTimeGroups.get(0)));
                        } else {
                            throw (new Exception("Non so che roba sia: " + s));
                        }
                    } else {
                        throw new Exception("Formato imprevisto per " + s);
                    }
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
