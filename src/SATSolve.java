import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by Eric He on 5/2/17.
 *
 * A SAT-solver approach to re-plan conflicting agents with no optimal non-conflicting paths. The general methodology
 * is taken from "A Simple Approach to Solving Cooperative Path-Finding as Propositional Satisfiability Works Well"
 * by Surynek in 2014. There are some modifications made as well that account for the collision avoidance table in
 * independence detection and bound reduction to get a pareto efficient paths with respect to makespan. Uses sat4j as
 * the SAT solver.
 */

public class SATSolve {

    // class that represents a time (i), vertex (j), and agent (k); the choice of this is based off of the
    // propositional variables in Surynek's paper.
    class Triple {
        int i, j, k;

        public Triple(int i, int j, int k) {
            this.i = i;
            this.j = j;
            this.k = k;
        }

        public int getI() {return this.i;}
        public int getJ() {return this.j;}
        public int getK() {return this.k;}
        public String toString() {return "(" + this.i + ", " + this.j + ", " + this.k + ")";}
    }

    // an initial makespan bound
    private int bound;
    // number of vertices on the board: a square integer
    private int vertices;
    // total number of agents in the game such that all robots iDs are less than or equal to this
    private int agents;
    // length of the board aka the square root of the vertices
    private int l;

    // simple constructor
    public SATSolve(int bound, int gridLength, int agents) {
        this.bound = bound;
        this.vertices = gridLength * gridLength;
        this.agents = agents;
        this.l = gridLength;
    }

    // mapping system that takes a triple and sends it to an integer, for a single agent, a higher number will
    // represent a triple at a higher time step.
    public int mapInt(int time, int vertex, int agent) {
        if (time > bound || vertex > vertices || agent > agents) return -1;
        return (agent - 1) * this.bound * this.vertices + (time - 1) * this.vertices + (vertex - 1) + 1;
    }

    // returns the triple based on an integer given the one-to-one mapping above
    public Triple getTriple(int mappedInt) {
        if (mappedInt == -1) return null;
        int j = (mappedInt - 1) % this.vertices + 1;
        int i = ((mappedInt - (j - 1)) % (this.bound * this.vertices)) / this.vertices + 1;
        int k = (mappedInt - ((i - 1) * this.vertices + (j - 1))) / (this.bound * this.vertices) + 1;
        return new Triple(i, j ,k);
    }

    // gets the corresponding vertex number based on coordinate
    public int getVertexNumber(int x, int y, int l) {
        return l * y + (x + 1);
    }

    // gets the x-coordinate based on vertex number
    public int getXC(int vnum, int l) {
        return (vnum - 1 + l) % l;
    }

    // gets the y-cordinate based on vertex number
    public int getYC(int vnum, int l) {
        return ((vnum - ((vnum - 1 + l) % l)) / l);
    }

    // performs the SAT solving given agents and collision avoidance (leave cat null if no independence detection)
    public int solve(LinkedList<Integer> conflictIDs, HashMap<Integer, Agent> agents,
                     HashMap<Integer, ArrayList<Cell>> cat) throws ContradictionException, TimeoutException {
        // slightly arbitrary, may change later
        final int MAXVAR = 1000000;
        final int NBCLAUSES = 500000;
        ISolver solver = SolverFactory.newDefault();
        solver.newVar(MAXVAR);
        solver.setExpectedNumberOfClauses(NBCLAUSES);

        // Change LinkedList into an array for more convenient clause handling
        int[] conflicts = new int[conflictIDs.size()];
        for (int i = 0; i < conflictIDs.size(); i++) {
            conflicts[i] = conflictIDs.get(i);
        }

        try {
            // add all of the SAT-constraints, most taken from Surynek
            // add starting positions and ending positions
            for (int id : conflicts) {
                Agent a = agents.get(id);
                // set the start positions
                solver.addClause(new VecInt(new int[]{mapInt(1, getVertexNumber(a.getSI(), a.getSJ(), this.l), id)}));
                // set the end positions
                solver.addClause(new VecInt(new int[]{mapInt(this.bound, getVertexNumber(a.getEI(), a.getEJ(), this.l), id)}));
            }

            // at least one vertex occupied at every time step
            for (int id : conflicts) {
                for (int t = 1; t <= this.bound; t++) {
                    int[] oneVertexAtLeast = new int[this.vertices];
                    for (int v = 1; v <= vertices; v++) {
                        oneVertexAtLeast[v - 1] = mapInt(t, v, id);
                    }
                    solver.addClause(new VecInt(oneVertexAtLeast));
                }
            }

            // not more than one vertex occupied at every time step
            for (int id : conflicts) {
                for (int t = 1; t <= this.bound; t++) {
                    for (int i = 1; i < this.vertices; i++) {
                        for (int j = i + 1; j <= this.vertices; j++) {
                            solver.addClause(new VecInt(new int[]{-1 * mapInt(t, i, id), -1 * mapInt(t, j, id)}));
                        }
                    }
                }
            }

            // at most one agent is placed in each vertex at each time step
            for (int t = 1; t <= this.bound; t++) {
                for (int v = 1; v <= this.vertices; v++) {
                    for (int i = 0; i < conflicts.length - 1; i++) {
                        for (int j = i + 1; j < conflicts.length; j++) {
                            int[] x = new int[]{-1 * mapInt(t, v, conflicts[i]), -1 * mapInt(t, v, conflicts[j])};
                            solver.addClause(new VecInt(x));
                        }
                    }
                }
            }

            // an agent relocates to some of its neighbors or makes no move
            for (int id : conflicts) {
                for (int t = 1; t < this.bound; t++) {
                    for (int vx = 0; vx < this.l; vx++) {
                        for (int vy = 0; vy < this.l; vy++) {
                            // top left corner
                            if (vx == 0 && vy == 0) {
                                solver.addClause(new VecInt(new int[]{
                                        -1 * mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx + 1, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy + 1, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx + 1, vy + 1, this.l), id)
                                }));
                                solver.addClause(new VecInt(new int[]{
                                        -1 * mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx + 1, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy + 1, this.l), id),
                                        mapInt(t, getVertexNumber(vx + 1, vy + 1, this.l), id)
                                }));
                            }
                            // top right corner
                            else if (vx == this.l - 1 && vy == 0) {
                                solver.addClause(new VecInt(new int[]{
                                        -1 * mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx - 1, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy + 1, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx - 1, vy + 1, this.l), id)
                                }));
                                solver.addClause(new VecInt(new int[]{
                                        -1 * mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx - 1, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy + 1, this.l), id),
                                        mapInt(t, getVertexNumber(vx - 1, vy + 1, this.l), id)
                                }));
                            }
                            // bottom left corner
                            else if (vx == 0 && vy == this.l - 1) {
                                solver.addClause(new VecInt(new int[]{
                                        -1 * mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy - 1, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx + 1, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx + 1, vy - 1, this.l), id),
                                }));
                                solver.addClause(new VecInt(new int[]{
                                        -1 * mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy - 1, this.l), id),
                                        mapInt(t, getVertexNumber(vx + 1, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx + 1, vy - 1, this.l), id),
                                }));
                            }
                            // bottom right corner
                            else if (vx == this.l - 1 && vy == this.l - 1) {
                                solver.addClause(new VecInt(new int[]{
                                        -1 * mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy - 1, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx - 1, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx - 1, vy - 1, this.l), id)
                                }));
                                solver.addClause(new VecInt(new int[]{
                                        -1 * mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy - 1, this.l), id),
                                        mapInt(t, getVertexNumber(vx - 1, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx - 1, vy - 1, this.l), id)
                                }));
                            }
                            // top edge
                            else if (vy == 0) {
                                solver.addClause(new VecInt(new int[]{
                                        -1 * mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx - 1, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx + 1, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy + 1, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx - 1, vy + 1, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx + 1, vy + 1, this.l), id)
                                }));
                                solver.addClause(new VecInt(new int[]{
                                        -1 * mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx - 1, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx + 1, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy + 1, this.l), id),
                                        mapInt(t, getVertexNumber(vx - 1, vy + 1, this.l), id),
                                        mapInt(t, getVertexNumber(vx + 1, vy + 1, this.l), id)
                                }));
                            }
                            // bottom edge
                            else if (vy == this.l - 1) {
                                solver.addClause(new VecInt(new int[]{
                                        -1 * mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx - 1, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx + 1, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy - 1, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx - 1, vy - 1, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx + 1, vy - 1, this.l), id)
                                }));
                                solver.addClause(new VecInt(new int[]{
                                        -1 * mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx - 1, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx + 1, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy - 1, this.l), id),
                                        mapInt(t, getVertexNumber(vx - 1, vy - 1, this.l), id),
                                        mapInt(t, getVertexNumber(vx + 1, vy - 1, this.l), id)
                                }));
                            }
                            // left edge
                            else if (vx == 0) {
                                solver.addClause(new VecInt(new int[]{
                                        -1 * mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy - 1, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy + 1, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx + 1, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx + 1, vy - 1, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx + 1, vy + 1, this.l), id)
                                }));
                                solver.addClause(new VecInt(new int[]{
                                        -1 * mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy - 1, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy + 1, this.l), id),
                                        mapInt(t, getVertexNumber(vx + 1, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx + 1, vy - 1, this.l), id),
                                        mapInt(t, getVertexNumber(vx + 1, vy + 1, this.l), id)
                                }));
                            }
                            // right edge
                            else if (vy == this.l - 1) {
                                solver.addClause(new VecInt(new int[]{
                                        -1 * mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy + 1, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy + 1, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx - 1, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx - 1, vy + 1, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx - 1, vy + 1, this.l), id)
                                }));
                                solver.addClause(new VecInt(new int[]{
                                        -1 * mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy + 1, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy + 1, this.l), id),
                                        mapInt(t, getVertexNumber(vx - 1, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx - 1, vy + 1, this.l), id),
                                        mapInt(t, getVertexNumber(vx - 1, vy + 1, this.l), id)
                                }));
                            }
                            // normal
                            else {
                                solver.addClause(new VecInt(new int[]{
                                        -1 * mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx - 1, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx + 1, vy, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy - 1, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx, vy + 1, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx - 1, vy - 1, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx - 1, vy + 1, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx + 1, vy - 1, this.l), id),
                                        mapInt(t + 1, getVertexNumber(vx + 1, vy + 1, this.l), id)
                                }));
                                solver.addClause(new VecInt(new int[]{
                                        -1 * mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx - 1, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx + 1, vy, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy - 1, this.l), id),
                                        mapInt(t, getVertexNumber(vx, vy + 1, this.l), id),
                                        mapInt(t, getVertexNumber(vx - 1, vy - 1, this.l), id),
                                        mapInt(t, getVertexNumber(vx - 1, vy + 1, this.l), id),
                                        mapInt(t, getVertexNumber(vx + 1, vy - 1, this.l), id),
                                        mapInt(t, getVertexNumber(vx + 1, vy + 1, this.l), id)
                                }));
                            }
                        }
                    }
                }
            }

            // if cat is provided add simple clauses that avoid collisions
            if (cat != null) {
                for (int i = 1; i <= this.agents; i++) {
                    if (!conflictIDs.contains(i)) {
                        ArrayList<Cell> p = cat.get(i);
                        for (int t = 1; t <= p.size(); t++) {
                            solver.addClause(new VecInt(new int[]{-1 * mapInt(t, getVertexNumber(p.get(t - 1).i, p.get(t - 1).j, this.l), i)}));
                        }
                    }
                }
            }
        }
        catch (ContradictionException e) {
            return -1;
        }

        // check to see if the problem is satisfiable
        if (solver.isSatisfiable()) {
            // go through conflicting agents and determine their paths, use bound reduction to get shorter paths
            for (int i : conflicts) {
                Agent a = agents.get(i);
                // bound starts at original and then slowly decremented
                int bound = this.bound;
                // keep track of last constraints
                IConstr lastFailure = null;
                // while the problem is satisfiable, try to reduce the bound
                // once a failure occurs revert the constraint
                boolean needToRemove = true;
                while (solver.isSatisfiable()) {
                    bound--;
                    try {
                        lastFailure = solver.addClause(new VecInt(
                                new int[]{mapInt(bound, getVertexNumber(a.getEI(), a.getEJ(), this.l), i)}));
                    }
                    catch (ContradictionException e) {
                        needToRemove = false;
                        break;
                    }
                }
                if (needToRemove) solver.removeConstr(lastFailure);
            }


            // loop through all the paths for final clean-up
            // keep track of makespan for the group
            int mpl = -1;
            if (solver.isSatisfiable()) {
                for (int i : conflicts) {
                    ArrayList<Cell> path = new ArrayList<>();
                    for (int k = (i - 1) * this.bound * this.vertices + 1; k <= i * this.bound * this.vertices; k++) {
                        if (solver.model(k)) {
                            Triple t = getTriple(k);
                            path.add(new Cell(getXC(t.getJ(), this.l), getYC(t.getJ(), this.l)));
                        }
                    }

                    // trim paths
                    for (int j = this.bound - 1; j > 0; j--) {
                        if (path.get(j).toString().equals(path.get(j - 1).toString()))
                            path.remove(j);
                        else {
                            break;
                        }
                    }
                    // set paths
                    Agent a = agents.get(i);
                    a.setPath(path);
                    a.setIncorrect();
                    // update cat
                    if (cat != null) {
                        cat.remove(i);
                        cat.put(i, a.getPath());
                    }
                    // update makespan
                    mpl = Math.max(mpl, path.size());
                }
            }
            return mpl;
        }
        else {
            return -1;
        }
    }
}

