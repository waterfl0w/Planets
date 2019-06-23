package edu.um.landing.lander.thruster;

import edu.um.landing.lander.Direction;
import edu.um.landing.lander.LandingModule;

public abstract class IThruster<F,T> {

    private Direction direction;
    private double force;
    private double timeToBurn = 0;
    private double mass;

    public IThruster(Direction direction, double force, double mass) {
        this.direction = direction;
        this.force = force;
        this.mass = mass;
    }

    public double getMass() {
        return mass;
    }

    public double getTimeToBurn() {
        return this.timeToBurn;
    }

    public Direction getDirection() {
        return direction;
    }

    public double getRawForce() {
        return force;
    }

    public final void burn(double seconds) {
        this.timeToBurn = Math.min(seconds, LandingModule.TIME_STEP);
    }

    public abstract F getForce();

    public abstract T getThrust();

    public abstract T getNewton();


    public final void update() {
        if(timeToBurn != 0) {
            timeToBurn -= LandingModule.TIME_STEP;
            timeToBurn = Math.max(timeToBurn, 0);
        }
    }

    public final boolean isBurning() {
        return this.timeToBurn > 0;
    }

}
