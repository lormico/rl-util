package it.lmico.myapplication;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ParsingUnitTest {

    String partenzeRaw;
    ArrayList<Date> expectedPsp;
    ArrayList<Date> expectedColombo;

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
        expectedColombo.add(new Date());

        test_expectations();
    }

    public void test_expectations() {

        Map<String, ArrayList<Date>> result = Parser.parseChanges(partenzeRaw);
        List<Date> colombo = result.get("Colombo");
        List<Date> psp = result.get("PSP");
        assertEquals(colombo, expectedColombo);
        assertEquals(psp, expectedPsp);
    }
}