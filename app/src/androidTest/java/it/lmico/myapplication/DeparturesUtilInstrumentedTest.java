package it.lmico.myapplication;

import android.content.res.XmlResourceParser;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.lmico.myapplication.departures.DeparturesUtil;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static it.lmico.myapplication.Constants.NORTHBOUND;
import static it.lmico.myapplication.Constants.SOUTHBOUND;
import static it.lmico.myapplication.departures.DeparturesUtil.CHANGED;
import static junit.framework.TestCase.assertEquals;

@RunWith(AndroidJUnit4.class)
public class DeparturesUtilInstrumentedTest {


    private DeparturesUtil departuresUtil;
    private Map<String, List<LocalTime>> changes;

    @Before
    public void setUp() {

        XmlResourceParser xmlResourceParser = getInstrumentation()
                .getTargetContext()
                .getResources()
                .getXml(R.xml.departures);
        departuresUtil = new DeparturesUtil(xmlResourceParser);
        changes = new HashMap<>();
        changes.put(NORTHBOUND, new ArrayList<LocalTime>());
        changes.put(SOUTHBOUND, new ArrayList<LocalTime>());

    }

    @Test
    public void departuresUtilBaseTest() {

        assertEquals(false, departuresUtil.hasChanges);

    }

    @Test
    public void applyChangesTest() {

        List<LocalTime> expected = Arrays.asList(
                LocalTime.of(10,5),
                LocalTime.of(10,20),
                LocalTime.of(10,35));
        changes.put(SOUTHBOUND, expected);
        departuresUtil.applyChanges(changes);

        List<LocalTime> actual = departuresUtil.getNextNDepartures(
                SOUTHBOUND,
                LocalDateTime.now().withHour(10).withMinute(5),
                3,
                CHANGED);

        assertEquals(expected, actual);
    }
}
