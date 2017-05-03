/**
 * Created by Eric He on 4/24/17.
 *
 * A unit of the grid that can also be called square, node, vertex, etc.
 */

public class Cell{
    int heuristicCost = 0; // h-cost
    int finalCost = 0; // f-cost
    int i, j; // coordinate representation
    int distance; // distance from source
    Cell parent; // maintain pointers for path

    // basic constructor
    Cell(int i, int j) {
        this.i = i;
        this.j = j;
    }

    @Override
    public String toString(){
        return "[" + this.i + ", " + this.j + "]";
    }
}

