package edu.um.landing;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DataLogger {

    public Map<Double, Map<String, Double>> data = new LinkedHashMap<>();

    public DataLogger() {}

    public Map<Double, Map<String, Double>> getData() {
        return data;
    }

    public double[] getData(String system) {
        return data.values().stream().mapToDouble(stringDoubleMap -> stringDoubleMap.get(system)).toArray();
    }

    public void add(double height, String system, double value) {
        if(data.containsKey(height)) {
            this.data.get(height).put(system, value);
        } else {
            this.data.put(height, new HashMap<String, Double>() {{
                this.put(system, value);
            }});
        }
    }

}
