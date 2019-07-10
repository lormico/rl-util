package it.lmico.myapplication.departures;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.lmico.myapplication.Constants.HOLIDAY;
import static it.lmico.myapplication.Constants.NORTHBOUND;
import static it.lmico.myapplication.Constants.SATURDAY;
import static it.lmico.myapplication.Constants.SOUTHBOUND;
import static it.lmico.myapplication.Constants.WEEKDAY;

public class DeparturesMap {

    private Map<String, Map<String, List<LocalTime>>> departuresMap;
    public DeparturesMap() {

        this.departuresMap = new HashMap<>();
        for (String day : Arrays.asList(WEEKDAY, SATURDAY, HOLIDAY)) {
            departuresMap.put(day, new HashMap<String, List<LocalTime>>());
            for (String direction : Arrays.asList(NORTHBOUND, SOUTHBOUND)) {
                departuresMap.get(day).put(direction, new ArrayList<LocalTime>());
            }
        }
    }

    private DeparturesMap(DeparturesMap clone) {
        this.departuresMap = clone.departuresMap;
    }

    public DeparturesMap clone() {
        return new DeparturesMap(this);
    }

    Map<String, List<LocalTime>> get(String day) {
        return this.departuresMap.get(day);
    }

    void put(String day, Map<String, List<LocalTime>> map) {
        this.departuresMap.put(day, map);
    }
}