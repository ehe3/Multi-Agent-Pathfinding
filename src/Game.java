import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by Eric He on 4/26/17.
 *
 * A solver and instance creator for the multi-agent pathfinding problem that uses a hybrid independence detection
 * and SAT-solving algorithm.
 *
 * The A* search is based off of the code found here: http://www.codebytes.in/2015/02/a-shortest-path-finding-algorithm.html
 */

public class Game {

    // agents in the game
    private HashMap<Integer, Agent> agents;
    // board length
    private int l;
    // collision avoidance table
    private HashMap<Integer, ArrayList<Cell>> cat;
    // max path length
    private int mpl;
    // max-replan size
    private int maxReplan;
    // number of agents
    private int num;
    // keep track of groups
    private UnionFind groups;
    // total SAT collisions resolved
    private int SATc;
    // total ID collisions resolved
    private int IDc;
    // makespan bound
    private int bound;

    // simple constructor
    public Game(int l, int bound) {
        this.agents = new HashMap<>();
        this.l = l;
        this.num = 0;
        this.maxReplan = 1;
        this.SATc = 0;
        this.IDc = 0;
        this.bound = bound;
    }

    // adds an agent to the game, assumes agents with lesser IDs from 1 already added in
    public boolean add(Agent a) {
        for (int i = 1; i < a.getID(); i++) {
            Agent b = agents.get(i);
            if ((a.getSI() == b.getSI() && a.getSJ() == b.getSJ()) ||
                    (a.getEI() == b.getEI() && a.getEJ() == b.getEJ()))
                return false;
        }
        if (a.getSI() == a.getEI() && a.getSJ() == a.getEJ())
            return false;
        this.agents.put(a.getID(), a);
        this.num++;
        return true;
    }

    // return current max path length
    public int getMPL() {
        return this.mpl;
    }

    // return size of largest replan using SAT
    public int getMaxReplan() {
        return this.maxReplan;
    }

    // return the number of SAT collisions resolved during the algorithm
    public int getNumSATCollisionsResolved() {
        return this.SATc;
    }

    // return the number of ID collisions resolved during the algorithm
    public int getNumIDCollisionsResolved() {
        return this.IDc;
    }

    // goes through the collision avoidance table to see if there are currently any collisions
    // collisions occur when two robots are at the same vertex at the same time
    private LinkedList<Integer> detectCollision () {
        LinkedList<Integer> collides = new LinkedList<>();

        for (int i = 0; i < this.mpl; i++) {
            HashMap<String, Integer> checker = new HashMap<>();
            for (int j = 1; j <= agents.size(); j++) {
                ArrayList<Cell> p = cat.get(j);

                if (i >= p.size()) continue;
                String currCell = p.get(i).toString();
                if (!checker.containsKey(currCell))
                    checker.put(currCell, j);
                else {
                    collides.add(checker.get(currCell));
                    collides.add(j);
                    return collides;
                }
            }
        }
        return null;
    }

    // runs the simulation and solves the instance
    public boolean run() throws TimeoutException, ContradictionException {
        // UF-data structure to keep track of merging
        this.groups = new UnionFind(this.num);
        // create a collision avoidance table and update it using paths, update makespan while at it
        cat = new HashMap<>();
        int maxPathLength = 0;
        for (int i = 1; i <= num; i++) {
            Agent a = agents.get(i);
            maxPathLength = Math.max(maxPathLength, a.getPathLength());
            cat.put(a.getID(), a.getPath());
        }
        this.mpl = maxPathLength;

        // simulate until no more collisions
        LinkedList<Integer> c = detectCollision();
        while (c != null) {
            // first agent
            int i = c.get(0);
            Agent a = agents.get(i);
            // second agent
            int j = c.get(1);
            Agent b = agents.get(j);

            //only if both are single use ID replan
            if (a.isSingle() && b.isSingle()) {
                int bound1 = a.getPathCost();
                ArrayList<Cell> oldPath1 = a.getPath();
                a.AStar(cat);
                // replan for first fails
                if (a.getPathCost() != bound1) {
                    a.setPath(oldPath1);
                    a.setPathCost(bound1);
                    // try to replan second
                    int bound2 = b.getPathCost();
                    ArrayList<Cell> oldPath2 = b.getPath();
                    b.AStar(cat);
                    // replan for second fails
                    if (b.getPathCost() != bound2) {
                        b.setPath(oldPath2);
                        b.setPathCost(bound2);
                        LinkedList<Integer> satReplan = groups.merge(i, j);
                        // set tentative makespan bound
                        SATSolve sat = new SATSolve(this.bound, l, this.num);
                        int s = sat.solve(satReplan, agents, cat);
                        if (s == -1) {
                            System.out.println();
                            System.out.println("FAILED");
                            System.out.println();
                            return false;
                        }
                        else {
                            // if ID does not work use SAT-solver
                            this.SATc++;
                            a.unsingle();
                            b.unsingle();
                            a.setIncorrect();
                            b.setIncorrect();
                            this.mpl = Math.max(s, this.mpl);
                            this.maxReplan = Math.max(satReplan.size(), this.maxReplan);
                        }
                        c = detectCollision();
                    }
                    // replan for second succeeds + update cat
                    else {
                        this.IDc++;
                        cat.remove(j);
                        cat.put(j, b.getPath());
                        c = detectCollision();
                    }
                } else {
                    // replan for first succeeds + update cat
                    this.IDc++;
                    cat.remove(i);
                    cat.put(i, a.getPath());
                    c = detectCollision();
                }
            }
            // if not singular agent it means it was already replanned using a SAT-solver, then replan again with SAT
            else {
                LinkedList<Integer> satReplan = groups.merge(i, j);
                // set tentative makespan bound
                SATSolve sat = new SATSolve(this.bound, l, this.num);
                int s = sat.solve(satReplan, agents, cat);
                if (s == -1) {
                    System.out.println();
                    System.out.println("FAILED");
                    System.out.println();
                    return false;
                }
                else {
                    a.unsingle();
                    b.unsingle();
                    a.setIncorrect();
                    b.setIncorrect();
                    this.mpl = Math.max(s, this.mpl);
                    this.maxReplan = Math.max(satReplan.size(), this.maxReplan);
                }
                c = detectCollision();
            }
        }
        return true;
    }
}
