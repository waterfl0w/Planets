package edu.um.planet.data;

import edu.um.planet.math.Vector3;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to parse data from JPL's Horizons service.
 */
public class DataParser {

    private final static OkHttpClient client = new OkHttpClient();

    private final static String _START_DATE = "2019-04-14";
    private final static String _END_DATE = "2019-04-15";
    private final static String _URL = "https://ssd.jpl.nasa.gov/horizons_batch.cgi?batch=1&TABLE_TYPE=VECTORS&OUT_UNITS=KM-S&COMMAND=%%22%d%%22&CENTER=%%27500%%4010%%27&CSV_FORMAT=%%22YES%%22&REF_PLANE=ECLIPTIC&REF_SYSTEM=J2000&TP_TYPE=ABSOLUTE&LABELS=YES&OBJ_DATA=YES&START_TIME=%%22%s%%22&STOP_TIME=%%22%s%%22&STEP_SIZE=%%221d%%22";
    private final static Pattern _VECTOR_PATTERN = Pattern.compile("\\$\\$SOE(.+)\\$\\$EOE", Pattern.MULTILINE | Pattern.DOTALL);

    private final static Pattern _MASS_PATTERN = Pattern.compile("Mass,* x?10\\^([0-9]+) *\\(*kg\\)* *= *~?([0-9]+\\.?[0-9]+)+(\\+-([0-9]+\\.?[0-9]+))?");
    private final static Pattern _MASS_PATTERN_TWO = Pattern.compile("Mass *\\(10\\^([0-9]+\\.?[0-9]+) *(kg|g) *\\) *= *([0-9]+\\.?[0-9]+)");

    private final static Pattern _RADIUS = Pattern.compile("Vol\\. *Mean *Radius *,* *((km|m)|\\((km|m)\\)) *= *([0-9]+\\.?[0-9]+)", Pattern.CASE_INSENSITIVE);
    private final static Pattern _RADIUS_TWO = Pattern.compile("Radius *\\((km|m)\\) *= *([0-9]+\\.?[0-9]*) *x *([0-9]+\\.?[0-9]*) *x *([0-9]+\\.?[0-9]*)", Pattern.CASE_INSENSITIVE);
    private final static Pattern _RADIUS_THREE = Pattern.compile("Radius *\\((km|m)\\)(,mean)? *= *([0-9]+\\.?[0-9]*)", Pattern.CASE_INSENSITIVE);

    private final static Pattern _SEMI_MAJOR_AXIS = Pattern.compile("Semi-major axis, a \\(km\\)= [0-9]+\\.[0-9]+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private final static Pattern _NAME_PATTERN = Pattern.compile("(Target body name: (.+)(\\{))");

    public static void main(String[] args) {

        final int[][] objects = new int[][]{{10, -1, -1}, {199, -1, -1}, {299, -1, -1}, {399, 301, 301}, {499, 401, 402}, {599, 501, 560},
                {699, 601, 653}, {799, 701, 729}, {899, 801, 814}, {999, 901, 905}};

        StringBuilder builder = new StringBuilder();

        for (int[] object : objects) {
        //for(int i = 1; i < 1000; i++) {
            try {
                //callJPL(i, builder);
                callJPL(object[0], builder);

                if(object[1] != -1 && object[2] != -1) {
                    for (int i = object[1]; i <= object[2]; i++) {
                        callJPL(i, builder);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        try {
            System.out.println(builder.toString());
            //Files.write(Paths.get(String.format("data/%s.csv", _START_DATE)), builder.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void callJPL(int code, StringBuilder builder) throws IOException {
        System.out.println(String.format(_URL, code, _START_DATE, _END_DATE));
        Response response = client.newCall(new Request.Builder()
                .url(String.format(_URL, code, _START_DATE, _END_DATE))
                .get()
                .build()).execute();

        final String data = response.body().string();
        response.close();


        //--- Details
        int id = code;
        String name;
        double mass = -1;
        double radius = -1;
        Vector3 position;
        Vector3 velocity;
        double semiMajorAxis = -1;
        double eccentricity = -1;
        double inclination = -1;
        {

            {
                final Matcher matcher = _NAME_PATTERN.matcher(data);
                if(matcher.find() && matcher.groupCount() >= 2) {
                    name = matcher.group(2).trim();
                } else {
                    return;
                }

            }


            {
                final Matcher firstMatcher = _MASS_PATTERN.matcher(data);
                final Matcher secondMatcher = _MASS_PATTERN_TWO.matcher(data);

                if(firstMatcher.find() && (firstMatcher.groupCount() == 2 || firstMatcher.groupCount() == 4)) {
                    try {
                        mass = Double.valueOf(firstMatcher.group(2)) * Math.pow(10, Double.valueOf(firstMatcher.group(1)));
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println(data);
                    }
                } else if(secondMatcher.find() && secondMatcher.groupCount() == 3) {
                    try {
                        mass = Double.valueOf(secondMatcher.group(3)) * Math.pow(10, Double.valueOf(secondMatcher.group(1)));

                        String unit = secondMatcher.group(2);
                        if(unit.equalsIgnoreCase("kg")) {
                            mass *= 1;
                        } else if(unit.equalsIgnoreCase("g")) {
                            mass *= 1E-3;
                        } else {
                            System.out.println("UNKNOWN UNIT, 1: " + unit);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println(data);
                    }
                } else{
                    System.out.println("No mass: " + code);
                }

            }

            if(code != 10){ //we cannot lookup this information for the sun
                final Matcher firstMatcher = _SEMI_MAJOR_AXIS.matcher(data);

                if(firstMatcher.find() && firstMatcher.groupCount() == 1) {
                    semiMajorAxis = Double.parseDouble(firstMatcher.group(1)) * 1E3;
                } else {
                    System.out.println("No semi-major axis: " + code);
                }
            }
        }

        //--- Radius
        {
            final Matcher matcher = _RADIUS.matcher(data);
            final Matcher secondMatcher = _RADIUS_TWO.matcher(data);
            final Matcher thirdMatcher = _RADIUS_THREE.matcher(data);
            if(matcher.find()) {
                if (matcher.groupCount() == 2) {
                    radius = Double.parseDouble(matcher.group(2));
                    String unit = matcher.group(1);
                    if (unit.equalsIgnoreCase("km")) {
                        radius *= 1E3;
                    } else if (unit.equalsIgnoreCase("m")) {
                        radius *= 1;
                    } else {
                        System.out.println("No unit, 2: " + unit);
                    }
                } else if (matcher.groupCount() == 4) {
                    radius = Double.parseDouble(matcher.group(4));
                    String unit = matcher.group(2);
                    if(unit == null) {
                        unit = matcher.group(3);
                    }
                    if (unit.equalsIgnoreCase("km")) {
                        radius *= 1E3;
                    } else if (unit.equalsIgnoreCase("m")) {
                        radius *= 1;
                    } else {
                        System.out.println("No unit, 3: " + unit);
                    }
                } else {
                    System.out.println("No radius, 1: " + code + " -> " + matcher.groupCount());
                }
            } else if(secondMatcher.find()) {
                if(secondMatcher.groupCount() == 4) {
                    radius = Math.max(Double.parseDouble(secondMatcher.group(2)), Math.max(Double.parseDouble(secondMatcher.group(3)), Double.parseDouble(secondMatcher.group(4))));
                    String unit = secondMatcher.group(1);
                    if (unit.equalsIgnoreCase("km")) {
                        radius *= 1E3;
                    } else if (unit.equalsIgnoreCase("m")) {
                        radius *= 1;
                    } else {
                        System.out.println("No unit, 4: " + unit);
                    }
                }else {
                    System.out.println("No radius, 2: " + code + " -> " + secondMatcher.groupCount());
                }
            } else if(thirdMatcher.find()) {
                if(thirdMatcher.groupCount() == 2 || thirdMatcher.groupCount() == 3) {
                    radius = Double.parseDouble(thirdMatcher.group(3));
                    String unit = thirdMatcher.group(1);
                    if (unit.equalsIgnoreCase("km")) {
                        radius *= 1E3;
                    } else if (unit.equalsIgnoreCase("m")) {
                        radius *= 1;
                    } else {
                        System.out.println("No unit, 5: " + unit);
                    }
                } else {
                    System.out.println("No radius, 3: " + code + " -> " + thirdMatcher.groupCount());
                }
            } else{
                System.out.println("No radius: " + code);
            }
        }

        //--- Vectors
        {
            final Matcher matcher = _VECTOR_PATTERN.matcher(data);
            assert matcher.find();

            final String[] vectors = matcher.group(1).trim().split("\n");
            assert vectors.length == 2;

            final String[] parts = vectors[1].split(",");
            assert parts.length == 11;

            final double px = Double.parseDouble(parts[2]) * 1E3;
            final double py = Double.parseDouble(parts[3]) * 1E3;
            final double pz = Double.parseDouble(parts[4]) * 1E3;

            final double vx = Double.parseDouble(parts[5]) * 1E3;
            final double vy = Double.parseDouble(parts[6]) * 1E3;
            final double vz = Double.parseDouble(parts[7]) * 1E3;

            position = new Vector3(px, py, pz);
            velocity = new Vector3(vx, vy, vz);

        }

        builder.append(String.format("%d,%s,%f,%f,%f,%f,%f,%f,%f,%f%n", id, name, radius, mass, position.getX(), position.getY(), position.getZ(), velocity.getX(), velocity.getY(), velocity.getZ()));
        System.out.println(code);

    }

}
