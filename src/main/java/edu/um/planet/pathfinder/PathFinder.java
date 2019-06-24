package edu.um.planet.pathfinder;

import edu.um.landing.FuelTracker;
import edu.um.planet.Universe;
import edu.um.planet.gui.Debugger;
import edu.um.planet.physics.CannonBall;
import edu.um.planet.physics.PhysicalObject;
import edu.um.planet.math.Vector3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PathFinder {

    public static void main(String[] args) {
        new PathFinder();
    }

    //---
    private final static double geo = 42164E3;
    private Universe universe = new Universe();
    private PhysicalObject start = universe.getCelestialBody(399);
    private PhysicalObject end = universe.getCelestialBody(606);

    private int globalId = -1;


    public PathFinder() {
        new Debugger(universe, false);
        universe._TIME_DELTA = 60;
        universe._LOOP_ITERATIONS = 1;
        universe.save();
        simulateTimePeriod(TimeUnit.DAYS.toMinutes(7), 30);
    }

    public void simulateTimePeriod(double timeDelta, double tests) {

        for (int j = 0; j < 1; j++) {

            List<TimeMeta> futurePoints = new ArrayList<>();
            universe._LOOP_ITERATIONS = (int) timeDelta;
            for (int i = 0; i < tests; i++) {
                universe.update();
                System.out.println(i);
                futurePoints.add(new TimeMeta(end.getPosition().clone(), universe.getTimeSinceStart()));
            }

            List<Rocket> rockets = new ArrayList<>();
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
            final AtomicInteger launchIndex = new AtomicInteger(0);

            final double timing = timeDelta;
            double timeToNextStart = timing;

            while (anyoneGettingCloser || !allLaunched){
                timeToNextStart -= universe.getUpdateStep();

                if(launchIndex.get() < futurePoints.size() && timeToNextStart <= 0) {
                    Debugger.lockSimulation(() -> {
                        TimeMeta meta = futurePoints.get(launchIndex.get());
                        Vector3 dir = start.getPosition().dir(meta.position);
                        Vector3 startPosition = start.getPosition().add(dir.multiply(geo));
                        double distance = meta.position.subtract(startPosition).length();
                        double speed = distance / meta.timeOffset / TimeUnit.MINUTES.toSeconds(10);

                        // int id, PhysicalObject target, Vector3 position, Vector3 velocity, Vector3 acceleration, double accelerateInSeconds
                        CannonBall rocket = new CannonBall(
                                globalId,
                                end,
                                startPosition.add(dir.multiply(geo)),
                                start.getVelocity().multiply(0),
                                dir.multiply(speed),
                                TimeUnit.MINUTES.toSeconds(10)
                        );
                        universe.getBodies().add(rocket);
                        rockets.add(new Rocket(meta, rocket));
                        globalId--;
                    });
                    timeToNextStart = timing;
                    launchIndex.incrementAndGet();
                } else if(launchIndex.get() >= futurePoints.size()) {
                    allLaunched = true;
                    System.out.println("all launched");
                }

                anyoneGettingCloser = false;

                for(Rocket rocket : rockets) {
                    if(rocket.cannonBall.isStillGettingCloser()) {
                        rocket.timeMeta.travelTime = universe.getTimeSinceStart() - rocket.timeMeta.timeOffset;
                        rocket.timeMeta.distance = end.getPosition().subtract(rocket.cannonBall.getPosition()).length();
                        anyoneGettingCloser = true;
                    }
                    double distance = end.getPosition().subtract(rocket.cannonBall.getPosition()).length();
                    if(distance < min_distance) {
                        min_distance = distance;
                        closestRocket = rocket.cannonBall;
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

            for(Rocket rocket : rockets) {
                FuelTracker fuelTracker = new FuelTracker();
                fuelTracker.add(rocket.cannonBall.getMass(), rocket.cannonBall.getAcceleration().length() * TimeUnit.MINUTES.toSeconds(10));
                System.out.printf("%e\t|%d\t|%e\t|\n", rocket.timeMeta.distance, TimeUnit.SECONDS.toDays((long) rocket.timeMeta.travelTime), fuelTracker.getUsage());
            }

            System.out.println(min_distance);
        }


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

    private Vector3 direction(PhysicalObject start, PhysicalObject target) {
        return target.getPosition().subtract(start.getPosition()).normalise();
    }

    public class TimeMeta {

        public Vector3 position;
        public double timeOffset;
        public double travelTime;
        public boolean savedTravelTime = false;
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
