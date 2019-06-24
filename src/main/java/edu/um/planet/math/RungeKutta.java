package edu.um.planet.math;

public class RungeKutta {

    private static Vector3 force;

    public static Vector3[] solve(Vector3 velo, Vector3 posi, Interval interval, Vector3 force) {
        double dt  = 1;
        final int n = (int) Math.ceil(Math.abs(interval.b() - interval.a()) /dt + dt);
        assert n == (int) Math.ceil(Math.abs(interval.b() - interval.a()) /dt + dt);
        RungeKutta.force = force;

        for(int i = 0; i < n - 1; i++) {

            double ti = interval.a() + (i * dt);
            Vector3 k1 = f1(ti, posi, velo).multiply(dt);
            Vector3 l1 = f2(ti, posi, velo).multiply(dt);
            Vector3 k2 = f1(ti + dt/2D, posi.add(k1.divide(2)).multiply(dt), velo.add(((l1).divide(2))).multiply(dt));
            Vector3 l2 = f2(ti + dt/2D, posi.add(k1.divide(2)).multiply(dt), velo.add(((l1).divide(2))).multiply(dt));
            Vector3 k3 = f1(ti + dt/2D, posi.add(k2.divide(2)).multiply(dt), velo.add(((l2).divide(2))).multiply(dt));
            Vector3 l3 = f2(ti + dt/2D, posi.add(k2.divide(2)).multiply(dt), velo.add(((l2).divide(2))).multiply(dt));
            Vector3 k4 = f1(ti + dt, posi.add((k3.multiply(dt))), velo.add((l3.multiply(dt))));
            Vector3 l4 = f2(ti + dt, posi.add((k3.multiply(dt))), velo.add((l3.multiply(dt))));

            Vector3 k = k1.add((k2.multiply(2))).add((k3.multiply(2))).add(k4);
            Vector3 l = l1.add((l2.multiply(2))).add((l3.multiply(2))).add(l4);

            posi = posi.add(k.multiply(dt/6));
            velo = velo.add(l.multiply(dt/6));


        }
        return new Vector3[] {posi,velo};
    }


    public static Vector3 f1(double tim, Vector3 pos, Vector3 vel){
        return vel;
    }
    public static Vector3 f2(double tim, Vector3 pos, Vector3 vel){
        return force;
    }

}