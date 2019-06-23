package edu.um.planet.physics;

import edu.um.planet.Universe;
import edu.um.planet.math.Vector3;

import java.awt.*;
import java.util.List;

public class CannonBallLowMass extends PhysicalObject {

    private Vector3 acceleration = new Vector3(22714.7683,1695.9483,-162.4729);
    //16755.4035,18500.7133,-261.8501
    //1790261.9160,-5262363.6349,39665.3714
    //22714.7683,1695.9483,-162.4729
    private double accelerateInSeconds = 1;

    public CannonBallLowMass(PhysicalObject spawn) {
        super(-11,"Cannon Ball", Color.RED, 1, 1, spawn.getPosition().add(new Vector3(0, 0, spawn.getRadius())), spawn.getVelocity());
    }

    @Override
    public void updateVelocity(Universe universe, List<PhysicalObject> objectList) {
        //super.updateVelocity(universe, objectList); // gravity

        if(accelerateInSeconds > 0) {
            // update velocity v(t+1)=v(t) + dT * a
            Vector3 velocity = this.getVelocity();
            double accelerationTime = Math.min(accelerateInSeconds, universe._TIME_DELTA);
            accelerateInSeconds -= accelerationTime;
            velocity = velocity.add(new Vector3(acceleration.getX() * accelerationTime, acceleration.getY() * accelerationTime, acceleration.getZ() * accelerationTime));
            this.setVelocity(velocity);
        }

    }
}
