package it.lmico.myapplication;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.lmico.myapplication.Constants.NORTHBOUND;
import static it.lmico.myapplication.Constants.SOUTHBOUND;
import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ParsingUnitTest {

    String partenzeRaw;
    List<LocalTime> expectedPsp;
    List<LocalTime> expectedColombo;

    @Before
    public void setUp() {
        partenzeRaw = "";
        expectedPsp = new ArrayList<>();
        expectedColombo = new ArrayList<>();
    }

    @Test
    public void regolare() {

        partenzeRaw = "    REGOLARE";

        test_expectations();
    }

    @Test
    public void psp_ok() {

        partenzeRaw = "    Partenze da Porta San Paolo ore regolare da Colombo ore: 8.15-8.25-8.35-8.45-9.05  ";
        expectedColombo = Arrays.asList(
                LocalTime.of(8,15),
                LocalTime.of(8,25),
                LocalTime.of(8,35),
                LocalTime.of(8,45),
                LocalTime.of(9,05)
        );

        test_expectations();
    }

    @Test
    public void simplePspOk() {

        partenzeRaw = "    Partenze da Porta San Paolo ore regolare da Colombo ore: 20:00  ";
        expectedColombo.add(LocalTime.of(20,0));

        test_expectations();
    }

    @Test
    public void checkOrder() {

        partenzeRaw = "Partenze da C.Colombo: ore 10.30 da PSP: ore 11.00";
        expectedColombo.add(LocalTime.of(10,30));
        expectedPsp.add(LocalTime.of(11,0));

        test_expectations();
    }


    public void test_expectations() {

        Map<String, List<LocalTime>> result = Parser.parseChanges(partenzeRaw);
        List<LocalTime> colombo = result.get(NORTHBOUND);
        List<LocalTime> psp = result.get(SOUTHBOUND);
        assertEquals(colombo, expectedColombo);
        assertEquals(psp, expectedPsp);
    }

    @Test
    public void testGetLocalTimesFromString() {

        String s1 = "regolare";
        String s2 = "05.30 23:45 abd 10. bab asd 4.33";
        String s3 = s1 + " ma anche " + s2;

        List<LocalTime> expected = Arrays.asList(
                LocalTime.of(5, 30),
                LocalTime.of(23,45),
                LocalTime.of(4,33));

        try {

            assertEquals(Parser.getLocalTimesFromString(s1), new ArrayList<LocalTime>());
            assertEquals(Parser.getLocalTimesFromString(s2), expected);
            Parser.getLocalTimesFromString(s3);

        } catch (Exception e){
            assertEquals("Il gruppo contiene 'REGOLARE' e contemporaneamente delle cifre!", e.getMessage());
        }


    }

    @Test
    public void testPatterns() {
        partenzeRaw = "Partenze rimodulate: da Porta S. Paolo: 10.30, 20.30, 22:30 da Cristoforo Colombo: 11.24, 22.33, 10:10";


    }
}