package edu.um.planet.math;

import edu.um.planet.Universe;

public class Vector3 implements Cloneable {

    private double x;
    private double y;
    private double z;
    private double length;

    public Vector3() {
        this(0, 0, 0);
    }

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        updateLength();
    }

    public double get(Component axis) {
        switch (axis) {

            case X: return this.getX();
            case Y: return this.getY();
            case Z: return this.getZ();

        }

        throw new IllegalArgumentException();
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double length() {
        return this.length;
    }

    public Vector3 mul(double factor) {
        return multiply(factor);
    }

    public Vector3 mul(Vector3 b) {
        return multiply(b);
    }

    public Vector3 multiply(double factor) {
        return new Vector3(this.x  * factor, this.y * factor, this.z * factor);
    }

    public Vector3 multiply(Vector3 b) {
        return new Vector3(this.x  * b.x, this.y * b.y, this.z * b.z);
    }

    public Vector3 normalise() {
        return new Vector3(this.x / this.length, this.y / this.length, this.z / this.length);
    }

    public Vector3 add(Vector3 b, boolean createCopy) {
        if(createCopy) {
            return new Vector3(this.x + b.x, this.y + b.y, this.z + b.z);
        } else {
            this.x += b.getX();
            this.y += b.getY();
            this.z += b.getZ();
            updateLength();
            return this;
        }
    }

    public Vector3 add(Vector3 b) {
        return add(b, true);
    }

    public Vector3 add(double b) {
        return new Vector3(this.x + b, this.y + b, this.z + b);
    }

    public Vector3 subtract(Vector3 b) {
        return new Vector3(this.x - b.x, this.y - b.y, this.z - b.z);
    }

    public Vector3 div(Vector3 b) {
        return new Vector3(this.x / b.x, this.y / b.x, this.z / b.z);
    }

    public Vector3 div(double factor) {
        return new Vector3(this.x / factor, this.y / factor, this.z / factor);
    }

    public Vector3 divide(Vector3 b) {
        return new Vector3(this.x / b.x, this.y / b.x, this.z / b.z);
    }

    public Vector3 divide(double factor) {
        return new Vector3(this.x / factor, this.y / factor, this.z / factor);
    }

    public Vector3 dir(Vector3 b) {
        return b.subtract(this).normalise();
    }

    @Override
    public Vector3 clone() {
        return new Vector3(this.x, this.y, this.z);
    }

    public Vector3 copy() {
        return clone();
    }

    private final void updateLength() {
        this.length = Math.sqrt((this.x * this.x) + (this.y * this.y) + (this.z * this.z));
    }

    @Override
    public String toString() {
        return String.format("[%.4f,%.4f,%.4f]", this.x, this.y, this.z);
    }

    public static Vector3 randomNormalised() {
        return new Vector3(Universe.R.nextDouble(), Universe.R.nextDouble(), Universe.R.nextDouble()).normalise();
    }

    public static Vector3 randomSigns() {
        return new Vector3(Universe.R.nextBoolean() ? 1 : -1, Universe.R.nextBoolean() ? 1 : -1, Universe.R.nextBoolean() ? 1 : -1);
    }

    public enum Component {
        X, Y, Z
    }

}
