/**
 * Created by ehe on 5/2/17.
 */

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.*;

import java.lang.reflect.Array;
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

    public SATSolve(int bound, int vertices, int agents) {
        this.bound = bound;
        this.vertices = vertices;
        this.agents = agents;
        this.l = (int) Math.sqrt(vertices);
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

    public String getCoordinate(int vnum, int l) {
        int x = (vnum - 1 + l) % l;
        return "(" + x + "," + ((vnum - x) / l) + ")";
    }

    public void solve(LinkedList<Integer> conflictIDs, HashMap<Integer, Agent> agents) throws ContradictionException, TimeoutException {
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

//            // end position at at least one vertex in time bound
//            int [] endPosClause = new int[this.bound];
//            int endV = getVertexNumber(a.getEI(), a.getEJ(), this.l);
//            for (int t = 1; t <= this.bound; t++)
//                endPosClause[t - 1] = mapInt(t, endV, id);
//            solver.addClause(new VecInt(endPosClause));
//
//            // does not move after end vertex is hit
//            for (int j = 1; j < this.bound; j++) {
//                for (int k = j + 1; k <= this.bound; k++ )
//                solver.addClause(new VecInt(new int[]{-1 * mapInt(j, endV, id), mapInt(k, endV, id)}));
//            }

            // an agent is placed in exactly one vertex at each time step
            for (int n = 1; n < this.bound; n++) {
                // not more than one vertex occupied at every time step
                int [] oneVertexAtLeast = new int[this.vertices];
                for (int i = 1; i < this.vertices; i++) {
                    for (int j = i + 1; j <= this.vertices; j++) {
                        solver.addClause(new VecInt(new int[]{-1 * mapInt(n, i, id), -1 * mapInt(n, j, id)}));
                    }
                    oneVertexAtLeast[i - 1] = mapInt(n, i, id);
                }
                // at least one vertex occupied at every time step
                oneVertexAtLeast[this.vertices - 1] = mapInt(n, this.vertices, id);
                solver.addClause(new VecInt(oneVertexAtLeast));
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

        IProblem problem = solver;
//        if (problem.isSatisfiable()) {
//            System.out.println("Satisfiable !");
//            HashMap<Integer, String[]> paths = new HashMap<>();
//            for (int num = 1; num <= this.agents; num++) {
//                String[] p = new String[this.bound];
//                paths.put(num, p);
//            }
//            for (int i = 1; i <= this.agents * this.bound * this.vertices; i++) {
//                if (problem.model(i)) {
//                    Triple t = getTriple(i);
//                    String[] x = paths.get(t.getK());
//                    x[t.getI() - 1] = getCoordinate(t.getJ(), this.l);
//                }
//            }
//            for (int k = 1; k <= this.agents; k++) {
//                System.out.print(k + ": ");
//                for (String s : paths.get(k))
//                    System.out.print(s + " ");
//                System.out.println();
//            }
//        } else {
//            System.out.println("Unsatisfiable !");
//        }

        if (problem.isSatisfiable()) {
            HashMap<Integer, ArrayList<String>> paths = new HashMap<>();
            for (int num = 1; num <= this.agents; num++) {
                ArrayList<String> p = new ArrayList<>();
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
                            ArrayList<String> x = paths.get(t.getK());
                            String curr = getCoordinate(t.getJ(), this.l);
                            x.add(curr);
                        }
                    }
                }
            }

            for (int i : conflicts) {
                ArrayList<String> p = paths.get(i);
                for (int j = this.bound - 1; j > 0; j--) {
                    if (p.get(j).equals(p.get(j - 1)))
                        p.remove(j);
                    else {
                        break;
                    }
                }
                System.out.print(i + ":");
                for (String s : paths.get(i)) {
                    System.out.print(s + " ");
                }
                System.out.println();
            }
        }
        else {
            System.out.println("Not satisfiable");
        }
    }

    public static void main (String[] args) throws ContradictionException, TimeoutException {
        SATSolve s = new SATSolve(10, 25 ,2);
        Agent r1 = new Agent(1, 5,0, 0, 4, 4);
        Agent r2 = new Agent(2, 5,4, 4, 0, 0);
        LinkedList<Integer> ids = new LinkedList<>();
        ids.add(1);
        ids.add(2);
        HashMap<Integer, Agent> agents = new HashMap<>();
        agents.put(1, r1);
        agents.put(2, r2);
        s.solve(ids, agents);

    }
}
