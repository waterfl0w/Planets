package edu.um.planet;

import edu.um.planet.physics.CelestialBody;
import edu.um.planet.physics.PhysicalObject;
import edu.um.planet.math.Vector3;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class Universe {

    public final static Random R = new Random(1);

    public final static double _G = 6.6740831E-11; // gravity constant
    public final static double _AU = 1.49598E11; //AU in (m)
    public long _TIME_DELTA = 1; // in seconds, if this value is too big the simulation will break because of inaccuracies!
    public int _LOOP_ITERATIONS = 60;

    private long timeSinceStart = 0;

    private Instant starttime = Timestamp.valueOf("2019-03-14 00:00:00").toInstant();
    private Instant currentTime = starttime;

    private List<PhysicalObject> bodies = new ArrayList<>();

    //--- SAVE STATE
    private Map<Integer, PhysicalObject> save_bodies = new HashMap<>();
    private Instant save_timestamp;
    //---

    // TODO
    //  1) Make a seperate PhysicalObject class and generalise concepts
    //  1.1) Add dynamic time-steps. This seems to be required to updated super small objects (e.g. rockets and cannon balls) accurately!
    //  2) Add a rocket and a thruster system that can fire in 3D.
    //  2.1) Add acceleration to the rocket, so that we can properly launch (bonus).
    //  3) Add planet rotation
    //  4) Add warning when parameters are off
    //  4.1) Potential overflows!
    //  5.1) Objects crashing into each other.


    public Universe() {
        try {
            Files.readAllLines(Paths.get("data/test.csv")).forEach(line -> {
                String[] parts = line.split(",");
                if(Double.valueOf(parts[2]) == -1) return;
                double radius = Double.valueOf(parts[2]);
                this.bodies.add(new CelestialBody(Integer.valueOf(parts[0]), parts[1], Color.WHITE, radius == -1 ? 1 : radius, Double.valueOf(parts[3]), new Vector3(Double.valueOf(parts[4]),Double.valueOf(parts[5]),Double.valueOf(parts[6])), new Vector3(Double.valueOf(parts[7]),Double.valueOf(parts[8]),Double.valueOf(parts[9]))));
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        //this.bodies.add(new CannonBall(- 100, this.getObject("Earth (399)"), this.getObject("Titan (606)")));
    }

    public Universe(List<PhysicalObject> bodies, Instant time) {
        this.bodies = bodies;
        this.currentTime = time;
        this.starttime = time;
    }

    public void save() {
        // clone all physical objects & save current time
        this.save_bodies.clear();
        this.bodies.forEach(o -> this.save_bodies.put(o.getId(), o.clone()));
        this.save_timestamp = Instant.ofEpochMilli(this.currentTime.toEpochMilli());
    }

    public void recover() {
        // reset bodies & current time
        this.timeSinceStart = 0;
        this.save_bodies.forEach((id, o) -> this.getCelestialBody(id).recover(o));
        this.currentTime = Instant.ofEpochMilli(this.save_timestamp.toEpochMilli());
    }

    public Instant getStartTime() {
        return starttime;
    }

    public Instant getCurrentTime() {
        return currentTime;
    }

    public PhysicalObject getCelestialBody(int id) {
        Optional<PhysicalObject> b = this.bodies.stream().filter(e -> e.getId() == id).findAny();
        if(b.isPresent()) {
            return b.get();
        } else {
            System.out.println("No body with id: " + id);
            return null;
        }
    }


    public PhysicalObject getObject(String name) {
        return this.bodies.stream().filter(e -> e.getName().equals(name)).findAny().get();
    }

    public synchronized List<PhysicalObject> getBodies() {
        return bodies;
    }

    public void update() {
        update(null);
    }

    public void update(Consumer<Universe> updated) {

        for(int i = 0; i < _LOOP_ITERATIONS; i++) {
            //--- velocity
            for(PhysicalObject body : this.bodies) {
                body.updateVelocity(this, this.bodies);
            }

            if(!PhysicalObject._USE_RUNGE_KUTTA) {
                //--- update position
                for (PhysicalObject body : this.bodies) {
                    body.updatePosition(this);
                }
            }

            this.timeSinceStart += _TIME_DELTA;
            this.currentTime = this.currentTime.plusSeconds(_TIME_DELTA);

            if(updated != null) {
                updated.accept(this);
            }
        }


    }

    public boolean has(int id) {
        return this.bodies.stream().anyMatch(e -> e.getId() == id);
    }

    public long getUpdateStep() {
        return _TIME_DELTA * _LOOP_ITERATIONS;
    }

    public long getTimeSinceStart() {
        return this.timeSinceStart;
    }
}