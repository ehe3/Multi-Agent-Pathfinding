import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by ehe on 4/26/17.
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
    // number of agents
    private int num;
    // keep track of groups
    private UnionFind groups;

    public Game(int l) {
        this.agents = new HashMap<Integer, Agent>();
        this.l = l;
        this.num = 0;
    }

    public void add(Agent a) {
        this.agents.put(a.getID(), a);
        this.num++;
    }

    private LinkedList<Integer> detectCollision () {
        LinkedList<Integer> colliders = new LinkedList<>();

        for (int i = 0; i < this.mpl; i++) {
            HashMap<String, Integer> checker = new HashMap<>();
            for (int j = 1; j <= agents.size(); j++) {
                ArrayList<Cell> p = cat.get(j);

                if (i >= p.size()) continue;
                String currCell = p.get(i).toString();
                if (!checker.containsKey(currCell))
                    checker.put(currCell, j);
                else {
                    colliders.add(checker.get(currCell));
                    colliders.add(j);
                    return colliders;
                }
            }
        }
        return null;
    }

    private void printCat() {
        for (int i = 1; i <= num; i++) {
            ArrayList<Cell> p = this.cat.get(i);
            System.out.print(i + ": ");
            for (Cell c : p)
                System.out.print(c + " ");
            System.out.println();
        }
    }

    public void run() throws TimeoutException, ContradictionException {
        System.out.println("HELLO: " + this.num);
        this.groups = new UnionFind(this.num);


        cat = new HashMap<>();
        int maxPathLength = 0;
        for (int i = 1; i <= num; i++) {
            Agent a = agents.get(i);
            maxPathLength = Math.max(maxPathLength, a.getPathLength());
            cat.put(a.getID(), a.getPath());
            for (Cell c : a.getPath()) {
                System.out.print(c + " ");
            }
            System.out.println();
        }
        this.mpl = maxPathLength;

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
                        a.unsingle();
                        b.unsingle();
                        SATSolve sat = new SATSolve(2 * l, l, this.num);
                        int s = sat.solve(satReplan, agents, cat);
                        if (s == -1) {
                            System.out.println("Even SAT Failed :(");
                            break;
                        }
                        else {
                            this.mpl = Math.max(s, this.mpl);
                        }
                        c = detectCollision();
                        System.out.println(c != null);
                    }
                    // replan for second succeeds + update cat
                    else {
                        cat.remove(j);
                        cat.put(j, b.getPath());
                        c = detectCollision();
                    }
                } else {
                    // replan for first succeeds + update cat
                    cat.remove(i);
                    cat.put(i, a.getPath());
                    c = detectCollision();
                }
            }
            else {
                LinkedList<Integer> satReplan = groups.merge(i, j);
                a.unsingle();
                b.unsingle();
                SATSolve sat = new SATSolve(2 * l, l, this.num);
                int s = sat.solve(satReplan, agents, cat);
                if (s == -1) {
                    System.out.println("Even SAT Failed :(");
                    break;
                }
                else {
                    this.mpl = Math.max(s, this.mpl);
                }
                c = detectCollision();
            }
        }
        System.out.println();
        System.out.println("FINAL PATHS");
        System.out.println();
        for (int i = 1; i <= num; i++) {
            System.out.print(i + ":");
            Agent a = agents.get(i);
            for (Cell k : a.getPath()) {
                System.out.print(k + " ");
            }
            System.out.println();
        }

        System.out.println("Max Path Length of this Game is: " + this.mpl);
    }

    public static void main(String[] args) throws TimeoutException, ContradictionException {
        Game game = new Game(5);
        Agent r1 = new Agent(1, 5,0, 0, 0, 4);
        Agent r2 = new Agent(2, 5,0, 4, 0, 0);
        Agent r3 = new Agent(3, 5,4, 0 ,4 ,4);
        Agent r4 = new Agent(4, 5,4, 4 ,4 ,0);
        game.add(r1);
        game.add(r2);
        game.add(r3);
        game.add(r4);
        game.run();
    }


}
