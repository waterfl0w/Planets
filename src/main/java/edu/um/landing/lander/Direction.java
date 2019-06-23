package edu.um.landing.lander;

import edu.um.planet.math.Vector3;

public enum Direction {

    X_NEG(new Vector3(-1, 0, 0)),
    X_POS(new Vector3(1, 0, 0)),
    Z_NEG(new Vector3(0, 0, -1)),
    Z_POS(new Vector3(0, 0, 1)),
    Y_POS(new Vector3(0, 1, 0));

    private Vector3 dir;
    Direction(Vector3 dir) {
        this.dir = dir;
    }

    public Vector3 direction() {
        return dir;
    }

}

