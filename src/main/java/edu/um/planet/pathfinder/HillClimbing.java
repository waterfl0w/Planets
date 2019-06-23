package edu.um.planet.pathfinder;

import edu.um.planet.Universe;
import edu.um.planet.physics.CannonBall;
import edu.um.planet.math.Vector3;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


// finding titan 
public class HillClimbing {

    public static void main(String[] args){
        run();
    }

    public static void run(){

        final int SPAWN = 399;
        final int TARGET = 499;

        Universe inital_universe = new Universe();
        // velocity of earth
        Vector3 a =  new Vector3(1,1,1).multiply(inital_universe.getCelestialBody(SPAWN).getVelocity().normalise());
        Vector3 a_old = null;
        Vector3 b = new Vector3(1,1,1);
        Vector3 target = inital_universe.getCelestialBody(TARGET).getPosition();

        boolean found = false;

        // runs until no probe hits titan
        while(!found){

            Universe universe = new Universe();
            universe._TIME_DELTA = 1;
            Map<Integer, Vector3> initialA = new HashMap<>();

            // creating several different probes
            /*for(int i=0; i<500; i++) {
                // changing variables for each probe
                Vector3 x = a.add(new Vector3(100 * (i - initialA.size() / 2), 100 * (i - initialA.size() / 2) , 100 * (i - initialA.size() / 2) ).multiply(b));
                universe.getBodies().add(new CannonBall(-i, universe.getCelestialBody(SPAWN), universe.getCelestialBody(TARGET), target, x));
                initialA.put(-i, x);
            }*/
            // runs until no better probe is found
            boolean changed = false;
            int runs = 0;
            double minDistance = Double.MAX_VALUE;

            //Map<Integer, Double> probes = new HashMap<>();
            int id = -1;
            final double timeStep = TimeUnit.DAYS.toSeconds(14);
            double countdown = timeStep;
            int key = -1;
            while (true) {
                runs++;
                countdown -= universe._TIME_DELTA * universe._LOOP_ITERATIONS;
                if(countdown < 0) {
                    initialA.put(key, new Vector3());
                    key--;
                    countdown = timeStep;
                }

                universe.update();

                boolean t = false;
                for (int i = 0; i < initialA.size(); i++) {
                    CannonBall ball = (CannonBall) universe.getCelestialBody(-i);
                    if (ball.isAccelerating() && !ball.hasHit()) {
                        t = true;
                        break;
                    }
                }
                if(t) continue;

                for (int i = 0; i < initialA.size(); i++) {
                    CannonBall ball = (CannonBall) universe.getCelestialBody(-i);
                    double distance = ball.getTargetCoordinate().subtract(universe.getCelestialBody(-i).getPosition()).length();
                    // if probe hits titan
                    if (ball.hasHit()) {
                        minDistance = distance;
                        System.out.println(initialA.get(-i));
                        found = true;
                        // if we find a new probe with a smaller distance, update minDistance, Vector b and id
                    } else if (distance < minDistance) {
                        minDistance = distance;
                        // adjusting the vector
                        b = ball.getTargetCoordinate().subtract(universe.getCelestialBody(-i).getPosition()).normalise();
                        changed = true;
                        id = -i;
                        //probes.put(-i, distance);
                    }

                }
                if (!changed) {
                    break;
                }
                changed = false;
            }


            /*int id = probes.entrySet().stream().sorted((o1, o2) -> Double.compare(o2.getValue(), o1.getValue()))
                    .limit(10).skip((int)(9 * Math.random())).findFirst().get().getKey();*/;
            a_old = a;
            a = initialA.get(id);

            if(a.subtract(a_old).length() < 1) {
                universe.update();
                target = universe.getCelestialBody(TARGET).getPosition();
                System.out.println(target);
            }
            System.out.println(minDistance + " -> " + a);
            //System.out.println(probes.get(id) + " -> " + initialA.get(id));
        }

    }

}
