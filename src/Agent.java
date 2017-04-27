import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;


public class Agent {
    private static final int DCOST = 14;
    private static final int VCOST = 10;

    // identifier
    private int id;
    // dimensions of the grid
    private int x, y;
    // start position
    private int si, sj;
    // end position
    private int ei, ej;
    // path for independent A*
    private Cell[] path;
    // cost of current path
    private int pathCost;
    // length of current path
    private int pathLength;

    public Agent(int id, int x, int y, int si, int sj, int ei, int ej) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.si = si;
        this.sj = sj;
        this.ei = ei;
        this.ej = ej;
        // run A* to determine the independent path
        AStar();
    }

    // returns the id of a robot
    public int getID() {
        return id;
    }

    // returns orig A* path of robot
    public Cell[] getPath() {
        return this.path;
    }

    // returns the path cost
    public int getPathCost() {
        return this.pathCost;
    }

    // returns the path length
    public int getPathLength() {
        return this.pathLength;
    }

    // helper to perform A* updates
    private void checkAndUpdateCost(Cell current, Cell t, int cost, PriorityQueue<Cell> open, boolean[][] closed){
        if(t == null || closed[t.i][t.j]) return;
        int t_final_cost = t.heuristicCost +cost;
        boolean inOpen = open.contains(t);
        if(!inOpen || t_final_cost < t.finalCost){
            t.finalCost = t_final_cost;
            t.parent = current;
            if(!inOpen) open.add(t);
        }
    }

    // runs A* and sets the path and pathCost instance variables
    public void AStar(){
        //Reset
        Cell [][] grid = new Cell[x][y];
        boolean [][] closed = new boolean[x][y];
        PriorityQueue<Cell> open = new PriorityQueue<>((Object o1, Object o2) -> {
            Cell c1 = (Cell)o1;
            Cell c2 = (Cell)o2;

            return c1.finalCost < c2.finalCost ? -1 :
                    c1.finalCost > c2.finalCost ? 1 : 0;
        });

        for(int i = 0; i < x; ++i) {
            for(int j = 0; j < y; ++j) {
                grid[i][j] = new Cell(i, j);
                grid[i][j].heuristicCost = Math.abs(i - ei) + Math.abs(j - ej);
            }
        }
        grid[si][sj].finalCost = 0;

        System.out.println("Grid: ");
        for(int i=0;i<x;++i){
            for(int j=0;j<y;++j){
                if(i==si&&j==sj)System.out.print("SO  "); //Source
                else if(i==ei && j==ej)System.out.print("DE  ");  //Destination
                else if(grid[i][j]!=null)System.out.printf("%-3d ", 0);
                else System.out.print("BL  ");
            }
            System.out.println();
        }
        System.out.println();

        // add the start location to open list.
        open.add(grid[si][sj]);

        // performs the A* search
        Cell current;
        while(true) {
            current = open.poll();
            if(current == null) break;
            closed[current.i][current.j] = true;

            if(current.equals(grid[ei][ej])){
                break;
            }

            Cell t;
            if(current.i - 1 >= 0){
                t = grid[current.i - 1][current.j];
                checkAndUpdateCost(current, t, current.finalCost + VCOST, open, closed);

                if(current.j - 1 >= 0){
                    t = grid[current.i - 1][current.j - 1];
                    checkAndUpdateCost(current, t, current.finalCost + DCOST, open, closed);
                }

                if(current.j + 1 < grid[0].length) {
                    t = grid[current.i - 1][current.j + 1];
                    checkAndUpdateCost(current, t, current.finalCost + DCOST, open, closed);
                }
            }

            if(current.j - 1 >= 0){
                t = grid[current.i][current.j - 1];
                checkAndUpdateCost(current, t, current.finalCost + VCOST, open, closed);
            }

            if(current.j + 1 < grid[0].length){
                t = grid[current.i][current.j + 1];
                checkAndUpdateCost(current, t, current.finalCost + VCOST, open, closed);
            }

            if(current.i + 1 < grid.length){
                t = grid[current.i + 1][current.j];
                checkAndUpdateCost(current, t, current.finalCost + VCOST, open, closed);

                if(current.j - 1 >= 0){
                    t = grid[current.i + 1][current.j - 1];
                    checkAndUpdateCost(current, t, current.finalCost + DCOST, open, closed);
                }

                if(current.j + 1 < grid[0].length){
                    t = grid[current.i + 1][current.j + 1];
                    checkAndUpdateCost(current, t, current.finalCost + DCOST, open, closed);
                }
            }
        }

        System.out.println("\nScores for cells: ");
        for(int i=0;i<x;++i){
            for(int j=0;j<x;++j){
                if(grid[i][j]!=null)System.out.printf("%-3d ", grid[i][j].finalCost);
                else System.out.print("BL  ");
            }
            System.out.println();
        }
        System.out.println();

        pathCost = 0;
        int score = 0;
        int pathLength = 1;
        if(closed[ei][ej]) {
            // first pass: determine path length and cost
            Cell c = grid[ei][ej];
            while(c.parent != null) {
                pathLength ++;
                score = Math.abs(c.i - c.parent.i) + Math.abs(c.j - c.parent.j);
                if (score == 1) pathCost += VCOST;
                else if (score == 2) pathCost += DCOST;
                c = c.parent;
            }
            this.pathCost = pathCost;
            this.pathLength = pathLength;

            c = grid[ei][ej];
            path = new Cell[pathLength];
            path[pathLength - 1] = c;
            // second pass: add to path
            while (c.parent != null) {
                pathLength--;
                path[pathLength - 1] = c.parent;
                c = c.parent;
            }

        } else {
            this.pathCost = -1;
            return;
        }
        this.path = path;
    }

    public static void main(String[] args) throws Exception{
        Agent robot = new Agent(1, 5, 5, 0, 0, 3, 2);
        Cell [] path = robot.getPath();
        for (int i = 0; i < path.length; i++) {
            System.out.println(path[i]);
        }
        System.out.println();
        System.out.println(robot.getPathCost());
    }
}