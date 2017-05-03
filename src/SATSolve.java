/**
 * Created by ehe on 5/2/17.
 */

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class SATSolve {

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

    private int bound;
    private int vertices;
    private int agents;
    private int l;

    public SATSolve(int bound, int gridLength, int agents) {
        this.bound = bound;
        this.vertices = gridLength * gridLength;
        this.agents = agents;
        this.l = gridLength;
    }

    public int mapInt(int time, int vertex, int agent) {
        if (time > bound || vertex > vertices || agent > agents) return -1;
        return (agent - 1) * this.bound * this.vertices + (time - 1) * this.vertices + (vertex - 1) + 1;
    }

    public Triple getTriple(int mappedInt) {
        if (mappedInt == -1) return null;
        int j = (mappedInt - 1) % this.vertices + 1;
        int i = ((mappedInt - (j - 1)) % (this.bound * this.vertices)) / this.vertices + 1;
        int k = (mappedInt - ((i - 1) * this.vertices + (j - 1))) / (this.bound * this.vertices) + 1;
        return new Triple(i, j ,k);
    }

    public int getVertexNumber(int x, int y, int l) {
        return l * y + (x + 1);
    }

    public int getXC(int vnum, int l) {
        return (vnum - 1 + l) % l;
    }

    public int getYC(int vnum, int l) {
        return ((vnum - ((vnum - 1 + l) % l)) / l);
    }

    public int solve(LinkedList<Integer> conflictIDs, HashMap<Integer, Agent> agents, HashMap<Integer, ArrayList<Cell>> cat) throws ContradictionException, TimeoutException {
        final int MAXVAR = 1000000;
        final int NBCLAUSES = 500000;

        ISolver solver = SolverFactory.newDefault();
        solver.newVar(MAXVAR);
        solver.setExpectedNumberOfClauses(NBCLAUSES);

        // Change LinkedList into an array for better clause handling
        int[] conflicts = new int[conflictIDs.size()];
        for (int i = 0; i < conflictIDs.size(); i++) {
            conflicts[i] = conflictIDs.get(i);
        }

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
                for (int v = 1; v <= 25; v++) {
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
                        solver.addClause(new VecInt(new int[]{-1 * mapInt(t, v, conflicts[i]), -1 * mapInt(t, v, conflicts[j])}));
                    }
                }
            }
        }

        // an agent relocates to some of its neighbors or makes no move
        for (int id: conflicts) {
            Agent a = agents.get(id);
            for (int t = 1; t < this.bound; t++) {
                for (int vx = 0; vx < this.l; vx++) {
                    for (int vy = 0; vy < this.l; vy++) {
                        //top left corner
                        if (vx == 0 && vy == 0) {
                            solver.addClause(new VecInt(new int[]{
                                    -1 * mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                    mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                    mapInt(t + 1, getVertexNumber(vx + 1, vy, this.l), id),
                                    mapInt(t + 1, getVertexNumber(vx, vy + 1, this.l), id),
                                    mapInt(t + 1, getVertexNumber(vx + 1, vy  + 1, this.l), id)
                            }));
                            solver.addClause(new VecInt(new int[]{
                                    -1 * mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                    mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                    mapInt(t, getVertexNumber(vx + 1, vy, this.l), id),
                                    mapInt(t, getVertexNumber(vx, vy + 1, this.l), id),
                                    mapInt(t, getVertexNumber(vx + 1, vy  + 1, this.l), id)
                            }));
                        }
                        //top right corner
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
                        //bottom left corner
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
                        //bottom right corner
                        else if (vx == this.l - 1 && vy == this.l - 1) {
                            solver.addClause(new VecInt(new int[]{
                                    -1 * mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                    mapInt(t + 1, getVertexNumber(vx, vy, this.l), id),
                                    mapInt(t + 1, getVertexNumber(vx, vy - 1, this.l), id),
                                    mapInt(t + 1, getVertexNumber(vx - 1, vy, this.l), id),
                                    mapInt(t + 1, getVertexNumber(vx - 1, vy - 1, this.l), id)
                            }));
                            solver.addClause(new VecInt(new int[]{
                                    -1 * mapInt(t  + 1, getVertexNumber(vx, vy, this.l), id),
                                    mapInt(t, getVertexNumber(vx, vy, this.l), id),
                                    mapInt(t, getVertexNumber(vx, vy - 1, this.l), id),
                                    mapInt(t, getVertexNumber(vx - 1, vy, this.l), id),
                                    mapInt(t, getVertexNumber(vx - 1, vy - 1, this.l), id)
                            }));
                        }
                        //top edge
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
                        //bottom edge
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
                        //left edge
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
                        //right edge
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
                        //normal
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

        // cat avoidance
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

        IProblem problem = solver;
        if (problem.isSatisfiable()) {
            HashMap<Integer, ArrayList<Cell>> paths = new HashMap<>();
            for (int num = 1; num <= this.agents; num++) {
                ArrayList<Cell> p = new ArrayList<>();
                paths.put(num, p);
            }
            for (int i : conflicts) {
                Agent a = agents.get(i);
                int origBound = this.bound;
                IConstr lastFailure = null;
                boolean needToRemove = true;
                while (problem.isSatisfiable()) {
                    origBound--;
                    try {
                        lastFailure = solver.addClause(new VecInt(new int[]{mapInt(origBound, getVertexNumber(a.getEI(), a.getEJ(), this.l), i)}));
                    }
                    catch (ContradictionException e) {
                        needToRemove = false;
                        break;
                    }
                }
                if (needToRemove) solver.removeConstr(lastFailure);

                if (problem.isSatisfiable()) {
                    for (int k = (i - 1) * this.bound * this.vertices + 1; k <= i * this.bound * this.vertices; k++) {
                        if (problem.model(k)) {
                            Triple t = getTriple(k);
                            ArrayList<Cell> x = paths.get(t.getK());
                            x.add(new Cell(getXC(t.getJ(), this.l), getYC(t.getJ(), this.l)));
                        }
                    }
                }
            }

            int mpl = -1;
            for (int i : conflicts) {
                ArrayList<Cell> p = paths.get(i);
                for (int j = this.bound - 1; j > 0; j--) {
                    if (p.get(j).toString().equals(p.get(j - 1).toString()))
                        p.remove(j);
                    else {
                        break;
                    }
                }
                System.out.print(i + ":");
                for (Cell s : paths.get(i)) {
                    System.out.print(s + " ");
                }
                System.out.println();
                Agent a = agents.get(i);
                a.setPath(p);
                a.setIncorrect();
                cat.remove(i);
                cat.put(i, a.getPath());
                mpl = Math.max(mpl, p.size());
            }
            return mpl;
        }
        else {
            return -1;
        }
    }

    public static void main (String[] args) throws ContradictionException, TimeoutException {
        Cell [][] grid = new Cell[5][5];
        for(int i = 0; i < 5; ++i) {
            for(int j = 0; j < 5; ++j) {
                grid[i][j] = new Cell(i, j);
            }
        }
        SATSolve s = new SATSolve(10, 5 ,8);
        Agent r4 = new Agent(4, 5,0, 0, 4, 4);
        Agent r7 = new Agent(7, 5,4, 4, 0, 0);
        LinkedList<Integer> ids = new LinkedList<>();
        ids.add(4);
        ids.add(7);
        HashMap<Integer, Agent> agents = new HashMap<>();
        agents.put(4, r4);
        agents.put(7, r7);
        System.out.println(s.solve(ids, agents, null));
    }
}
