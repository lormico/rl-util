package it.lmico.myapplication;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    public static Map<String, ArrayList<Date>> parsePartenze (String s) {

        Map<String, ArrayList<Date>> result = new HashMap<>();
        result.put("Colombo", null);
        result.put("PSP", null);

        Pattern p_regolare = Pattern.compile("regolare", Pattern.CASE_INSENSITIVE);
        Pattern p_cifre = Pattern.compile("[0-9]+");

        try {

            int count_regolare = 0;
            while (p_regolare.matcher(s).find()) { count_regolare++; }
            int count_cifre = 0;
            while (p_cifre.matcher(s).find()) { count_cifre++; }

            if (count_regolare > 0 && count_cifre == 0) {
                // Regolare ?
                return result;
            }


        } catch (Exception e) {

        }

        return null;
    }

}
