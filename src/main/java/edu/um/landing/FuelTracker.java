package edu.um.landing;

public class FuelTracker {

    //--- Constants
    public final static double FUEL_ENERGY_DENSITY = 42 * 10E6;
    public final static double FUEL_MASS_DENSITY = 0.8;
    public final static double FUEL_PRICE_PER_LITRE = 1.81; //USD
    //---

    private double fuelUsage;

    public FuelTracker() {}

    public double getUsage() {
        return fuelUsage;
    }

    public void addRaw(double mass) {
        this.fuelUsage += mass;
    }

    /**
     * Add fuel usage.
     * @param mass The mass of the object.
     * @param acceleration The accleration of the object.
     * @return The used fuel in kg.
     */
    public double add(double mass, double acceleration) {
        final double usage = Math.abs(0.5 * mass * Math.pow(acceleration, 2)) / FUEL_ENERGY_DENSITY * FUEL_MASS_DENSITY;
        this.fuelUsage += usage;
        return usage;
    }

}
