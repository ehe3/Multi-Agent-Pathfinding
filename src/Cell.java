/**
 * Created by ehe on 4/24/17.
 */

public class Cell{
    int heuristicCost = 0; // h-cost
    int finalCost = 0; // f-cost
    int i, j;
    Cell parent; // maintain pointers for path

    Cell(int i, int j) {
        this.i = i;
        this.j = j;
    }

    @Override
    public String toString(){
        return "[" + this.i + ", " + this.j + "]";
    }
}

