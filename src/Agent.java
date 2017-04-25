import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

public class Agent {
    private static final int DCOST = 14;
    private static final int VCOST = 10;

    // identifier
    private int id;
    // map with cells: blocked cells are just null Cell values in grid
    private Cell [][] grid;
    // start position
    private int startI, startJ;
    // end position
    private int endI, endJ;
    // path for independent A*
    private Queue<Cell> path;

    public Agent(int id, Cell [][] grid, int startI, int startJ, int endI, int endJ, int[][] blocked) {
        this.id = id;
        this.grid = grid;
        this.startI = startI;
        this.startJ = startJ;
        this.endI = endI;
        this.endJ = endJ;
        // run A* to determine the independent path
        this.path = AStar(grid.length, grid[0].length, startI, startJ, endI, endJ, blocked);
    }

    // returns the id of a robot
    public int getID() {
        return id;
    }

    // returns orig A* path of robot
    public Queue<Cell> getPath() {
        return this.path;
    }

    // blocked cells are set to null in grid representation
    private void setBlocked(int i, int j){
        grid[i][j] = null;
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

    /*
    Params :
    id = robot number
    x, y = Board's dimensions
    si, sj = start location's x and y coordinates
    ei, ej = end location's x and y coordinates
    int[][] blocked = array containing inaccessible cell coordinates
    */
    public Queue<Cell> AStar(int x, int y, int si, int sj, int ei, int ej, int[][] blocked){
        //Reset
        grid = new Cell[x][y];
        boolean [][] closed = new boolean[x][y];
        path = new LinkedList<>();
        PriorityQueue<Cell> open = new PriorityQueue<>((Object o1, Object o2) -> {
            Cell c1 = (Cell)o1;
            Cell c2 = (Cell)o2;

            return c1.finalCost < c2.finalCost ? -1 :
                    c1.finalCost > c2.finalCost ? 1 : 0;
        });

        //Set start position
        startI = si;
        startJ = sj;

        //Set End Location
        endI = ei;
        endJ = ej;

        for(int i = 0; i < x; ++i) {
            for(int j = 0; j < y; ++j) {
                grid[i][j] = new Cell(i, j);
                grid[i][j].heuristicCost = Math.abs(i - endI) + Math.abs(j - endJ);
            }
        }
        grid[si][sj].finalCost = 0;
           
        // set blocked cells
        for(int i = 0; i < blocked.length; ++i){
            setBlocked(blocked[i][0], blocked[i][1]);
        }

        // add the start location to open list.
        open.add(grid[startI][startJ]);

        // performs the A* search
        Cell current;
        while(true) {
            current = open.poll();
            System.out.println(current);
            if(current == null) break;
            closed[current.i][current.j] = true;

            if(current.equals(grid[endI][endJ])){
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

        if(closed[endI][endJ]) {
            //Trace back the path
            Cell c = grid[endI][endJ];
            path.add(c);
            while(c.parent != null) {
                path.add(c.parent);
                c = c.parent;
            }
        } else {
            return null;
        }

        return path;
    }

    public static void main(String[] args) throws Exception{
        Cell [][] grid = new Cell[5][5];
        Agent robot = new Agent(1, grid, 0, 0, 3, 2, new int[][]{{0,4},{2,2},{3,1},{3,3}});
        for (Cell c : robot.getPath())
            System.out.println(c);
    }
}