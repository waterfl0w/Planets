package edu.um.planet.math;

import edu.um.planet.Universe;

public class SimulateAccelerationTime {

    public static void main(String[] args) {
        double a = -1;
        double closest = Double.MAX_VALUE;
        Vector3 dir = null;
        for(double x = 19; x <= 1000; x += 100) {
            Universe universe = new Universe();
            //universe.getBodies().add(new CannonBall((int) -x, universe.getObject("Earth (399)"), universe.getObject("Titan (606)"), new Vector3(28326.4654,-15065.0340,-10.7450)));
            for(int i = 0; i < 10000; i++) {
                double distance = universe.getObject("Titan (606)").getPosition().subtract(universe.getCelestialBody(-100).getPosition()).length();
                if(distance < closest) {
                    closest = distance;
                    a = x;
                    dir = universe.getObject("Titan (606)").getPosition().subtract(universe.getCelestialBody(-100).getPosition()).normalise();
                }
                universe.update();
            }


        }
        System.out.println(dir);
        System.out.println("Closest: " + closest);
        System.out.println("a: " + a);

    }

}
