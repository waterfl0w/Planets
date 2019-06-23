package edu.um.planet.physics;

import edu.um.planet.math.Vector3;

import java.awt.*;

public class CelestialBody extends PhysicalObject {

    private int parentId;

    public CelestialBody(int id, String name, Color color, double radius, double mass, Vector3 position, Vector3 velocity) {
        super(id, name, color, radius, mass, position, velocity);

        //TODO a bit hacky
        if(!(id < 1000 && id > 99)) {
            this.parentId = 10;
        } else {
            //abc
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
