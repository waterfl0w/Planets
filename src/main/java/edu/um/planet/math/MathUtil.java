package edu.um.planet.math;

public class MathUtil {

    /**
     * This method clamps a value between two other values, thus it may be at least {@link Interval#a()} and at most
     * {@link Interval#b()}, inclusively.
     * @param interval The interval in which the value should be contained.
     * @param value The value.
     * @return Clamped value.
     */
    public static double clamp(Interval interval, int value) {
        return Math.max(interval.a(), Math.min(interval.b(), value));
    }

}
