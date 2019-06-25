package edu.um.planet.pathfinder;

import edu.um.landing.FuelTracker;
import edu.um.planet.Universe;
import edu.um.planet.gui.Debugger;
import edu.um.planet.math.Interval;
import edu.um.planet.math.MathUtil;
import edu.um.planet.physics.CannonBall;
import edu.um.planet.physics.PhysicalObject;
import edu.um.planet.math.Vector3;

import java.util.ArrayList;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PathFinder {

    public static void main(String[] args) {
         HohmannTransfer.MinimalValues minimalValues = new HohmannTransfer().calculateMinimalFuelUsage();

        new PathFinder(399, 606, TITAN_GEO, minimalValues.timeInSeconds / 2.1D, minimalValues.fuel * 0.75, 0);
        //new PathFinder(606, 399, EARTH_GEO, minimalValues.timeInSeconds - (63072000 * 1.5), minimalValues.fuel * (0.5352), (3.153600e+07 - 3.153600e+07));
    }

    public final static double EARTH_GEO = 532E3;
    public final static double TITAN_GEO = 343E3;

    //---
    private Universe universe = new Universe();
    private PhysicalObject start;
    private PhysicalObject end;
    private double geo;

    private int globalId = -1;
    private double timeConstraint;
    private double fuelConstraint;

    public PathFinder(int start, int end, double geo, double timeConstraint, double fuelConstraint, double timeOffset) {
        this.geo = geo;
        this.start = universe.getCelestialBody(start);
        this.end = universe.getCelestialBody(end);
        this.timeConstraint = timeConstraint;
        this.fuelConstraint = fuelConstraint;

        new Debugger(universe, false);
        if(timeOffset > 0) {
            universe._TIME_DELTA = 1;
            universe._LOOP_ITERATIONS = (int) timeOffset;
            universe.update();
        }

        universe._TIME_DELTA = 60;
        universe._LOOP_ITERATIONS = 60;
        calculate();
    }

    public void calculate() {
        //--- Yearly --> Months
        universe.save();

        List<Result> yearly = simulateTimePeriod(0, TimeUnit.DAYS.toMinutes(365), 8, 4);
        System.out.println("------------------------------ YEARLY ------------------------------");
        for (Result result : yearly) {
            System.out.println(String.format("%e - %e - %e - %e (%.4f)", result.rocket.timeMeta.timeOffset, result.rocket.timeMeta.travelTime, result.rocket.timeMeta.distance, result.fuelUsage, result.fuelUsage / fuelConstraint));
        }

        List<Result> monthly = new ArrayList<>();
        for(Result result : yearly) {
            Interval interval = result.interval;
            double timeDelta = TimeUnit.DAYS.toMinutes(30);
            int tests = (int) MathUtil.clamp(Interval.of(1, 12), (int) Math.ceil((interval.b() - interval.a()) / timeDelta));
            monthly.addAll(simulateTimePeriod((long) interval.a(), timeDelta, tests, (int) Math.ceil(tests * 1)));
        }
        System.out.println("------------------------------ MONTHLY ------------------------------");
        for (Result result : monthly) {
            System.out.println(String.format("%e - %e - %e - %e (%.4f)", result.rocket.timeMeta.timeOffset, result.rocket.timeMeta.travelTime, result.rocket.timeMeta.distance, result.fuelUsage, result.fuelUsage / fuelConstraint));
        }

        //--- Months --> Weeks
        List<Result> weekly = new ArrayList<>();
        for(Result result : monthly) {
            Interval interval = result.interval;
            double timeDelta = TimeUnit.DAYS.toMinutes(7);
            int tests = (int) MathUtil.clamp(Interval.of(1, 4), (int) Math.ceil((interval.b() - interval.a()) / timeDelta));
            weekly.addAll(simulateTimePeriod((long) interval.a(), timeDelta, tests, (int) (tests * 0.5)));
        }
        System.out.println("------------------------------ WEEKLY ------------------------------");
        for (Result result : weekly) {
            System.out.println(String.format("%e - %e - %e - %e (%.4f)", result.rocket.timeMeta.timeOffset, result.rocket.timeMeta.travelTime, result.rocket.timeMeta.distance, result.fuelUsage, result.fuelUsage / fuelConstraint));
        }


        //--- Weeks -> Days
        List<Result> days = new ArrayList<>();
        for(Result result : weekly) {
            Interval interval = result.interval;
            double timeDelta = TimeUnit.DAYS.toMinutes(1);
            int tests = (int) MathUtil.clamp(Interval.of(1, 7), (int) Math.ceil((interval.b() - interval.a()) / timeDelta));
            days.addAll(simulateTimePeriod((long) interval.a(), timeDelta, tests, (int) (tests * 0.5)));
        }
        System.out.println("------------------------------ DAILY ------------------------------");
        for (Result result : days) {
            System.out.println(String.format("%e - %e - %e - %e (%.4f)", result.rocket.timeMeta.timeOffset, result.rocket.timeMeta.travelTime, result.rocket.timeMeta.distance, result.fuelUsage, result.fuelUsage / fuelConstraint));
        }



    }

    public List<Result> simulateTimePeriod(long simulationOffset, double timeDelta, int tests, int limit) {

        //--- offset the simulation and save
        if(simulationOffset > 0) {
            universe._TIME_DELTA = 60;
            universe._LOOP_ITERATIONS = (int) (simulationOffset / 60D);
            universe.update();
        }
        //---

        List<TimeMeta> futurePoints = new ArrayList<>();
        universe._LOOP_ITERATIONS = (int) timeDelta;
        for (int i = 0; i < tests; i++) {
            universe.update();
            System.out.println(i);
            futurePoints.add(new TimeMeta(end.getPosition().clone(), universe.getTimeSinceStart()));
        }

        final List<Rocket> rockets = new ArrayList<>();
        Debugger.lockSimulation(() -> {
            universe.recover();
            start = universe.getCelestialBody(start.getId());
            end = universe.getCelestialBody(end.getId());
            //rockets = spawnRockets(start.getPosition(), futurePoints);
        });

        boolean anyoneGettingCloser = true;
        boolean allLaunched = false;
        universe._LOOP_ITERATIONS = 60;
        universe._TIME_DELTA = 60 * 30;

        while (anyoneGettingCloser){

            if(!allLaunched) {
                Debugger.lockSimulation(() -> {
                    for (int i = 0; i < futurePoints.size(); i++) {
                        TimeMeta meta = futurePoints.get(i);
                        Vector3 dir = start.getPosition().dir(meta.position);
                        Vector3 startPosition = start.getPosition().add(dir.multiply(geo));
                        double distance = meta.position.subtract(startPosition).length();
                        double speed = distance / meta.timeOffset / TimeUnit.MINUTES.toSeconds(10);

                        // int id, PhysicalObject target, Vector3 position, Vector3 velocity, Vector3 acceleration, double accelerateInSeconds
                        CannonBall rocket = new CannonBall(
                                globalId,
                                HohmannTransfer.DRY_MASS,
                                geo,
                                end,
                                startPosition.add(dir.multiply(geo)),
                                start.getVelocity().multiply(0),
                                dir.multiply(speed),
                                TimeUnit.MINUTES.toSeconds(10)
                        );
                        universe.getBodies().add(rocket);
                        rockets.add(new Rocket(meta, rocket));
                        globalId--;
                    }
                });
                allLaunched = true;
            }

            anyoneGettingCloser = false;

            for(Rocket rocket : rockets) {
                if(rocket.cannonBall.isStillGettingCloser()) {
                    rocket.timeMeta.travelTime = universe.getTimeSinceStart();
                    rocket.timeMeta.distance = end.getPosition().subtract(rocket.cannonBall.getPosition()).length();
                    anyoneGettingCloser = true;
                } else if(!rocket.timeMeta.hasData) {
                    rocket.timeMeta.travelTime = universe.getTimeSinceStart();
                    rocket.timeMeta.distance = end.getPosition().subtract(rocket.cannonBall.getPosition()).length();
                    rocket.timeMeta.hasData = true;
                }
            }

            universe.update();

        }

        rockets.sort(new Comparator<Rocket>() {
            @Override
            public int compare(Rocket o1, Rocket o2) {
                return Double.compare(score(o1), score(o2));
            }

            public double score(Rocket rocket) {
                FuelTracker fuelTracker = new FuelTracker();
                fuelTracker.add(rocket.cannonBall.getMass(), rocket.cannonBall.getAcceleration().length() * TimeUnit.MINUTES.toSeconds(10));
                return fuelTracker.getUsage() / rocket.timeMeta.travelTime / (1D / rocket.timeMeta.distance);
            }

        });

        /*for(Rocket rocket : rockets) {
            FuelTracker fuelTracker = new FuelTracker();
            fuelTracker.add(rocket.cannonBall.getMass(), rocket.cannonBall.getAcceleration().length() * TimeUnit.MINUTES.toSeconds(10));
            System.out.printf("%e\t|%d\t|%e\t|\n", rocket.timeMeta.distance, TimeUnit.SECONDS.toDays((long) rocket.timeMeta.travelTime), fuelTracker.getUsage());
        }*/

        List<Rocket> sortedRockets = rockets.stream()
                .filter(e -> e.timeMeta.travelTime > 1)
                .filter(e -> e.timeMeta.travelTime <= timeConstraint)
                .filter(e -> {
                    FuelTracker fuelTracker = new FuelTracker();
                    fuelTracker.add(e.cannonBall.getMass(), e.cannonBall.getAcceleration().length() * TimeUnit.MINUTES.toSeconds(10));
                    System.out.println(fuelTracker.getUsage() / this.fuelConstraint);
                    return fuelTracker.getUsage() < this.fuelConstraint;
                })
                .limit(limit)
                .sorted(Comparator.comparingDouble(o -> o.timeMeta.travelTime + o.timeMeta.timeOffset)).collect(Collectors.toList());
        List<Result> intervals = new ArrayList<>();
        for (int i = 0; i < sortedRockets.size() - 1; i++) {
            Rocket a = sortedRockets.get(i);
            Rocket b = sortedRockets.get(i+1);
            FuelTracker fuelTracker = new FuelTracker();
            fuelTracker.add(a.cannonBall.getMass(), a.cannonBall.getAcceleration().length() * TimeUnit.MINUTES.toSeconds(10));
            intervals.add(new Result(a, fuelTracker.getUsage(), Interval.of(a.timeMeta.timeOffset, b.timeMeta.timeOffset)));
        }

        System.out.println(intervals.size());
        universe.recover(); // recovery before going on
        return intervals;

    }


    public class TimeMeta {

        public Vector3 position;
        public double timeOffset;
        public double travelTime;
        public double distance;
        public boolean hasData = false;

        public TimeMeta(Vector3 position, double timeOffset) {
            this.position = position;
            this.timeOffset = timeOffset;
        }

    }

    public class Rocket {
        public TimeMeta timeMeta;
        public CannonBall cannonBall;

        public Rocket(TimeMeta timeMeta, CannonBall cannonBall) {
            this.timeMeta = timeMeta;
            this.cannonBall = cannonBall;
        }

    }

    public class Result {

        public Interval interval;
        public Rocket rocket;
        public double fuelUsage;

        public Result(Rocket rocket, double fuelUsage, Interval interval) {
            this.interval = interval;
            this.fuelUsage = fuelUsage;
            this.rocket = rocket;
        }

    }

}
