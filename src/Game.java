import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Created by ehe on 4/26/17.
 */
public class Game {

    // agents in the game
    private LinkedList<Agent> agents;
    // dimensions of the board
    private int x, y;
    // collision avoidance table
    private HashMap<Integer, Cell[]> cat;

    public Game(LinkedList<Agent> agents, int x, int y) {
        this.agents = agents;
        this.x = x;
        this.y = y;
    }

    public void run() {
        cat = new HashMap<>();
        int maxPathLength = 0;
        for (Agent a : agents) {
            maxPathLength = Math.max(maxPathLength, a.getPathLength());
            cat.put(a.getID(), a.getPath());
            for (Cell c : a.getPath()) {
                System.out.print(c + " ");
            }
            System.out.println();
        }

        System.out.println(maxPathLength);
        boolean success = true;
        for (int i = 0; i < maxPathLength; i++) {
            HashSet<String> checker = new HashSet<>();
            for (int j = 1; j <= agents.size(); j++) {
                Cell[] p = cat.get(j);

                if (i >= p.length) continue;
                if (!checker.contains(p[i].toString()))
                    checker.add(p[i].toString());
                else {
                    System.out.println("Broke at step: " + i);
                    success = false;
                    break;
                }
            }
        }

        if (success) System.out.println("Success!");
    }

    public static void main(String[] args) {
        LinkedList<Agent> a = new LinkedList<>();
        Agent r1 = new Agent(1, 5, 5, 0, 0, 4, 4);
        Agent r2 = new Agent(2, 5, 5, 4, 4, 0, 0);
        a.add(r1);
        a.add(r2);
        Game game = new Game(a, 5, 5);
        game.run();
    }


}
