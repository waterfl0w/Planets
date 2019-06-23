package edu.um.landing.lander.thruster;

import edu.um.landing.lander.Direction;
import edu.um.landing.lander.LandingModule;
import edu.um.planet.math.Vector3;

public class Thruster extends IThruster<Double, Vector3> {

    public Thruster(Direction direction, double force, double mass) {
        super(direction, force, mass);
    }

    @Override
    public Double getForce() {
        return getRawForce() / getMass();
    }

    @Override
    public Vector3 getThrust() {
        Vector3 v = new Vector3(0, 0, 0);
        if(isBurning()) {
            v = getDirection().direction().mul(getRawForce()).mul(Math.min(LandingModule.TIME_STEP, getTimeToBurn())).div(getMass());
        }
        return v;
    }

    @Override
    public Vector3 getNewton() {
        Vector3 v = new Vector3(0, 0, 0);
        if(isBurning()) {
            v = getDirection().direction().mul(getRawForce()).mul(Math.min(LandingModule.TIME_STEP, getTimeToBurn()));
        }
        return v;
    }

}
