package edu.um.planet.physics;

import edu.um.planet.Universe;
import edu.um.planet.math.Vector3;

import java.awt.*;
import java.util.List;

public class CannonBall extends PhysicalObject {

    private Vector3 acceleration;
    private Vector3 targetCoordinate;
    private double accelerateInSeconds = 20 * 60;
    private boolean isHit;
    private PhysicalObject target;

    // TODO/NOTE: Does the probe still get closer? This is specific to the particle swarm implementation but should work
    //  in general as well... not guaranteed tho...
    private boolean stillGettingCloser = true;
    private double minDistanceToTarget = Double.MAX_VALUE;

    // change ID, spawn is earth, target titan and change acceleration
    public CannonBall(int id, PhysicalObject spawn, PhysicalObject target, Vector3 targetCoordinate, Vector3 acceleration) {
        super(id,"Cannon Ball", Color.RED, 1, 300, spawn.getPosition().add(new Vector3(spawn.getRadius(), spawn.getRadius(), spawn.getRadius()).multiply(spawn.getVelocity().normalise())), spawn.getVelocity());
        this.setVelocity(spawn.getVelocity());
        this.acceleration = acceleration;
        this.target = target;
        this.targetCoordinate = targetCoordinate;
    }

    // NOTE: Not clean but this construction should exclusively be used for Particle Swarm as it makes some changes to
    // the parameters of the method.
    public CannonBall(int id, PhysicalObject spawn, PhysicalObject target, Vector3 acceleration, double accelerateInSeconds) {
        super(id,"Cannon Ball", Color.RED, 1, 300,
                spawn.getPosition().add(
                        new Vector3(spawn.getRadius(), spawn.getRadius(), spawn.getRadius()).multiply(Vector3.randomNormalised()).multiply(Vector3.randomSigns())
                ),
                spawn.getVelocity());
        this.setVelocity(spawn.getVelocity());
        this.acceleration = acceleration;
        this.accelerateInSeconds = accelerateInSeconds;
        this.target = target;
    }

    public CannonBall(int id, PhysicalObject target, Vector3 position, Vector3 velocity, Vector3 acceleration, double accelerateInSeconds) {
        super(id, "Cannon Ball", Color.RED, 1, 300, position, velocity);
        this.setPosition(position);
        this.setVelocity(velocity);
        this.target = target;
        this.acceleration = acceleration;
        this.accelerateInSeconds = accelerateInSeconds;
    }

    public Vector3 getAcceleration() {
        return this.acceleration;
    }

    @Override
    public void updateVelocity(Universe universe, List<PhysicalObject> objectList) {
        super.updateVelocity(universe, objectList); // gravity
        // hit or not
        isHit = false;

        //--- min distance to target / still getting closer?
        final double distance = this.target.getPosition().subtract(this.getPosition()).length();
        if(distance < minDistanceToTarget) {
            minDistanceToTarget = distance;
        } else {
            this.stillGettingCloser = false;
        }
        //---

        if(this.target.getPosition().subtract(this.getPosition()).multiply(1D / universe._TIME_DELTA).length()-target.getRadius() < getVelocity().length()) {
            this.setVelocity(this.target.getVelocity());
            isHit = true;
            System.out.println("hti");
        } else if(accelerateInSeconds > 0) {
            // update velocity v(t+1)=v(t) + dT * a
            Vector3 velocity = this.getVelocity();
            double accelerationTime = Math.min(accelerateInSeconds, universe._TIME_DELTA);
            accelerateInSeconds -= accelerationTime;
            velocity = velocity.add(new Vector3(acceleration.getX() * accelerationTime, acceleration.getY() * accelerationTime, acceleration.getZ() * accelerationTime));
            this.setVelocity(velocity);
        }

    }

    public boolean isStillGettingCloser() {
        return this.stillGettingCloser;
    }

    public boolean hasHit(){
        return isHit;
    }

    public Vector3 getTargetCoordinate() {
        return targetCoordinate;
    }

    public boolean isAccelerating() {
        return accelerateInSeconds > 0;
    }
}
