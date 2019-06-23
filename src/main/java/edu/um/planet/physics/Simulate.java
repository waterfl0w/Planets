package edu.um.planet.physics;

import edu.um.planet.Universe;
import edu.um.planet.math.Vector3;

import java.time.Instant;
import java.util.List;
import java.util.LinkedList;

public class Simulate {

    public static void main(String[] args) {
        new Simulate();
    }

    private Universe universe = new Universe();

    public Simulate() {

        long time = System.currentTimeMillis();
        final double seconds = 929292307.2 / 10;
        universe._LOOP_ITERATIONS = (int) (seconds / 60);

        List<Vector3> positions = new LinkedList<>();
        List<Instant> times = new LinkedList<>();

        Vector3 initialEarthPosition = universe.getObject("Earth (399)").getPosition().clone();
        Vector3 initialEarthVelocity = universe.getObject("Earth (399)").getVelocity().clone();

        universe.update(universe -> {

            double distanceEarthSaturn = universe.getObject("Earth (399)").getPosition().subtract(universe.getObject("Saturn (699)").getPosition()).length();
            double distanceEarthTitan = universe.getObject("Earth (399)").getPosition().subtract(universe.getObject("Titan (606)").getPosition()).length();

            if(distanceEarthTitan < distanceEarthSaturn) {
                positions.add(universe.getObject("Titan (606)").getPosition().clone());
                times.add(universe.getCurrentTime());
            }

        });
        System.out.println(System.currentTimeMillis() - time);

        Vector3 titan = positions.get(times.size()/2);
        Instant timeNow = times.get(times.size()/2);

        double speed = titan.subtract(initialEarthPosition).length() / ((timeNow.toEpochMilli() - universe.getStartTime().toEpochMilli()) / 1000D);
        Vector3 direction = titan.subtract(initialEarthPosition);
        final Vector3 velocityChange  = direction.normalise().multiply(speed).subtract(initialEarthVelocity);
        System.out.println("min: " + velocityChange);


    }

}
