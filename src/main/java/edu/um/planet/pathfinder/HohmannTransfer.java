package edu.um.planet.pathfinder;

import edu.um.landing.FuelTracker;
import edu.um.planet.Universe;
import edu.um.planet.physics.PhysicalObject;

import java.util.concurrent.TimeUnit;

import static java.lang.Math.PI;

public class HohmannTransfer {

    private static Universe universe = new Universe();

    // P1 - orbital periods in seconds
    private static double orbitalPeriodsEarth = 365.25636 * 86400;
    private static double orbitalPeriodsSaturn = 10759.721 * 86400;
    private static double orbitalPeriodTitan = 16 * 86400;

    private static double rotationSpeedEarth = TimeUnit.DAYS.toSeconds(1); //unique to titan
    private static double rotationSpeedSaturn = TimeUnit.HOURS.toSeconds(10) + TimeUnit.MINUTES.toSeconds(42); //unique to titan
    private static double rotationSpeedTitan = orbitalPeriodTitan; //unique to titan

    public static void main(String[] args) {
        new HohmannTransfer().experiment();
    }

    //----


    private double fuel_fill = 57133.947101939135;
    private double dry_mass = 2800E3;

    private double mass = dry_mass;
    private double fuel = fuel_fill;

    public void experiment() {

        FuelTracker totalTracker = new FuelTracker();
        double time_earth_saturn = -1;
        double time_saturn_titan = -1;
        double time_titan_landing = -1;
        double time_titan_saturn = -1;
        double time_saturn_earth = -1;

        while (true) {
            fuel = fuel_fill;
            mass = dry_mass + fuel_fill;
            System.out.println(String.format("Fuel: %e", fuel));

            universe._TIME_DELTA = 1;
            // Earth -> Titan
            Result r = findVelocities(10, 399, 699, orbitalPeriodsEarth, orbitalPeriodsSaturn, rotationSpeedEarth, rotationSpeedSaturn);
            universe._LOOP_ITERATIONS = (int) (r.time * 86400);
            time_earth_saturn = r.time;
            universe.update();
            //---
            updateFuelUsage(totalTracker.add(mass, r.firstPush));
            updateFuelUsage(totalTracker.add(mass, r.secondPush));
            //---

            // Saturn -> Titan
            r = findVelocities(699, 699, 606, orbitalPeriodsSaturn, orbitalPeriodTitan, rotationSpeedSaturn, rotationSpeedTitan);
            universe._LOOP_ITERATIONS = (int) (r.time * 86400);
            time_saturn_titan = r.time;
            universe.update();
            //---
            updateFuelUsage(totalTracker.add(mass, r.firstPush));
            updateFuelUsage(totalTracker.add(mass, r.secondPush));
            //---
            System.out.println(r);

            // Landing
            /*System.out.println("Landing...");
            double geostationary_orbit_end = Math.pow((Universe._G * universe.getCelestialBody(606).getMass() * Math.pow(rotationSpeedTitan, 2)) / (4 * Math.pow(PI, 2)), 1D / 3);
            Simulator simulator = new Simulator(true, new LandingModule(false,
                    new Vector3(Math.random(), 1, Math.random()).multiply(geostationary_orbit_end),
                    universe.getCelestialBody(606).getVelocity().multiply(new Vector3(0.01, -0.01, 0.01)),
                    -Math.PI + 0.005,
                    0.1,
                    ControllerMode.CLOSED));
            double usage = simulator.getLandingModule().getFuelTracker().getUsage();
            totalTracker.addRaw(usage);
            fuel -= usage;
            mass -= usage;
            if(fuel < 0) {
                fuel_fill += Math.abs(fuel);
            }*/


            // Titan -> Saturn
            r = findVelocities(699, 606, 699, orbitalPeriodTitan, orbitalPeriodsSaturn, rotationSpeedTitan, rotationSpeedSaturn);
            universe._LOOP_ITERATIONS = (int) (r.time * 86400);
            time_titan_saturn = r.time;
            universe.update();
            //---
            updateFuelUsage(totalTracker.add(mass, r.firstPush));
            updateFuelUsage(totalTracker.add(mass, r.secondPush));
            //---

            // Saturn -> Earth
            r = findVelocities(10, 699, 399, orbitalPeriodsSaturn, orbitalPeriodsEarth, rotationSpeedSaturn, rotationSpeedEarth);
            time_saturn_earth = r.time;
            //---
            updateFuelUsage(totalTracker.add(mass, r.firstPush));
            if(!(updateFuelUsage(totalTracker.add(mass, r.secondPush)))) {
                continue;
            }
            //---

            System.out.println(String.format("Dry mass: %e | Fueled Mass: %e | Fuel (t): %e | Fuel Price (%.2f/l): %.2fUSD",
                    dry_mass, dry_mass + totalTracker.getUsage(), totalTracker.getUsage() / 1000,
                    FuelTracker.FUEL_PRICE_PER_LITRE, totalTracker.getUsage() * 0.8 * FuelTracker.FUEL_PRICE_PER_LITRE));
            System.out.println(String.format("Earth -> Saturn: %.2f days", time_earth_saturn));
            System.out.println(String.format("Saturn -> Titan: %.2f days", time_saturn_titan));
            System.out.println(String.format("Titan Landing: %.2f days", time_titan_landing));
            System.out.println(String.format("Titan -> Saturn: %.2f days", time_titan_saturn));
            System.out.println(String.format("Saturn -> Earth: %.2f days", time_saturn_earth));
            System.out.println(String.format("Total Time: %.2f days", (time_earth_saturn + time_saturn_titan + time_titan_landing + time_titan_saturn + time_saturn_earth) ));
            break;
        }
    }

    public boolean updateFuelUsage(double fuel_used) {
        fuel -= fuel_used;
        mass -= fuel_used;
        if(fuel < 0) {
            fuel_fill += Math.abs(fuel);
            return false;
        }
        return true;
    }

    public static Result findVelocities(int center, int start, int end, double orbitalPeriodsStart, double orbitalPeriodsEnd,
                                          double rotationTimeStart, double rotationTimeEnd){

        PhysicalObject centerObject = universe.getCelestialBody(center);

        // (m1*V^2)/r = (G*m1*m2)/r^2 -> ((G*m2*T^2)/(4*pi^2))^1/3
        double geostationary_orbit_start = Math.pow((Universe._G * universe.getCelestialBody(start).getMass() * Math.pow(rotationTimeStart, 2)) / (4 * Math.pow(PI, 2)), 1D/3D);
        double geostationary_orbit_end = Math.pow((Universe._G * universe.getCelestialBody(end).getMass() * Math.pow(rotationTimeEnd, 2)) / (4 * Math.pow(PI, 2)), 1D/3);

        if(center != start && center != end) {
            geostationary_orbit_start = 0;
            geostationary_orbit_end = 0;
        } else if(center != start) {
            geostationary_orbit_start = 0;
        } else if(center != end) {
            geostationary_orbit_end = 0;
        }

        final double distanceStartToCenter = universe.getCelestialBody(start).getPosition().subtract(centerObject.getPosition()).add(geostationary_orbit_start).length();
        final double distanceEndToCenter = universe.getCelestialBody(end).getPosition().subtract(centerObject.getPosition()).add(geostationary_orbit_end).length();

        // distance from its center to its furthest side
        final double semi_major_axis = (distanceStartToCenter + distanceEndToCenter) / 2;

        final double periodTransferOrbit = Math.sqrt(((4 * Math.pow(PI, 2)) / (Universe._G * centerObject.getMass()) * Math.pow(semi_major_axis, 3)));

        final double velocityOfStart = (2 * PI * distanceStartToCenter) / orbitalPeriodsStart;
        final double velocityOfEnd = (2 * PI * distanceEndToCenter) / orbitalPeriodsEnd;

        // we need to know how fast the orbit is at the perhelion in order to launch our spacecraft into rhe elliptical orbit from earth's orbit
        final double velocityPerhelion = (2 * PI * semi_major_axis / periodTransferOrbit) * Math.sqrt(((2 * semi_major_axis) / distanceStartToCenter) - 1);
        // crucial for figuring out how much fuel the spacecraft will need
        // how much the velocity of our spacecraft needs to change to switch from Earth's orbit to the tranfer orbit
        final double firstChangeInVelocity = velocityPerhelion - velocityOfStart;

        // aphelion - end of the ellipse furthest from sun, ergo, the end that lines up with the orbit of Saturn
        final double velocityAphelion = (2 * PI * semi_major_axis / periodTransferOrbit) * Math.sqrt((2 * semi_major_axis / distanceEndToCenter) - 1);

        // change in velocity necessary to send the spacecraft from the elliptical transfer orbit into Mars' orbit
        final double secondChangeInVelocity = velocityOfEnd - velocityAphelion;

        // in days
        final double timeOfFlightDays = TimeUnit.SECONDS.toDays((long) (0.5D * periodTransferOrbit));
        //System.out.println("The amount of days the flight takes: " + timeOfFlightDays);

        final double[] velocities = new double[2];
        velocities[0] = firstChangeInVelocity;
        velocities[1] = secondChangeInVelocity;

        return new Result(firstChangeInVelocity, secondChangeInVelocity, timeOfFlightDays);
    }

    public static class Result {

        public double firstPush;
        public double secondPush;
        public double time;

        public Result(double firstPush, double secondPush, double time) {
            this.firstPush = firstPush;
            this.secondPush = secondPush;
            this.time = time;
        }

        @Override
        public String toString() {
            return String.format("%s %s %.4f", firstPush, secondPush, time);
        }
    }


}
