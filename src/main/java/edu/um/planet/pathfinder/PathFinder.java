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
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PathFinder {

    public static void main(String[] args) {
        new PathFinder(EARTH_GEO);
    }

    public final static double EARTH_GEO = 532E3;
    public final static double TITAN_GEO = 343E3;

    //---
    private Universe universe = new Universe();
    private PhysicalObject start = universe.getCelestialBody(606);
    private PhysicalObject end = universe.getCelestialBody(301);
    private double geo;

    private int globalId = -1;
    private final HohmannTransfer.MinimalValues minimalValues;

    public PathFinder(double geo) {
        this.minimalValues = new HohmannTransfer().calculateMinimalFuelUsage();
        this.geo = geo;

        new Debugger(universe, false);
        universe._TIME_DELTA = 60;
        universe._LOOP_ITERATIONS = 1;
        calculate();
    }

    public void calculate() {
        //--- Monthly --> Weekly
        universe.save();

        List<Interval> monthly = simulateTimePeriod(0, TimeUnit.DAYS.toMinutes(7), 60, 10);

        for(Interval interval : monthly) {
            System.out.println(interval.a() + " -> " + ((interval.b() - interval.a()) / TimeUnit.DAYS.toMinutes(1)));
            double timeDelta = TimeUnit.DAYS.toMinutes(1);
            int tests = (int) MathUtil.clamp(Interval.of(1, 20), (int) Math.ceil((interval.b() - interval.a()) / timeDelta));
            simulateTimePeriod((long) interval.a(), timeDelta, tests, (int) Math.ceil(tests * 0.3333));
        }

    }

    public List<Interval> simulateTimePeriod(long simulationOffset, double timeDelta, int tests, int limit) {

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
        double min_distance = Double.MAX_VALUE;
        CannonBall closestRocket = null;
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
                    rocket.timeMeta.travelTime = rocket.timeMeta.timeOffset - universe.getTimeSinceStart();
                    rocket.timeMeta.distance = end.getPosition().subtract(rocket.cannonBall.getPosition()).length();
                    anyoneGettingCloser = true;
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
                .filter(e -> {
                    FuelTracker fuelTracker = new FuelTracker();
                    fuelTracker.add(e.cannonBall.getMass(), e.cannonBall.getAcceleration().length() * TimeUnit.MINUTES.toSeconds(10));
                    System.out.println(fuelTracker.getUsage() / minimalValues.fuel);
                    return fuelTracker.getUsage() < (minimalValues.fuel * 0.75);
                })
                .filter(e -> e.timeMeta.travelTime < 0.5 * minimalValues.timeInSeconds)
                .limit(limit)
                .sorted(Comparator.comparingDouble(o -> o.timeMeta.timeOffset)).collect(Collectors.toList());
        List<Interval> intervals = new ArrayList<>();
        for (int i = 0; i < sortedRockets.size() - 1; i++) {
            Rocket a = sortedRockets.get(i);
            Rocket b = sortedRockets.get(i+1);
            if (a.timeMeta.travelTime > 1 && b.timeMeta.travelTime > 1) {
                intervals.add(Interval.of(a.timeMeta.timeOffset, b.timeMeta.timeOffset));
            }
        }

        System.out.println(intervals.size());
        universe.recover(); // recovery before going on
        return intervals;

    }


    private List<CannonBall> spawnRockets(Vector3 startPosition, List<TimeMeta> metas) {
        List<CannonBall> rockets = new ArrayList<>();
        double geo = 42164E3;
        for(TimeMeta meta : metas) {
            Vector3 distance = meta.position.subtract(startPosition);
            Vector3 dir = distance.normalise();
            double speed = distance.length() / meta.timeOffset / TimeUnit.MINUTES.toSeconds(10);

            // int id, PhysicalObject target, Vector3 position, Vector3 velocity, Vector3 acceleration, double accelerateInSeconds
            CannonBall rocket = new CannonBall(
                    globalId,
                    HohmannTransfer.DRY_MASS,
                    geo,
                    end,
                    startPosition.add(dir.multiply(geo)),
                    start.getVelocity(),
                    dir.multiply(speed),
                    TimeUnit.MINUTES.toSeconds(10)
            );
            System.out.println("add rocket");
            rockets.add(rocket);
            universe.getBodies().add(rocket);
        }
        globalId--;

        return rockets;
    }

    public class TimeMeta {

        public Vector3 position;
        public double timeOffset;
        public double travelTime;
        public double distance;

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

}
