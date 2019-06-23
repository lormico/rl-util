package it.lmico.myapplication;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import it.lmico.myapplication.departures.DeparturesUtil;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class DeparturesUtilUnitTest {

    public Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    public DeparturesUtil departuresUtil;

    @Before
    public void setUp() {

        departuresUtil = new DeparturesUtil(appContext.getResources().getXml(R.xml.departures));

    }
    @Test
    public void nextDate() {


    }


}