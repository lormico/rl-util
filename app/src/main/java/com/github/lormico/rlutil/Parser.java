package com.github.lormico.rlutil;

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

import static com.github.lormico.rlutil.Constants.NORTHBOUND;
import static com.github.lormico.rlutil.Constants.SOUTHBOUND;

public abstract class Parser {

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
    private static Pattern pPsp = Pattern.compile("paolo|psp|porta|roma", Pattern.CASE_INSENSITIVE);
    private static Pattern pTime = Pattern.compile(sTime);

    private static Matcher mRegolare;
    private static Matcher mCifre;
    private static Matcher mColomboGroup;
    private static Matcher mColombo;
    private static Matcher mPsp;
    private static Matcher mTime;

    private static Matcher mPspFirst;
    private static Matcher mColomboFirst;
    private static Matcher mPspLast;
    private static Matcher mColomboLast;
    private static Matcher mRegolareFirst;
    private static Matcher mRegolareLast;

    /**
     * Parse the line status string and figure out the changes to the ordinary timetable.
     *
     * At first, it counts how many "time groups" there are in the string:
     *      - none: there are no changes
     *      - 1:    only the departures in one direction are changed
     *      - 2:    both directions are affected by the changes
     *
     * @param   s   the string containing the line status
     * @return      a direction - updated departures map
     */
    public static Map<String, List<LocalTime>> parseChanges(String s) {

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
                // TODO: testare che sia regolare

            } else if (foundTimeGroups.size() == 1) {

                /* Only one time group found, we can expect either:
                 *  - "XXX regolare YYY [hh:mm]"
                 *  - "XXX [hh:mm] YYY regolare"
                 *  - "XXX [hh:mm]"
                 */

                // Get the text blocks before and after the time group
                mat = Pattern.compile(
                        "(.*?)(" + foundTimeGroups.get(0) + ")(.*)").matcher(s);

                if (mat.matches()) {
                    foundTextGroups.add(mat.group(1));
                    foundTextGroups.add(mat.group(3));
                } else {
                    throw new Exception("Impossibile estrarre testo prima e dopo i blocchi di orari per " + s);
                }

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
                    /* The second text block contains no information:
                     * concentrate on the first.
                     *
                     * Examples: "XXX regolare YYY [hh:mm]"
                     *           "XXX [hh:mm]"
                     */

                    if (noRegolareAtAll) {
                        /* The word "REGOLARE" is not present in the first block.
                         *
                         * Example: "Partenze da DDD [hh:mm]"
                         */

                        if (onlyPspFirst) {
                            result.put(SOUTHBOUND, getLocalTimesFromString(foundTimeGroups.get(0)));
                        } else if (onlyColomboFirst) {
                            result.put(NORTHBOUND, getLocalTimesFromString(foundTimeGroups.get(0)));
                        } else {
                            throw new Exception("AAAA");
                        }
                    } else {
                        /* The word "REGOLARE" is present in the first block.
                         * We need to find out what direction it refers to.
                         *
                         * Example: "XXX regolare YYY [hh:mm]"
                         */
                        handleOneRegolare(foundTextGroups.get(0), foundTimeGroups.get(0), result);
                    }

                } else {
                    /* Both the first and the second text group contain information.
                     *
                     * Examples:    "XXX hh:mm YYY regolare"
                     *              "XXX regolare YYY hh:mm poi regolare"
                     */

                    // Check for "regolare" at the end of the text group
                    boolean onlyLastRegolare = !bRegolareFirst && bRegolareLast;
                    boolean twoRegolare = bRegolareFirst && bRegolareLast;
                    if (onlyLastRegolare) {
                        // "regolare" only appears after the time group
                        // Which direction does the time group refer to?
                        mPsp = pPsp.matcher(foundTextGroups.get(1));
                        mColombo = pColombo.matcher(foundTextGroups.get(1));
                        if (mPsp.find() && !mColombo.find()) {
                            // "CC [hh:mm] PSP regolare"
                            result.put(NORTHBOUND, getLocalTimesFromString(foundTimeGroups.get(0)));
                        } else if (mColombo.find() && !mPsp.find()) {
                            // "PSP [hh:mm] CC regolare"
                            result.put(SOUTHBOUND, getLocalTimesFromString(foundTimeGroups.get(0)));
                        }
                    } else if (twoRegolare) {
                        // We got "regolare" before and after the time group:
                        //      "XXX regolare YYY [hh:mm] poi regolare"
                        // we can ignore the second occurrence, and fall within the one "regolare" case
                        handleOneRegolare(foundTextGroups.get(0), foundTimeGroups.get(0), result);
                    } else {
                        // Unexpected format
                        throw new Exception("Formato imprevisto per " + s);
                    }
                }

            } else if (foundTimeGroups.size() == 2){

                /* Two time groups found, both directions are affected.
                 * We can ignore any occurrence of "regolare".
                 * The only expected structure is:
                 *  - "XXX [hh:mm] YYY [hh:mm]"
                 */

                // Extract the two text groups before each time group
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

                // Find out which direction the first text group refers to
                mPsp = pPsp.matcher(foundTextGroups.get(0));
                mColombo = pColombo.matcher(foundTextGroups.get(0));

                if (mPsp.find() && !mColombo.find()) {
                    // PSP [hh:mm] CC [hh:mm]
                    result.put(NORTHBOUND, getLocalTimesFromString(foundTimeGroups.get(1)));
                    result.put(SOUTHBOUND, getLocalTimesFromString(foundTimeGroups.get(0)));
                } else if (!mPsp.find() && mColombo.find()) {
                    // CC [hh:mm] PSP [hh:mm]
                    result.put(NORTHBOUND, getLocalTimesFromString(foundTimeGroups.get(0)));
                    result.put(SOUTHBOUND, getLocalTimesFromString(foundTimeGroups.get(1)));
                } else {
                    // Unexpected format
                    throw new Exception("AAAA");
                }

            } else {
                // More than 2 time groups, unexpected
                throw new Exception("AAAA");
            }



        } catch (Exception e) {
            Log.e("Parser","problema nel parsing: " + e.getMessage());
        }

        return result;
    }


    /**
     * Handle the phrase structure "XXX regolare YYY [hh:mm]"
     *
     * First it finds out which direction is XXX, then puts the YYY amended departures in the result map
     *
     * @param   textGroup   the string containing the text before the time group
     * @param   timeGroup   the amended departures
     * @param   result      the empty directions - updated departures map to populate
     */
    private static void handleOneRegolare(String textGroup, String timeGroup, Map<String, List<LocalTime>> result) throws Exception {
        Pattern pRegolareGroup = Pattern.compile("(.*?)(regolare)(.*)", Pattern.CASE_INSENSITIVE);
        mRegolare = pRegolareGroup.matcher(textGroup);

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
            result.put(NORTHBOUND, getLocalTimesFromString(timeGroup));
        } else if (colomboThenPsp) {
            result.put(SOUTHBOUND, getLocalTimesFromString(timeGroup));
        } else {
            throw (new Exception("Non so che roba sia!"));
        }
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
