import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by Eric He on 5/3/17.
 *
 * Runs multiple simulations of Games to determine statistics needed for data analysis
 */
public class Simulator {

    public static void main(String[] args) throws TimeoutException, ContradictionException {
        // SAT vs SAT + ID time, 0.25 Density for 2x2, 4x4, 6x6, 8x8, 30 iterations
        System.out.println();
        System.out.println("**********Test 1 Begins**********");
        System.out.println();
        for (int i = 2; i <= 8; i += 2) {
            int robots = (i * i) / 4;
            SATSolve sat = new SATSolve(2 * i, i, robots);
            long IDtot = 0;
            long SATtot = 0;
            int xCount = 0;
            int yCount = 0;
            for (int iter = 1; iter <= 30; iter++) {
                LinkedList<Integer> conflictIDs = new LinkedList<>();
                HashMap<Integer, Agent> ags = new HashMap<>();
                Game g = new Game(i, 2 * i);
                for (int a = 1; a <= robots; a++) {
                    Agent jamesBond = new Agent(a, i);
                    while (!g.add(jamesBond)) {
                        jamesBond = new Agent(a, i);
                    }
                    conflictIDs.add(a);
                    ags.put(a, jamesBond);
                }
                // add timer here
                long startTime1 = System.currentTimeMillis();
                boolean x = g.run();
                long endTime1 = System.currentTimeMillis();
                if (x) {
                    IDtot += (endTime1 - startTime1);
                    xCount++;
                }
                // add timer here
                long startTime2 = System.currentTimeMillis();
                int y = sat.solve(conflictIDs, ags, null);
                long endTime2 = System.currentTimeMillis();
                if (y != -1) {
                    SATtot += (endTime2 - endTime1);
                    yCount++;
                }

            }
            System.out.println("Test 1: Complete for " + i + "x" + i + " grid");
            System.out.println("Success rate for ID + SAT: " + xCount + "/30");
            System.out.println("Avg ID + SAT time: " + ((double) IDtot) / xCount);
            System.out.println("Success rate for SAT: " + yCount + "/30");
            System.out.println("Avg SAT time: " + ((double) SATtot) / yCount);
            System.out.println();
        }
        System.out.println("Test 1 Completed");

        // SAT vs SAT + ID time, 0.5 Density for 2x2, 4x4, 6x6, 8x8, 30 iterations
        System.out.println();
        System.out.println();
        System.out.println("**********Test 2 Begins**********");
        System.out.println();
        for (int i = 2; i <= 8; i += 2) {
            int robots = (i * i) / 4;
            SATSolve sat = new SATSolve(2 * i, i, robots);
            long IDtot = 0;
            long SATtot = 0;
            int xCount = 0;
            int yCount = 0;
            for (int iter = 1; iter <= 30; iter++) {
                LinkedList<Integer> conflictIDs = new LinkedList<>();
                HashMap<Integer, Agent> ags = new HashMap<>();
                Game g = new Game(i, 2 * i);
                for (int a = 1; a <= robots; a++) {
                    Agent jamesBond = new Agent(a, i);
                    while (!g.add(jamesBond)) {
                        jamesBond = new Agent(a, i);
                    }
                    conflictIDs.add(a);
                    ags.put(a, jamesBond);
                }
                // add timer here
                long startTime1 = System.currentTimeMillis();
                boolean x = g.run();
                long endTime1 = System.currentTimeMillis();
                if (x) {
                    IDtot += (endTime1 - startTime1);
                    xCount++;
                }
                // add timer here
                long startTime2 = System.currentTimeMillis();
                int y = sat.solve(conflictIDs, ags, null);
                long endTime2 = System.currentTimeMillis();
                if (y != -1) {
                    SATtot += (endTime2 - endTime1);
                    yCount++;
                }

            }
            System.out.println("Test 2: Complete for " + i + "x" + i + " grid");
            System.out.println("Success rate for ID + SAT: " + xCount + "/30");
            System.out.println("Avg ID + SAT time: " + ((double) IDtot) / xCount);
            System.out.println("Success rate for SAT: " + yCount + "/30");
            System.out.println("Avg SAT time: " + ((double) SATtot) / yCount);
            System.out.println();
        }
        System.out.println("Test 2 Completed");

        System.out.println();
        System.out.println("**********Test 3 Begins**********");
        System.out.println();
        // largest replan size/ total agents + num replans
        // for densities about 0.2, 0.5, 0.8 for grids 4x4, 5x5, 6x6, 7x7, 30 iterations
        for (int i = 4; i <= 7; i++) {
            for (double d = 0.2; d <= 0.8; d += 0.3) {
                int IDtot = 0;
                int ReplansTot = 0;
                int xCount = 0;
                int robots = (int) (i * i * d);
                for (int iter = 1; iter <= 30; iter++) {
                    Game g = new Game(i, 2 * i);
                    for (int a = 1; a <= robots; a++) {
                        Agent jamesBond = new Agent(a, i);
                        while (!g.add(jamesBond)) {
                            jamesBond = new Agent(a, i);
                        }
                    }
                    // add timer here
                    long startTime1 = System.currentTimeMillis();
                    boolean x = g.run();
                    long endTime1 = System.currentTimeMillis();
                    if (x) {
                        IDtot += g.getMaxReplan();
                        ReplansTot += (g.getNumSATCollisionsResolved() + g.getNumIDCollisionsResolved());
                        xCount++;
                    }
                }
                System.out.println("Test 3: Complete for " + i + "x" + i + " grid, density: " + d);
                System.out.println("Success rate for ID + SAT: " + xCount + "/30");
                System.out.println("Avg max replan Size out of Agents: " + ((double) IDtot) / xCount + "/" + (robots));
                System.out.println("Number of total replans: " + ((double) ReplansTot) / xCount);
                System.out.println();
            }
        }
        System.out.println("Test 3 Completed");
    }
}
