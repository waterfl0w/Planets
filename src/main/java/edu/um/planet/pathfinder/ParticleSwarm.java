package edu.um.planet.pathfinder;

import edu.um.planet.Universe;
import edu.um.planet.physics.CannonBall;
import edu.um.planet.physics.PhysicalObject;
import edu.um.planet.math.Vector3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ParticleSwarm {

    public static void main(String[] args) {
        ParticleSwarm particleSwarm = new ParticleSwarm();
        particleSwarm.setup();
        particleSwarm.run();
    }

    //--- CONFIGURATION
    private static final int start = 399;
    private static final int end = 499;

    private static final int _SIZE = 100;
    private static final int _ITERATIONS = 1000;

    private final static double accelerationInSeconds = TimeUnit.MINUTES.toSeconds(60);

    //---
    private List<Particle> particles = new ArrayList<>(_SIZE);

    private Universe universe = new Universe();

    public ParticleSwarm() { }

    public void setup() {
        // save the current state of the universe
        this.universe.save();

        // configure universe
        this.universe._TIME_DELTA = 60;
        this.universe._LOOP_ITERATIONS = 30;

        // direction between start and end bodies
        final Vector3 direction = universe.getCelestialBody(end).getPosition().subtract(universe.getCelestialBody(start).getPosition()).normalise();

        for(int i = 0; i < _SIZE; i++) {
            Vector3 acceleration = direction.multiply(Vector3.randomSigns().multiply(new Vector3(30000, 30000, 30000)));

            PhysicalObject spawn = universe.getCelestialBody(start);
            this.particles.add(new Particle(
                    new CannonBall(-i-1,
                            HohmannTransfer.DRY_MASS,
                            -1,
                            universe.getCelestialBody(end),
                            spawn.getPosition().add(
                                    new Vector3(spawn.getRadius(), spawn.getRadius(), spawn.getRadius()).multiply(Vector3.randomNormalised()).multiply(Vector3.randomSigns())
                            ),
                            spawn.getVelocity(),
                            acceleration,
                            accelerationInSeconds)
                    )
            );
        }
    }

    public void run() {

        for(int i = 0; i < _ITERATIONS; i++) {

            //--- 1. add particles
            for(Particle particle : this.particles) {
                this.universe.getBodies().add(particle.getBall());
            }

            double min_distance = Double.MAX_VALUE;

            //--- 2. simulate universe
            while (true) {

                boolean isAnyoneStillGettingCloser = false;
                for(Particle particle : particles) {
                    //--- 4. save the best state of every drone even if other drones still keep flying towards the target
                    // NOTE: The particle is no longer getting closer to the target & we haven't saved it yet.
                    if(!particle.getBall().isStillGettingCloser() && !particle.isLocalDone()) {
                        particle.setIsLocalDone(true);
                        particle.saveLocal();
                        min_distance = Math.min(min_distance, universe.getCelestialBody(end).getPosition().subtract(particle.getBall().getPosition()).length());
                    } else if(particle.getBall().isStillGettingCloser()) {
                        isAnyoneStillGettingCloser = true;
                    }
                }

                //--- 3. stop simulating universe as soon as none of the drones get any better
                // if nobody gets closer to the target then we are done with updating the universe
                if(!isAnyoneStillGettingCloser) {
                    break;
                }

                universe.update();

            }

            //--- 5. calculate the cost
            Particle bestSwarmParticle = null;
            for(Particle particle : particles) {
                particle.setIsLocalDone(false); //reset the is local done value for the next run

                final double error = cost(universe, particle);
                System.out.println(error);
                particle.updateError(error);
                if(bestSwarmParticle == null || error < bestSwarmParticle.getError()) {
                    bestSwarmParticle = particle;
                }
            }
            // & update the best values in each particle
            for(Particle particle : particles) {
                particle.updateState(bestSwarmParticle);
            }

            //--- 6. reset universe
            System.out.println("MIN_DISTANCE -> " + min_distance);
            this.universe.recover();

        }

    }

    // NOTE: This is the cost/error function that we are trying to minimise.
    public static double cost(Universe u, Particle particle) {
        return particle.getLocalDistanceToTarget();
        /*final double s1 = 1;
        final double s2 = 0;

        return -particle.getBall().getMass()
                + s1 * particle.getLocalDistanceToTarget()
                + s2 * particle.getLocalVelocity().length();*/
    }

    public class Particle {

        private CannonBall cannonBall;

        //--- The data associated with the current state of the particle.
        private Vector3 position;
        private Vector3 acceleration;
        private double error = Double.MAX_VALUE;

        //--- The best data that has ever been associated with this particle in its life-timeInSeconds.
        private Vector3 best_position;
        private Vector3 best_acceleration;
        private double best_error;

        //--- Stored after not getting any closer to the target
        private Vector3 local_position;
        private Vector3 local_velocity;
        private double local_distance_to_target;
        private boolean isLocalDone = false;

        public Particle(CannonBall cannonBall) {
            // NOTE: No defense-copy required since Vector3s are immutable.
            this.cannonBall = cannonBall;
            this.position = cannonBall.getPosition();
            this.acceleration = cannonBall.getAcceleration();
        }

        public void updateError(double e) {
            if(e < this.error) {
                this.best_error = e;
                this.best_position = position;
                this.best_acceleration = acceleration;
            }
            this.error = e;
        }

        public void updateState(Particle swarmBest) {

            this.position = position.add(swarmBest.getPosition()).add(best_position);
            this.acceleration = acceleration.add(swarmBest.getAcceleration().divide(swarmBest.getBall().getMass())).add(best_acceleration);

            PhysicalObject spawn = universe.getCelestialBody(start);

            this.cannonBall = new CannonBall(
                    this.cannonBall.getId(),            // id
                    HohmannTransfer.DRY_MASS,
                    -1,
                    universe.getCelestialBody(end),     // end object
                    this.position,                      // position @ start
                    spawn.getVelocity(),                // velocity @ start
                    this.acceleration,                  // acceleration @ start
                    accelerationInSeconds               // timeInSeconds to accelerate the object in seconds
            );

        }

        // saves the current position & velocity of the cannon ball
        public void saveLocal() {
            this.local_position = this.cannonBall.getPosition();
            this.local_velocity = this.cannonBall.getVelocity();
            this.local_distance_to_target = universe.getCelestialBody(end).getPosition().subtract(local_position).length();
        }

        public boolean isLocalDone() {
            return this.isLocalDone;
        }

        public void setIsLocalDone(boolean isLocalDone) {
            this.isLocalDone = isLocalDone;
        }

        public CannonBall getBall() {
            return this.cannonBall;
        }

        public Vector3 getPosition() {
            return position;
        }

        public Vector3 getAcceleration() {
            return this.acceleration;
        }

        public double getError() {
            return error;
        }

        public Vector3 getBestPosition() {
            return best_position;
        }

        public Vector3 getBestAcceleration() {
            return best_acceleration;
        }

        public double getBestError() {
            return best_error;
        }

        public Vector3 getLocalPosition() {
            return this.local_position;
        }

        public Vector3 getLocalVelocity() {
            return this.local_velocity;
        }

        public double getLocalDistanceToTarget() {
            return this.local_distance_to_target;
        }

    }

}
