package it.lmico.myapplication;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    public static Map<String, ArrayList<LocalTime>> parsePartenze (String s) {

        Map<String, ArrayList<LocalTime>> result = new HashMap<>();
        result.put("Colombo", new ArrayList<LocalTime>());
        result.put("PSP", new ArrayList<LocalTime>());

        Pattern p_regolare = Pattern.compile("regolare", Pattern.CASE_INSENSITIVE);
        Pattern p_cifre = Pattern.compile("[0-9]+");

        try {

            int count_regolare = 0;
            Matcher m_regolare = p_regolare.matcher(s);
            while (m_regolare.find()) { count_regolare++; }

            int count_cifre = 0;
            Matcher m_cifre = p_cifre.matcher(s);
            while (m_cifre.find()) { count_cifre++; }

            if (count_regolare > 0 && count_cifre == 0) {
                // Regolare ?
                return result;
            }




        } catch (Exception e) {

        }

        return result;
    }

}
