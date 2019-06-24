package it.lmico.myapplication;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
    ArrayList<LocalTime> expectedPsp;
    ArrayList<LocalTime> expectedColombo;

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
        expectedColombo.add(LocalTime.now());

        test_expectations();
    }

    @Test
    public void simplePspOk() {

        partenzeRaw = "    Partenze da Porta San Paolo ore regolare per Colombo ore: 20:00  ";
        expectedColombo.add(LocalTime.now());

        test_expectations();
    }

    public void test_expectations() {

        Map<String, List<LocalTime>> result = Parser.parseChanges(partenzeRaw);
        List<LocalTime> colombo = result.get(NORTHBOUND);
        List<LocalTime> psp = result.get(SOUTHBOUND);
        assertEquals(colombo, expectedColombo);
        assertEquals(psp, expectedPsp);
    }
}