package edu.um.planet.math;

public class Interval {

    private final double a;
    private final double b;

    public Interval(double a, double b) {
        assert a <= b;
        this.a = a;
        this.b = b;
    }

    public double a() {
        return this.a;
    }

    public double b() {
        return this.b;
    }

    public static Interval of(double a, double b) {
        return new Interval(a, b);
    }

    @Override
    public String toString() {
        return String.format("[%e,%e]", this.a, this.b);
    }

}

