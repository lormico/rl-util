package com.github.lormico.rlutil.departures;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.lormico.rlutil.Constants.HOLIDAY;
import static com.github.lormico.rlutil.Constants.NORTHBOUND;
import static com.github.lormico.rlutil.Constants.SATURDAY;
import static com.github.lormico.rlutil.Constants.SOUTHBOUND;
import static com.github.lormico.rlutil.Constants.WEEKDAY;

public class DeparturesMap {

    private Map<String, Map<String, List<LocalTime>>> departuresMap;
    public DeparturesMap() {

        this.departuresMap = new HashMap<>();
        for (String direction : Arrays.asList(NORTHBOUND, SOUTHBOUND)) {
            departuresMap.put(direction, new HashMap<String, List<LocalTime>>());
            for (String day : Arrays.asList(WEEKDAY, SATURDAY, HOLIDAY)) {
                departuresMap.get(direction).put(day, new ArrayList<LocalTime>());
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
