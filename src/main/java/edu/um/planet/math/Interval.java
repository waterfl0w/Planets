package edu.um.planet.math;

/**
 * This class represents an interval.
 */
public class Interval {

    private final double a;
    private final double b;

    /**
     * Creates a new interval.
     * @param a This has to be smaller than or equal b; lower bound.
     * @param b This has to be greater than or equal to a; upper bound.
     */
    public Interval(double a, double b) {
        assert a <= b;
        this.a = a;
        this.b = b;
    }

    /**
     * Returns the lower bound of the interval.
     * @return
     */
    public double a() {
        return this.a;
    }

    /**
     * Returns the upper bound of the interval.
     * @return
     */
    public double b() {
        return this.b;
    }

    /**
     * Creates a new interval from [a,b].
     * @param a This has to be smaller than or equal b; lower bound.
     * @param b This has to be greater than or equal to a; upper bound.
     * @return
     */
    public static Interval of(double a, double b) {
        return new Interval(a, b);
    }

    @Override
    public String toString() {
        return String.format("[%e,%e]", this.a, this.b);
    }

}

