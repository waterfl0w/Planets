package edu.um.landing.lander;

import edu.um.landing.DataLogger;
import edu.um.landing.FuelTracker;
import edu.um.landing.lander.thruster.RotationThruster;
import edu.um.landing.lander.thruster.Thruster;
import edu.um.planet.math.Vector3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LandingModule {

    private DataLogger dataLogger = new DataLogger();
    private FuelTracker fuelTracker = new FuelTracker();

    public static double TIME_STEP = 0.01;

    private boolean isLanded = false;

    private Vector3 velocity;
    private Vector3 position;

    private Vector3 realPositions;
    private Vector3 realVelocity;

    private double targetTheta = 0;
    private double theta; // rotation
    private double thetaVelocity;

    private double mass = 10000;
    private double height=2;

    private final double gravityAcceleration;

    public Thruster downThruster = new Thruster(Direction.Y_POS, 400*10, mass);
    public Thruster leftThruster = new Thruster(Direction.X_POS, 200*25, mass);
    public Thruster rightThruster = new Thruster(Direction.X_NEG, 200*25, mass);
    public Thruster frontThruster = new Thruster(Direction.Z_POS, 200*25, mass);
    public Thruster backThruster = new Thruster(Direction.Z_NEG, 200*25, mass);

    public RotationThruster leftRotation = new RotationThruster(Direction.X_NEG, 100, mass, Math.sqrt(2), height);
    public RotationThruster rightRotation = new RotationThruster(Direction.X_POS, 100, mass, Math.sqrt(2), height);

    private int turns = 0;

    private ControllerMode controllerMode;

    //---
    private double time = 0;
    private boolean storedData;

    public LandingModule(boolean storeData, double gravityAcceleration, Vector3 position, Vector3 velocity, double theta, double thetaVelocity, ControllerMode controllerMode) {
        this.storedData = !storeData; // kinda hacky...
        this.gravityAcceleration = gravityAcceleration;
        this.realVelocity = velocity;
        this.realPositions = position;
        this.controllerMode = controllerMode;

        this.velocity = velocity.copy();
        this.position = position.copy();
        this.theta = theta;
        this.thetaVelocity = thetaVelocity;
    }

    public FuelTracker getFuelTracker() {
        return this.fuelTracker;
    }

    public boolean isLanded() {
        return isLanded;
    }

    public double getTheta() {
        return theta;
    }

    public double getThetaVelocity() {
        return thetaVelocity;
    }

    public Vector3 getRealPositions() {
        return realPositions;
    }

    public Vector3 getRealVelocity() {
        return realVelocity;
    }

    public double getHeight() {
        return height;
    }

    public void setTheta(double theta) {
        this.theta = Math.atan2(Math.sin(theta), Math.cos(theta));
    }


    public void updateController() {

        // --- update timeInSeconds
        this.time += TIME_STEP;

        final double distanceY = Math.abs(this.getPosition().getY());

        if(distanceY < 1E-3) {
            this.isLanded = true;

            double totalError = ((Math.abs(this.realPositions.getX()) + Math.abs(this.realPositions.getZ()) + Math.abs(this.realVelocity.getX()) + Math.abs(this.realVelocity.getZ()) + Math.abs(this.theta) + Math.abs(this.thetaVelocity)) / 0.7);

            if(!storedData) {
                List<String> keys = new ArrayList<>(dataLogger.getData().get(dataLogger.getData().keySet().stream().findAny().get()).keySet());
                StringBuilder header = new StringBuilder();
                header.append("timeInSeconds,");
                for(String key : keys) {
                    header.append(String.format("%s,", key));
                }
                String fileHeader = header.substring(0, header.length() - 1);

                StringBuilder buffer = new StringBuilder();
                buffer.append(String.format("%s\n", fileHeader));

                //----
                for (Map.Entry<Double, Map<String, Double>> entry : dataLogger.getData().entrySet()) {
                    buffer.append(String.format("%.8f,", entry.getKey()));
                    for (int i = 0; i < keys.size(); i++) {
                        String key = keys.get(i);
                        buffer.append(String.format("%.8f", entry.getValue().get(key)));
                        if(i < keys.size() - 1) {
                            buffer.append(",");
                        }
                    }
                    buffer.append("\n");
                }
                try {
                    Files.write(Paths.get("test.csv"), buffer.toString().getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                this.storedData = true;
            }

            System.out.println(String.format("pe=%s, ve=%s, t=%.4f, t'=%.4f\ne=%.4f", realPositions.toString(), realVelocity.toString(),
                    theta, thetaVelocity, totalError));
            System.out.println("<->");
            return;
        }

        final double signedYForce = this.getVelocity().getY() * mass;
        final double timeToY = (distanceY / Math.abs(this.getVelocity().getY()));

        final double yToZero = Math.abs(this.getVelocity().getY() / (this.downThruster.getForce())) ;
        final double timeToX = this.getVelocity().getX() == 0 ? 0 :  Math.abs(this.getPosition().getX()) / Math.abs(this.getVelocity().getX());
        final double timeToZ = this.getVelocity().getZ() == 0 ? 0 :  Math.abs(this.getPosition().getZ()) / Math.abs(this.getVelocity().getZ());


        // --- Rotation
        final double thetaToZero = Math.abs((theta-targetTheta) / thetaVelocity);
        final double halfToZero = Math.abs(Math.PI / leftRotation.getForce());
        if(Math.abs((theta-targetTheta)) > 1E-4) {
            final double thetaBreakingTime = Math.abs(thetaVelocity / leftRotation.getForce()); // assumes both thrusters are equally strong
            final double thetaDistance = Math.abs((theta-targetTheta));

            //--- Turning right
            final double rotationVelocityError = Math.toRadians(1E-4);
            final double rotationError = Math.toRadians(1E-4);
            if(thetaVelocity > rotationVelocityError) {

                // correct direction
                if((theta-targetTheta) < -rotationError) {
                    //Do we have to break?
                    if(thetaBreakingTime >= thetaToZero) {
                        leftRotation.burn(Math.abs(leftRotation.getForce() / ((thetaDistance / thetaBreakingTime) / thetaBreakingTime) - thetaVelocity));
                    }
                    //Do we have to accelerate?
                }
                // wrong direction
                else if((theta-targetTheta) > rotationError) {
                    //breaking
                    leftRotation.burn(Math.abs(thetaVelocity / leftRotation.getForce()));
                }

            } else if(thetaVelocity < -rotationVelocityError) {

                // correct direction
                if ((theta - targetTheta) > rotationError) {
                    //Do we have to break?
                    if (thetaBreakingTime >= thetaToZero) {
                        rightRotation.burn(Math.abs(rightRotation.getForce() / ((thetaDistance / thetaBreakingTime) / thetaBreakingTime) - thetaVelocity));
                    } else if(thetaToZero + timeToX + timeToZ > timeToY) {
                        leftRotation.burn(rightRotation.getForce() / ((thetaDistance / thetaBreakingTime) / thetaBreakingTime));
                    }
                    //Do we have to accelerate?
                }
                // wrong direction
                else if ((theta - targetTheta) < -rotationError) {
                    //breaking
                    rightRotation.burn(Math.abs(thetaVelocity / rightRotation.getForce()));
                }

            }  else {

                if(Math.abs(thetaVelocity) < rotationVelocityError) {
                    if((theta-targetTheta) > rotationError) {
                        leftRotation.burn(Math.abs((theta-targetTheta) / leftRotation.getForce()));
                    } else if((theta-targetTheta) < -rotationError) {
                        rightRotation.burn(Math.abs((theta-targetTheta) / rightRotation.getForce()));
                    }
                }

            }
        }

        if((yToZero + halfToZero + timeToX + timeToZ) * 2 >= timeToY && timeToX > 1E-3 && timeToZ > 1E-3 && timeToY > 1E-3) {
            this.targetTheta = 0;
        }

        // TODO 5 degrees seems alright, we should do some calculations to figure out what is acceptable and not just some
        //  magic number
        if(Math.abs((theta-targetTheta)) > Math.toRadians(10)) {
            return;
        }

        // --- Y-burn
        if((signedYForce < 0 && distanceY > 0.5)) {
            if(Math.abs(theta) <= Math.toRadians(10)) {
                if(timeToY * 0.98 <= yToZero) {
                    downThruster.burn(timeToY);
                } else if(timeToX + timeToZ > timeToY && yToZero > 1E-3
                        && Math.abs(position.getX()) > 1E1 &&  Math.abs(getPosition().getY()) > 1E1
                        && Math.abs(position.getZ()) > 1E1) {
                    downThruster.burn(yToZero);
                }
            } else if(timeToY <= yToZero) {
                targetTheta = 0;
            }
        }
        if(((yToZero + halfToZero + timeToX + timeToZ) * 2 < timeToY)
                && (this.getPosition().mul(new Vector3(1, 0, 1)).length() < 1)
                && this.getVelocity().mul(new Vector3(1, 0, 1)).length() < 0.5

        ) {
            if(Math.abs(targetTheta - Math.PI) <= Math.toRadians(1)) {
                this.downThruster.burn(TIME_STEP);
            } else if(turns > 0 && this.getVelocity().mul(new Vector3(1, 0, 1)).length() < 1) {
                turns--;
                //System.out.println("ROTATE");
                this.targetTheta = Math.toRadians(179.5);
            }
        }

        //Horizontal Translation
        // Note: Depending on the orientation of the module, we have to fire opposite thrusters.
        if(Math.abs(theta) <= Math.toRadians(5)) {
            controlHorizontalAxis(Vector3.Component.X, leftThruster, rightThruster, timeToY);
            controlHorizontalAxis(Vector3.Component.Z, frontThruster, backThruster, timeToY);
        } else if(Math.abs(theta-Math.PI) <= Math.toRadians(5)) {
            controlHorizontalAxis(Vector3.Component.X, rightThruster, leftThruster, timeToY);
            controlHorizontalAxis(Vector3.Component.Z, backThruster, frontThruster, timeToY);
        }
    }

    private void controlHorizontalAxis(Vector3.Component axis, Thruster positiveThruster, Thruster negativeThruster, double yBreakingTime) {

        double distanceToAxis = Math.abs(getPosition().get(axis));

        if(distanceToAxis <= 0) {
            return;
        }

        final double aV = this.getVelocity().get(axis);
        final double aF = aV * mass;

        final double velocityLimit = 1E-3;
        final double timeToA = (distanceToAxis / Math.abs(aV));

        if (aV > velocityLimit) { //moving right
            // Do we overstep in the next update?
            if (this.getPosition().add(this.velocity.mul(TIME_STEP)).get(axis) > 0) {
                negativeThruster.burn(Math.abs(aF / (negativeThruster.getForce())));
            } else if (this.getVelocity().get(axis) / (negativeThruster.getForce()) <= TIME_STEP) {
                final double maxThrust = (negativeThruster.getForce());
                final double thrustDelta = maxThrust - Math.abs(aF);
                positiveThruster.burn(Math.abs((thrustDelta) / positiveThruster.getForce()));
            }
            // Are we too slow to get to x=0 in timeInSeconds? -> Accelerate
            else if(timeToA <= (Math.abs(this.getVelocity().get(axis))/ negativeThruster.getForce())) {
                negativeThruster.burn( negativeThruster.getForce() / Math.abs(((distanceToAxis / timeToA) / timeToA) - Math.abs(this.getVelocity().get(axis))));
            }
            // Are we going too fast? ->  Decelerate
            else if(timeToA >= yBreakingTime) {
                positiveThruster.burn(positiveThruster.getForce() / Math.abs(((distanceToAxis / yBreakingTime) / yBreakingTime) - Math.abs(this.getVelocity().get(axis))));
            }
        } else if (aV < -velocityLimit) { //moving left
            if (this.getPosition().add(this.velocity.mul(TIME_STEP)).get(axis) < 0) {
                positiveThruster.burn(Math.abs(aF / (positiveThruster.getForce())));
            } else if (Math.abs(this.getVelocity().get(axis)) / (positiveThruster.getForce()) <=  TIME_STEP) {
                final double maxThrust = (positiveThruster.getForce());
                final double thrustDelta = maxThrust - Math.abs(aF);
                negativeThruster.burn(Math.abs((thrustDelta) / negativeThruster.getForce()));
            } else if(timeToA <= (Math.abs(this.getVelocity().get(axis))/ positiveThruster.getForce())) {
                positiveThruster.burn( positiveThruster.getForce() / Math.abs(((distanceToAxis / timeToA) / timeToA) - Math.abs(this.getVelocity().get(axis))));
            } else if(timeToA >= yBreakingTime) {
                negativeThruster.burn(negativeThruster.getForce() / Math.abs(((distanceToAxis / yBreakingTime) / yBreakingTime) - Math.abs(this.getVelocity().get(axis))));
            }
        } else if(distanceToAxis > 0.1) {
            if (this.getPosition().get(axis) < 0) {
                positiveThruster.burn(positiveThruster.getForce() / Math.abs(((distanceToAxis / timeToA) / timeToA) - Math.abs(this.getVelocity().get(axis))));
            } else if (this.getPosition().get(axis) > 0) {
                negativeThruster.burn(negativeThruster.getForce() / Math.abs(((distanceToAxis / yBreakingTime) / yBreakingTime) - Math.abs(this.getVelocity().get(axis))));
            }
        }
    }

    public void updatePosition() {

        if(this.isLanded) return;

        this.position = this.position.add(this.velocity.mul(TIME_STEP));
        if(this.position.getY() < 0) {
            this.position = new Vector3(this.position.getX(), 0, this.position.getZ());
        }

        this.realPositions = this.realPositions.add(this.realVelocity.mul(TIME_STEP));
        if(this.realPositions.getY() < 0) {
            this.realPositions = new Vector3(this.realPositions.getX(), 0, this.position.getZ());
        }

        this.setTheta(this.theta + this.thetaVelocity * TIME_STEP);

    }

    public void updateVelocity() {

        if(this.isLanded) {
            this.velocity = new Vector3();
            this.realVelocity = new Vector3();
            this.thetaVelocity = 0;
            return;
        }

        //--- Thrusters
        if(!this.storedData) {
            dataLogger.add(this.time, "realPositionX", realPositions.getX());
            dataLogger.add(this.time, "realPositionY", realPositions.getY());
            dataLogger.add(this.time, "realPositionZ", realPositions.getZ());
            dataLogger.add(this.time, "positionX", position.getX());
            dataLogger.add(this.time, "positionY", position.getY());
            dataLogger.add(this.time, "positionZ", position.getZ());
        }

        double gForce = 0;

        Vector3 thrustTotal = new Vector3(0, 0, 0);
        {
            Vector3 thrust = applyRotation(downThruster.getThrust());
            if(!this.storedData) {
                dataLogger.add(this.time, "down", thrust.length());
            }
            thrustTotal = thrustTotal.add(thrust);
        }
        {
            Vector3 thrust = applyRotation(leftThruster.getThrust());
            if(!this.storedData) {
                dataLogger.add(this.time, "left", -thrust.length());
            }
            thrustTotal = thrustTotal.add(thrust);
        }
        {
            Vector3 thrust = applyRotation(rightThruster.getThrust());
            if(!this.storedData) {
                dataLogger.add(this.time, "right", thrust.length());
            }
            thrustTotal = thrustTotal.add(thrust);
        }
        {
            Vector3 thrust = applyRotation(backThruster.getThrust());
            if(!this.storedData) {
                dataLogger.add(this.time, "back", -thrust.length());
            }
            thrustTotal = thrustTotal.add(thrust);
        }
        {
            Vector3 thrust = applyRotation(frontThruster.getThrust());
            if(!this.storedData) {
                dataLogger.add(this.time, "front", thrust.length());
            }
            thrustTotal = thrustTotal.add(thrust);
        }

        this.fuelTracker.add(mass, thrustTotal.length());
        this.realVelocity = this.realVelocity.add(thrustTotal);
        this.velocity = this.velocity.add(thrustTotal);

        gForce += thrustTotal.length();

        //--- Rotation
        {
            double totalRotation = 0;
            totalRotation += leftRotation.getThrust();
            totalRotation += rightRotation.getThrust();
            this.thetaVelocity += totalRotation;
            gForce += Math.PI * 2 * height * totalRotation;

            this.fuelTracker.add(mass, totalRotation);
            dataLogger.add(this.time,"theta", theta);
            dataLogger.add(this.time, "thetaVelocity", thetaVelocity);
        }

        //--- Wind
        Vector3 v = wind(getPosition(), mass).mul(TIME_STEP);
        dataLogger.add(this.time, "windStrength", v.getX());
        if(!Double.isNaN(v.getX())) {
            gForce += v.length();
            if(this.controllerMode == ControllerMode.CLOSED) {
                this.velocity = this.velocity.add(v);
            }
            this.realVelocity = this.realVelocity.add(v);
        }

        //--- Drag
        if(this.controllerMode == ControllerMode.CLOSED) {
            Vector3 drag = drag(position, velocity, mass).mul(TIME_STEP);
            this.velocity = this.velocity.subtract(drag);
            this.dataLogger.add(this.time, "drag", drag.length());
        }
        Vector3 drag = drag(realPositions, realVelocity, mass).mul(TIME_STEP);
        this.realVelocity = this.realVelocity.subtract(drag);
        this.dataLogger.add(this.time, "realDrag", drag.length());


        //--- Gravity
        //v+1 = v + (Gv*m)/deltaY
        Vector3 gravity = new Vector3(0, gravityAcceleration, 0).mul(mass).div(Math.pow(1287850D-this.getPosition().getY(), 2)).mul(-1);
        this.realVelocity = this.realVelocity.add(gravity);
        this.velocity = this.velocity.add(gravity);

        gForce += gravity.length();
        //System.out.println("gforce: " + (gForce / 9.8));

        leftRotation.update();
        rightRotation.update();
        downThruster.update();
        leftThruster.update();
        rightThruster.update();
        frontThruster.update();
        backThruster.update();
    }

    private Vector3 applyRotation(Vector3 force) {
        return new Vector3(force.getX() * Math.cos(theta) - force.getY() * Math.sin(theta),
                force.getX() * Math.sin(theta) + force.getY() * Math.cos(theta), force.getZ() * Math.cos(theta) - force.getY() * Math.sin(theta));
    }

    public Vector3 getPosition() {
        return this.position;
    }

    public Vector3 getVelocity() {
        return this.velocity;
    }

    @Override
    public String toString() {
        return String.format("Module\n\tr:[p=%s,v=%s,θ=%.2f,θ'=%.2f]\n\ta:[p=%s,v=%s,θ=%.2f,θ'=%.2f]",
                this.realPositions, this.realVelocity, Math.toDegrees(this.theta), Math.toDegrees(this.thetaVelocity),
                this.position, this.velocity, Math.toDegrees(this.theta), Math.toDegrees(this.thetaVelocity));
    }

    public static double pressure(Vector3 position) {

        if(position.getY() < 0 || position.getY() > 150000) {
            return 0;
        }

        return Math.pow(-1.579E-25*position.getY(), 5) + Math.pow(7.816E-20*position.getY(), 4)
                + Math.pow(-1.528E-14*position.getY(), 3) + Math.pow(1.484E-09*2, 2)
                + -7.262E-05*position.getY() + 1.468;
    }

    public Vector3 drag(Vector3 pos, Vector3 vel, double mass) {

        final double pressure = pressure(pos);

        if(pressure == 0) {
            return new Vector3();
        }

        Vector3 drag = new Vector3(
                (0.5 * 12.25 * 1 * vel.getX() * 1.1 * height * height),
                0,
                (0.5 * 12.25 * 1 * vel.getZ() * 1.1 * height * height)
        ).div(mass);

        if(drag.length() < 1E-5) {
            return new Vector3();
        }

        return drag;
    }

    public static Vector3 wind(Vector3 pos, double mass) {

        double temperature = 0;

        double distance = pos.getY();  // Convert y to meters until origin

        if(distance > 150_000) {
            return new Vector3();
        }

        if (distance < 47000) {
            double term1 = (Math.pow(distance, 2)) * Math.pow(10, -8);
            double term2 = 0.0011*distance;
            double term3 = 93.558;
            temperature = term1 - term2 + term3;
        } else if (distance > 47000) {
            double term1 = 4 * (Math.pow(distance, 6)) * Math.pow(10, -27);
            double term2 = 3 * (Math.pow(distance, 5)) * Math.pow(10, -21);
            double term3 = 7 * (Math.pow(distance, 4)) * Math.pow(10, -16);
            double term4 = (Math.pow(distance, 3)) * Math.pow(10, -10);
            double term5 = 7 * (Math.pow(distance, 2)) * Math.pow(10, -6);
            double term6 = 0.2648*distance;
            double term7 = 4025.8;
            temperature = term1 - term2 + term3 - term4 + term5 - term6 + term7;
        }

        final double speed = temperature * 2.714;

        // Adjust for uncertainty of wind readings
        final double adjustedSpeed = speed * (Math.random()*(0.5) + 0.5);

        // Adjust for uncertainty of latitude

        Vector3 dir = new Vector3(-1,0,0);

        // Direction
        if (distance > 6000 || distance < 700) {
            dir = new Vector3(1,0,0);
        }

        return dir.mul(adjustedSpeed).div(mass * 10);

    }

    public DataLogger getDataLogger() {
        return this.dataLogger;
    }

    public double getRotation() {
        return this.theta;
    }
}
