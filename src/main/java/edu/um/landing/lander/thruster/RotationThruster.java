package edu.um.landing.lander.thruster;

import edu.um.landing.lander.Direction;
import edu.um.planet.math.Vector3;
import edu.um.landing.lander.LandingModule;

/**
 * This thruster is position so that it is offset from the rotational axis, thus it is able to apply torque to the body
 * it has been attached to.
 */
public class RotationThruster extends IThruster<Double, Double> {

    private final double r;
    private final double h;

    /**
     *
     * @param direction Direction of the rotation.
     * @param force Force in Newton of the thruster.
     * @param mass Mass of the object to rotate.
     * @param r Distance from the rotation axis to the thruster.
     * @param h Height and width of the rectangle.
     */
    public RotationThruster(Direction direction, double force, double mass, double r, double h) {
        super(direction, force, mass);
        this.r = r;
        this.h = h;
    }

    @Override
    public Double getForce() {
        return getDirection().direction().mul(getRawForce()).div(getMass()).div(r).mul(Math.pow(h, 4) / 12D).getX();
    }

    @Override
    public Double getThrust() {
        Vector3 t = new Vector3();
        if(isBurning()) {
            t = getDirection().direction().mul(getRawForce()).div(getMass()).div(r).mul(Math.pow(h, 4) / 12D).mul(Math.min(LandingModule.TIME_STEP, getTimeToBurn()));
        }
        return t.getX();
    }

    @Override
    public Double getNewton() {
        Vector3 t = new Vector3();
        if(isBurning()) {
            t = getDirection().direction().mul(getRawForce()).div(r).mul(Math.pow(h, 4) / 12D).mul(Math.min(LandingModule.TIME_STEP, getTimeToBurn()));
        }
        return t.getX();
    }
}
