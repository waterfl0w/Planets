package edu.um.planet.physics;

import edu.um.planet.Universe;
import edu.um.planet.math.RungeKutta;
import edu.um.planet.math.Vector3;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

public class PhysicalObject implements Cloneable {

    public static boolean _USE_RUNGE_KUTTA = true;

    private int id;
    private String name;
    private Color color;

    private double mass;
    private double radius;
    private Vector3 position;
    private Vector3 velocity;

    public PhysicalObject(int id, String name, Color color, double radius, double mass, Vector3 position, Vector3 velocity) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.mass = mass;
        this.position = position;
        this.velocity = velocity;
        this.radius = radius;
    }

    public int getId() {
        return id;
    }


    public String getName() {
        return name;
    }

    public Color getColor() {
        return color;
    }

    public double getRadius() {
        return radius;
    }

    public Vector3 getPosition() {
        return position;
    }

    public void setPosition(Vector3 pos) {
        this.position = pos;
    }

    public double getMass() {
        return mass;
    }

    public Vector3 getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector3 velocity) {this.velocity = velocity; }

    public void updateVelocity(Universe universe, List<PhysicalObject> objectList) {

        // force
        Vector3 sum = new Vector3();
        if(!(this instanceof CannonBall)) {
            for (PhysicalObject other : objectList) {
                if (this != other && !(other instanceof CannonBall)) {
                    // F = (GMm)/(r^2)
                    final double F = (Universe._G * this.getMass() * other.getMass()) / Math.pow(other.getPosition().subtract(this.getPosition()).length(), 2);

                    assert !Double.isNaN(F);

                    // force components for x and y
                    sum = sum.add(other.getPosition().subtract(this.getPosition()).normalise().multiply(F));
                }
            }
        }


        if (_USE_RUNGE_KUTTA) {
            Vector3[] vectors = RungeKutta.solve(this.velocity, this.position, RungeKutta.Interval.of(0, universe._TIME_DELTA), sum.divide(getMass()));
            this.velocity = vectors[1];
            this.position = vectors[0];
        } else {
            Vector3 a = sum.divide(getMass());
            this.setVelocity(this.getVelocity().add(new Vector3(a.getX(), a.getY(), a.getZ()).multiply(universe._TIME_DELTA)));
        }

    }

    public void updatePosition(Universe universe) {
        Vector3 pos = this.position;
        double x = this.velocity.getX() * universe._TIME_DELTA;
        double y = this.velocity.getY() * universe._TIME_DELTA;
        double z = this.velocity.getZ() * universe._TIME_DELTA;
        this.position = pos.add(new Vector3(x, y, z));
    }

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
