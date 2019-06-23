package edu.um.planet.pathfinder.genetic;

import edu.um.planet.math.Vector3;

public class Individual {

    private Vector3 acceleration;
    private double fitness;
    private int id;

    public Individual(int id, Vector3 acceleration) {
        this.id = id;
        this.acceleration = acceleration;
        this.fitness = 0;
    }

    public int getId() {
        return id;
    }

    public Vector3 getChromosome() {
        return acceleration;
    }

    public double[] genoToPhenotype() {
        return new double[] {acceleration.getX(), acceleration.getY(), acceleration.getZ()};
    }

    public void setChromosome(Vector3 acceleration) {
        this.acceleration = acceleration;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public Individual clone() {
        return new Individual(id, this.acceleration.clone());
    }

}


