package edu.um.planet.physics;

import edu.um.planet.Universe;
import edu.um.planet.math.Interval;
import edu.um.planet.math.RungeKutta;
import edu.um.planet.math.Vector3;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is quintessential for the simulation as it basically is representable of every object in the simulation which
 * is physically present in the world, which is every object.
 */
public class PhysicalObject implements Cloneable {

    /**
     * This determines weather the simulation uses 4th order Runge-Kutta, or Newton's method to solve the differential
     * equations modelling the physics.
     */
    public static boolean _USE_RUNGE_KUTTA = true;
    /**
     * Used for some tests or debugging.
     */
    private final static boolean enableGravity = false;

    /**
     * The unique identifier (id), name and color of the object used to represent it in the 2D rendering of the world.
     */
    private int id;
    private String name;
    private Color color;

    private double mass;
    private double radius;
    private Vector3 position;
    private Vector3 velocity;

    /**
     * Creates a new phyiscal object.
     * @param id This is ideally the ID assigned by JPL Horizon, otherwise use negative IDs.
     * @param name The display name of the object.
     * @param color The color it should have in the 2D representation.
     * @param radius The radius of the physical object (assuming a sphere).
     * @param mass The mass of the celestial body.
     * @param position The position physical object relative to the sun.
     * @param velocity The velocity of the physical object relative to the sun.
     */
    public PhysicalObject(int id, String name, Color color, double radius, double mass, Vector3 position, Vector3 velocity) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.mass = mass;
        this.position = position;
        this.velocity = velocity;
        this.radius = radius;
    }

    /**
     * The id of the physical object.
     * @return
     */
    public int getId() {
        return id;
    }

    /**
     * The display name of the object.
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * The colour used in the representation.
     * @return
     */
    public Color getColor() {
        return color;
    }

    /**
     * The radius of the physical object.
     * @return m
     */
    public double getRadius() {
        return radius;
    }

    /**
     * The position of the physical object relative to the sun.
     * @return m
     */
    public Vector3 getPosition() {
        return position;
    }

    /**
     * The mass of the physical object.
     * @return kg
     */
    public double getMass() {
        return mass;
    }

    /**
     * The velocity of the object relative to the sun.
     * @return
     */
    public Vector3 getVelocity() {
        return velocity;
    }

    /**
     * Updates the postion of the physical object.
     * @param pos
     */
    public void setPosition(Vector3 pos) {
        this.position = pos;
    }

    /**
     * Updates the velocity of the physical object.
     * @param velocity
     */
    public void setVelocity(Vector3 velocity) {this.velocity = velocity; }

    /**
     * Update the velocity by taking into account the  gravity of all other objects.
     * @param universe The universe this object belongs to.
     * @param objectList The objects that should be taken into consideration.
     */
    public void updateVelocity(Universe universe, List<PhysicalObject> objectList) {

        // force
        Vector3 sum = new Vector3();
        for (PhysicalObject other : objectList) {
            if (this != other && (!(other instanceof CannonBall))) {
                // F = (GMm)/(r^2)
                final double F = (Universe._G * this.getMass() * other.getMass()) / Math.pow(other.getPosition().subtract(this.getPosition()).length(), 2);

                assert !Double.isNaN(F);

                // force components for x and y
                sum = sum.add(other.getPosition().subtract(this.getPosition()).normalise().multiply(F));
            }
        }


        if (_USE_RUNGE_KUTTA) {
            Vector3[] vectors = RungeKutta.solve(this.velocity, this.position, Interval.of(0, universe._TIME_DELTA), sum.divide(getMass()));
            this.velocity = vectors[1];
            this.position = vectors[0];
        } else {
            Vector3 a = sum.divide(getMass());
            this.setVelocity(this.getVelocity().add(new Vector3(a.getX(), a.getY(), a.getZ()).multiply(universe._TIME_DELTA)));
        }

    }

    /**
     * Updates the position of the object. This gets only called if {@link PhysicalObject#_USE_RUNGE_KUTTA} is set to true.
     * @param universe
     */
    public void updatePosition(Universe universe) {
        Vector3 pos = this.position;
        double x = this.velocity.getX() * universe._TIME_DELTA;
        double y = this.velocity.getY() * universe._TIME_DELTA;
        double z = this.velocity.getZ() * universe._TIME_DELTA;
        this.position = pos.add(new Vector3(x, y, z));
    }

    /**
     * Recovers an object by copying the position and velocity from the provided object.
     * @param b
     */
    public void recover(PhysicalObject b) {
        if(this.getId() != b.getId()) throw new IllegalArgumentException();
        //TODO maybe recover more data from the old object
        this.position = b.getPosition();
        this.velocity = b.getVelocity();
    }

    @Override
    public PhysicalObject clone() {
        // NOTE the vectors are immutable anyway so there is no point in copying them.
        return new PhysicalObject(this.id, this.name, this.color, this.radius, this.mass, this.position, this.velocity);
    }

    @Override
    public String toString() {
        return String.format("[name=%s,mass=%.0f,position=%s,velocity=%s]",
                this.name,
                this.mass,
                this.position,
                this.velocity);
    }

    public static class CircularBuffer<T> {

        private final List<T> data = new LinkedList<>();
        private int index = 0;
        private int size;

        public CircularBuffer(int size) {
            this.size = size;
        }

        public List<T> getData() {
            return this.data;
        }

        public void add(T value) {
            this.data.add(index, value);
            this.index = index + 1 % this.size;
        }

    }

}
