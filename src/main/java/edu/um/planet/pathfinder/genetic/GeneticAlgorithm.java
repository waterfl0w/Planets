package edu.um.planet.pathfinder.genetic;

import edu.um.planet.Universe;
import edu.um.planet.physics.CannonBall;
import edu.um.planet.math.Vector3;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;


public class GeneticAlgorithm {

    private final static double seconds = (929292307.2 / 1000);
    private final static double TIME_STEP = 60;
;
    private final static Random random = new Random();

    /**
     * @param args
     */
    public static void main(String[] args) {

        //--- Settings
        List<Individual> population = generatePopulation();
        final int POP_SIZE = population.size();

        final int GENERATIONS = 50000;

        //--- Elitist
        final Selection SELECTION_TYPE = Selection.ELITIST;
        final double ELITIST_SELECTION_RATE = 0.1D;

        //--- Mutation
        final double MUTATION_RATE = 1;

        //-- Crossover
        final CrossType CROSS_TYPE = CrossType.HALF_AND_HALF;

        //---
        System.out.printf("Population:%n\t- Size: %d%n", POP_SIZE);
        System.out.printf("Generations:%n\t- Max: %d%n", GENERATIONS);
        System.out.printf("Mutation:%n\t- Rate: %.2f%%%n", MUTATION_RATE * 100);
        System.out.printf("Crossover:%n\t- Status: %s%n\t- Type: %s%n", (CROSS_TYPE != CrossType.NONE ? "On" : "Off"), CROSS_TYPE.name());
        System.out.printf("Selection:%n\t- %s%n", SELECTION_TYPE.name());
        switch (SELECTION_TYPE) {
            case ELITIST:
                System.out.printf("\t\t- Selection Rate: %.2f%%%n", ELITIST_SELECTION_RATE * 100);
                break;
        }


        int requiredGenerations = 0;
        for(int iteration = 0; iteration < GENERATIONS; iteration++) {


            switch (SELECTION_TYPE) {
                case ELITIST:
                    population.sort((o1, o2) -> Double.compare(o2.getFitness(), o1.getFitness()));
                    population = population.subList(0, (int) (POP_SIZE * ELITIST_SELECTION_RATE));
                    break;
            }


            List<Individual> gen = new LinkedList<>();

            while(gen.size() < POP_SIZE) {

                // crossover
                Individual a = population.get((int) (random.nextDouble() * population.size()));
                Individual b = population.get((int) (random.nextDouble() * population.size()));

                Individual child = null;
                double[] chromosome = new double[3];
                switch (CROSS_TYPE) {

                    case HALF_AND_HALF:
                        if(random.nextBoolean()) {
                            chromosome[0] = a.genoToPhenotype()[0];
                            chromosome[1] = b.genoToPhenotype()[1];
                            chromosome[2] = a.genoToPhenotype()[2];
                        } else {
                            chromosome[0] = b.genoToPhenotype()[0];
                            chromosome[1] = a.genoToPhenotype()[1];
                            chromosome[2] = b.genoToPhenotype()[2];
                        }
                        break;
                    case ONE_BY_ONE:
                        /*for (int i = 0; i < TARGET.length(); i++) {
                            chromosome[i] = (i % 2 == 0) ? a.getChromosome()[i] : b.getChromosome()[i];
                        }*/
                        break;
                    case RANDOM:
                        /*for(int i = 0; i < TARGET.length(); i++) {
                            chromosome[i] = random.nextBoolean() ? a.getChromosome()[(int) (random.nextDouble() * a.getChromosome().length)] : b.getChromosome()[(int) (random.nextDouble() * b.getChromosome().length)];

                        }*/
                        break;
                    case NONE:
                        //chromosome = a.getChromosome().clone();
                        break;

                    default: throw new IllegalArgumentException(String.format("CROSS_TYPE %s not implemented", CROSS_TYPE));
                }
                child = new Individual(0, new Vector3(chromosome[0], chromosome[1], chromosome[2]));


                // mutation
                if(MUTATION_RATE > random.nextDouble()) {
                    double chance = (int) (Math.random() * 3);
                    double factor = 100;
                    if(random.nextBoolean()) {
                        factor = (1 / 100D);
                    }
                    if(chance == 0) {
                        child.setChromosome(child.getChromosome().add(new Vector3(child.getChromosome().getX() * factor, 0, 0)));
                    } else if(chance == 1) {
                        child.setChromosome(child.getChromosome().add(new Vector3(0, child.getChromosome().getY() * factor, 0)));
                    } else if(chance == 2) {
                        child.setChromosome(child.getChromosome().add(new Vector3(0, 0, child.getChromosome().getZ() * factor)));
                    }
                }

                // fitness

                child.setFitness(fitness(child));
                gen.add(child);

            }

            population = gen;

            Individual best = gen.stream().max(Comparator.comparing(Individual::getFitness)).get();
            Individual worst = gen.stream().min(Comparator.comparing(Individual::getFitness)).get();
            double avg = gen.stream().mapToDouble(Individual::getFitness).average().getAsDouble();

            System.out.printf("Generation %d> Best: '%s' (%.2f%%), Worst: '%s' (%.2f%%), Avg. %.2f%%%n",
                    iteration, best.getChromosome(), best.getFitness() * 100, best.getChromosome(), worst.getFitness() * 100, avg * 100
            );

            /*
            if(best.getFitness().compareTo(BigDecimal.ONE) >= 0) {
                requiredGenerations = iteration;
                break;
            }*/


        }

        /*double healthy = population.stream().filter(e -> e.getFitness().compareTo(BigDecimal.ONE) >= 0).count();

        if(healthy > 0) {
            System.out.printf("It took %d generations and %.2f%% of the population is healthy.",
                    requiredGenerations, (healthy / population.size()) * 100D
            );
        } else {
            System.out.printf("Failed to mutate in %d generations.", GENERATIONS);
        }*/

    }

    private static double fitness(Individual i) {
        Universe rocketUniverse = new Universe();
        rocketUniverse._TIME_DELTA = 60;
        rocketUniverse._LOOP_ITERATIONS = (int) (seconds / TIME_STEP);
        //rocketUniverse.getBodies().add(new CannonBall(i.getId(), rocketUniverse.getCelestialBody(399), rocketUniverse.getCelestialBody(699), i.getChromosome()));

        AtomicReference<Double> distance = new AtomicReference<>(Double.MAX_VALUE);
        rocketUniverse.update(u -> {
            u.getBodies().forEach(e -> {
                if(e instanceof CannonBall) {
                    Vector3 vec = e.getPosition().subtract(rocketUniverse.getCelestialBody(699).getPosition());
                    if(distance.get() > vec.length()) {
                        distance.set(vec.length());
                    }
                }
            });
        });
        return (1D / distance.get());
    }

    private enum Selection {

        ELITIST,
        TOURNAMENT

    }

    private enum CrossType {

        HALF_AND_HALF,
        ONE_BY_ONE,
        RANDOM,
        NONE

    }

    public static List<Individual> generatePopulation() {

        Universe universe = new Universe();
        universe._TIME_DELTA = 60;
        universe._LOOP_ITERATIONS = (int) (seconds / 60);

        List<Vector3> positions = new LinkedList<>();
        List<Instant> times = new LinkedList<>();

        Vector3 initialEarthPosition = universe.getObject("Earth (399)").getPosition().clone();
        Vector3 initialEarthVelocity = universe.getObject("Earth (399)").getVelocity().clone();

        universe.update(u -> {

            double distanceEarthSaturn = universe.getObject("Earth (399)").getPosition().subtract(universe.getObject("Saturn (699)").getPosition()).length();
            double distanceEarthTitan = universe.getObject("Earth (399)").getPosition().subtract(universe.getObject("Titan (606)").getPosition()).length();

            if(distanceEarthTitan < distanceEarthSaturn) {
                positions.add(universe.getObject("Titan (606)").getPosition().clone());
                times.add(universe.getCurrentTime());
            }

        });


        List<Individual> list = new ArrayList<>();
        int x = 1;
        for(int i = 0; i < positions.size(); i += 10) {
            Vector3 titan = positions.get(i);
            Instant timeNow = times.get(i);

            double speed = titan.subtract(initialEarthPosition).length() / ((timeNow.toEpochMilli() - universe.getStartTime().toEpochMilli()) / 1000D);
            Vector3 direction = titan.subtract(initialEarthPosition);
            final Vector3 acceleration = direction.normalise().multiply(speed).subtract(initialEarthVelocity).multiply(100);
            list.add(new Individual(-x, acceleration));
            x++;
        }

        // simulate and get fitness
        {
            Universe rocketUniverse = new Universe();
            rocketUniverse._TIME_DELTA = universe._TIME_DELTA;
            rocketUniverse._LOOP_ITERATIONS = (int) (seconds / universe._TIME_DELTA);
            for(Individual individual : list) {
                //rocketUniverse.getBodies().add(new CannonBall(individual.getId(), rocketUniverse.getCelestialBody(399), rocketUniverse.getCelestialBody(699), individual.getChromosome()));
            }

            Map<Integer, Vector3> distances = new HashMap<>();
            for(Individual individual : list) {
                distances.put(individual.getId(), new Vector3(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE));
            }
            rocketUniverse.update(u -> {
                 u.getBodies().forEach(e -> {
                     if(e instanceof CannonBall) {
                         Vector3 vec = e.getPosition().subtract(rocketUniverse.getCelestialBody(699).getPosition());
                         if(distances.get(e.getId()).length() > vec.length()) {
                             distances.put(e.getId(), vec);
                         }
                     }
                 });
            });
            for(Individual individual : list) {
                individual.setFitness(1/distances.get(individual.getId()).length());
            }

        }


        return list;


    }

}