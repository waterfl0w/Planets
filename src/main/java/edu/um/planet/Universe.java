package edu.um.planet;

import edu.um.planet.physics.CannonBall;
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
import java.util.stream.Collectors;

public class Universe {

    public final static Random R = new Random(1);

    public final static double _G = 6.6740831E-11; // gravity constant
    public final static double _AU = 1.49598E11; //AU in (m)
    public long _TIME_DELTA = 1; // in seconds, if this value is too big the simulation will break because of inaccuracies!
    public int _LOOP_ITERATIONS = 60;

    private long timeSinceStart = 0;

    private Instant starttime = Timestamp.valueOf("2019-03-14 00:00:00").toInstant();
    private Instant currentTime = starttime;

    private Map<Integer, PhysicalObject> bodies = new HashMap<>();

    //--- SAVE STATE
    private Map<Integer, PhysicalObject> save_bodies = new HashMap<>();
    private Instant save_timestamp;
    //---

    // TODO
    //  1) Make a seperate PhysicalObject class and generalise concepts
    //  1.1) Add dynamic timeInSeconds-steps. This seems to be required to updated super small objects (e.g. rockets and cannon balls) accurately!
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
                this.bodies.put(Integer.valueOf(parts[0]), new CelestialBody(Integer.valueOf(parts[0]), parts[1], Color.WHITE, radius == -1 ? 1 : radius, Double.valueOf(parts[3]), new Vector3(Double.valueOf(parts[4]),Double.valueOf(parts[5]),Double.valueOf(parts[6])), new Vector3(Double.valueOf(parts[7]),Double.valueOf(parts[8]),Double.valueOf(parts[9]))));
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        //this.bodies.add(new CannonBall(- 100, this.getObject("Earth (399)"), this.getObject("Titan (606)")));
    }

    public Universe(List<PhysicalObject> bodies, Instant time) {
        bodies.forEach(e -> {
            this.bodies.put(e.getId(), e);
        });
        this.currentTime = time;
        this.starttime = time;
    }

    /**
     * Saves the current velocity and position of all physical objects in the universe.
     */
    public void save() {
        // clone all physical objects & save current timeInSeconds
        this.save_bodies.clear();
        this.bodies.forEach((id, o) -> this.save_bodies.put(o.getId(), o.clone()));
        this.save_timestamp = Instant.ofEpochMilli(this.currentTime.toEpochMilli());
    }

    /**
     * Recovers the universe from the saved state after calling {@link Universe#save()}.
     */
    public void recover() {
        // reset bodies & current timeInSeconds
        this.timeSinceStart = 0;
        Iterator<PhysicalObject> iterator = this.bodies.values().iterator();
        while (iterator.hasNext()) {
            PhysicalObject physicalObject = iterator.next();
            if(this.save_bodies.containsKey(physicalObject.getId())) {
                this.getCelestialBody(physicalObject.getId()).recover(this.save_bodies.get(physicalObject.getId()));
            } else {
                iterator.remove();
            }
        }
        this.currentTime = Instant.ofEpochMilli(this.save_timestamp.toEpochMilli());
    }

    /**
     * Returns the start time of the universe.
     * @return
     */
    public Instant getStartTime() {
        return starttime;
    }

    /**
     * The current time in the ujniverse.
     * @return
     */
    public Instant getCurrentTime() {
        return currentTime;
    }

    /**
     * Returns the universe with a certain id.
     * @param id The id of the physical object.
     * @return
     */
    public PhysicalObject getCelestialBody(int id) {
        if(has(id)) {
            return this.bodies.get(id);
        } else {
            System.out.println("No body with id: " + id);
            return null;
        }
    }

    /**
     * Checks if an object with the given id exists.
     * @param id
     * @return
     */
    public boolean has(int id) {
        return this.bodies.containsKey(id);
    }

    /**
     * The amount of time every update passes in seconds.
     * @return
     */
    public long getUpdateStep() {
        return _TIME_DELTA * _LOOP_ITERATIONS;
    }

    /**
     * The amount of time that has passed since the beginning of the simulation.
     * @return
     */
    public long getTimeSinceStart() {
        return this.timeSinceStart;
    }

    /**
     * Returns a physical object based in the display name of an object.
     * @param name The display name of the object.
     * @return
     */
    public PhysicalObject getObject(String name) {
        return this.bodies.values().stream().filter(e -> e.getName().equals(name)).findAny().get();
    }

    /**
     * All physical objects in the universe.
     * @return
     */
    public synchronized Collection<PhysicalObject> getBodies() {
        return bodies.values();
    }

    /**
     * Calls {@link Universe#update()} without a callback.
     */
    public void update() {
        update(null);
    }

    /**
     * Updates the position and velocity of all objects. It uses {@link Universe#_TIME_DELTA} as the timestep and does
     * it {@link Universe#_LOOP_ITERATIONS} amount of times.
     * @param updated Is called every time a time step in the simulation has been completed.
     */
    public void update(Consumer<Universe> updated) {

        for(int i = 0; i < _LOOP_ITERATIONS; i++) {
            //--- velocity
            for(PhysicalObject body : this.bodies.values()) {
                body.updateVelocity(this, this.bodies.values().stream().filter(e ->  !(e instanceof CannonBall)).collect(Collectors.toList()));
            }

            if(!PhysicalObject._USE_RUNGE_KUTTA) {
                //--- update position
                for (PhysicalObject body : this.bodies.values()) {
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

}