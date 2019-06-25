package edu.um.landing;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DataLogger is used to track flight data.
 */
public class DataLogger {

    public Map<Double, Map<String, Double>> data = new LinkedHashMap<>();

    public DataLogger() {}

    public Map<Double, Map<String, Double>> getData() {
        return data;
    }

    /**
     * Retrive the data.
     * @param system The system the data is associated to.
     * @return
     */
    public double[] getData(String system) {
        return data.values().stream().mapToDouble(stringDoubleMap -> stringDoubleMap.get(system)).toArray();
    }

    /**
     * Adds a new data entry.
     * @param height Timestamp/height.
     * @param system The associated system.
     * @param value The associated value.
     */
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
