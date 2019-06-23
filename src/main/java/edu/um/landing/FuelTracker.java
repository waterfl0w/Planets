package edu.um.landing;

public class FuelTracker {

    private double fuelUsage;
    public final static double FUEL_ENERGY_DENSITY = 42 * 10E6;
    public final static double FUEL_MASS_DENSITY = 0.8;
    public final static double FUEL_PRICE_PER_LITRE = 1.81; //USD

    public FuelTracker() {}

    public double getUsage() {
        return fuelUsage;
    }

    public void addRaw(double mass) {
        this.fuelUsage += mass;
    }

    public double add(double mass, double velocity) {
        final double usage = Math.abs(0.5 * mass * Math.pow(velocity, 2)) / FUEL_ENERGY_DENSITY * FUEL_MASS_DENSITY;
        this.fuelUsage += usage;
        return usage;
    }

}
