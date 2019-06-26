package edu.um.planet.physics;

import edu.um.planet.math.Vector3;

import java.awt.*;

/**
 * A celestial body in the universe.
 */
public class CelestialBody extends PhysicalObject {

    private int parentId;

    public CelestialBody(int id, String name, Color color, double radius, double mass, Vector3 position, Vector3 velocity) {
        super(id, name, color, radius, mass, position, velocity);

        //TODO a bit hacky
        if(!(id < 1000 && id > 99)) {
            this.parentId = 10;
        } else {
            // Based on the first digit of the id, we can decide whether  or not another celestial body is either its
            // parent, child, or neither. - This is all based on the JPL Horizons ID System, e.g. Earth has the id 399
            // and Earth's Moon has the id 301, or Saturn is 699 and its moon Titan has the id 606, and this is consistent
            // throughout all natural celestial bodies.
            int c = id / 1 % 10;
            int b = id / 10 % 10;
            int a = id / 100 % 10;

            if (b != 9 && a != 9) {
                this.parentId = (int) ((c + 0.99D) * 1E2);
            } else {
                this.parentId = 10;
            }
        }
    }

    public int getParentId() {
        return this.parentId;
    }

}
