package edu.um.planet.math;

public class MathUtil {

    public static double clamp(Interval interval, int value) {
        return Math.max(interval.a(), Math.min(interval.b(), value));
    }

}
