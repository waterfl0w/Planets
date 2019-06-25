package edu.um.landing.lander.thruster;

import edu.um.landing.lander.Direction;
import edu.um.landing.lander.LandingModule;
import edu.um.planet.math.Vector3;

/**
 * This thruster is position on the axis of rotation thus it cannot apply any torque but it is able to easily
 * apply translation without introducing any unwanted movements like rotation.
 */
public class Thruster extends IThruster<Double, Vector3> {

    /**
     *
     * @param direction The direction the thruster is facing.
     * @param force The force of the thruster in Newton.
     * @param mass The mass of the body the thruster is attached to.
     */
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
            v = getDirection().direction().multiply(getRawForce()).multiply(Math.min(LandingModule.TIME_STEP, getTimeToBurn())).divide(getMass());
        }
        return v;
    }

    @Override
    public Vector3 getNewton() {
        Vector3 v = new Vector3(0, 0, 0);
        if(isBurning()) {
            v = getDirection().direction().multiply(getRawForce()).multiply(Math.min(LandingModule.TIME_STEP, getTimeToBurn()));
        }
        return v;
    }

}
